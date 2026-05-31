package com.wms.inbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.exception.dto.ExceptionOrderCreateDTO;
import com.wms.exception.entity.ExceptionItem;
import com.wms.exception.entity.ExceptionOrder;
import com.wms.exception.repository.ExceptionItemRepository;
import com.wms.exception.repository.ExceptionOrderRepository;
import com.wms.exception.service.ExceptionService;
import com.wms.inbound.dto.ReceiveDTO;
import com.wms.inbound.entity.InboundOrder;
import com.wms.inbound.entity.InboundOrderItem;
import com.wms.inbound.entity.ReceiveRecord;
import com.wms.inbound.repository.InboundOrderItemRepository;
import com.wms.inbound.repository.InboundOrderRepository;
import com.wms.inbound.repository.ReceiveRecordRepository;
import com.wms.inbound.service.InboundStatusService;
import com.wms.system.entity.BaseZone;
import com.wms.system.repository.BaseZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 收货服务
 * 阶段三: 收货作业 + 差异处理
 *
 * 改为记录式存储：
 * - 每次收货插入一条记录到 wms_receive_record
 * - 入库明细的 receivedQty 通过汇总计算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiveService {

    private final InboundOrderRepository orderRepository;
    private final InboundOrderItemRepository itemRepository;
    private final ReceiveRecordRepository receiveRecordRepository;
    private final ExceptionService exceptionService;
    private final ExceptionItemRepository exceptionItemRepository;
    private final ExceptionOrderRepository exceptionOrderRepository;
    private final BaseZoneRepository zoneRepository;
    private final InboundStatusService inboundStatusService;

    /** 差异阈值百分比(10%) */
    private static final double DIFF_THRESHOLD = 0.10;

    /** 需要创建异常处理单的差异原因（中文） */
    private static final List<String> EXCEPTION_REASONS_CN = List.of(
        "运输损耗", "包装破损", "质量不合格", "供应商少货"
    );

    /** 需要创建异常处理单的差异原因（英文） */
    private static final List<String> EXCEPTION_REASONS_EN = List.of(
        "Transport damage", "Package damaged", "Quality issue", "Supplier shortage"
    );

    /**
     * 通过入库单号查找入库单
     * TC-2.5.1 扫描入库单号定位单据
     */
    public InboundOrder findOrderByNo(String orderNo) {
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrder::getOrderNo, orderNo);
        InboundOrder order = orderRepository.selectOne(wrapper);
        if (order == null) {
            throw new RuntimeException("入库单不存在: " + orderNo);
        }
        return order;
    }

    /**
     * 通过SKU编码查找入库明细
     * TC-2.5.2 扫描商品条码识别商品
     */
    public InboundOrderItem findItemBySkuCode(Long orderId, String skuCode) {
        LambdaQueryWrapper<InboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrderItem::getOrderId, orderId)
               .eq(InboundOrderItem::getSkuCode, skuCode);
        InboundOrderItem item = itemRepository.selectOne(wrapper);
        if (item != null) {
            // 计算已收货数量（从记录表汇总）
            Integer receivedQty = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
            item.setReceivedQty(receivedQty);
        }
        return item;
    }

    /**
     * 通过条码查找入库明细
     */
    public InboundOrderItem findItemByBarcode(Long orderId, String barcode) {
        LambdaQueryWrapper<InboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrderItem::getOrderId, orderId)
               .eq(InboundOrderItem::getBarcode, barcode);
        InboundOrderItem item = itemRepository.selectOne(wrapper);
        if (item != null) {
            // 计算已收货数量（从记录表汇总）
            Integer receivedQty = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
            item.setReceivedQty(receivedQty);
        }
        return item;
    }

    /**
     * 执行收货
     * TC-2.5.3 录入实际收货数量
     * TC-2.6 收货数量差异处理
     *
     * 改为记录式：插入收货记录，不覆盖
     * 如果有短缺差异且原因属于异常类型，自动创建异常处理单
     */
    @Transactional
    public void receiveItem(ReceiveDTO dto, Long userId, String username) {
        // 参数校验
        validateReceiveDTO(dto);

        // 查询入库单
        InboundOrder order = orderRepository.selectById(dto.getOrderId());
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        // 状态校验
        validateOrderStatus(order);

        // 查询入库明细
        InboundOrderItem item = itemRepository.selectById(dto.getItemId());
        if (item == null) {
            throw new RuntimeException("入库明细不存在");
        }

        // 检查已隔离数量（已被异常处理单锁定的数量）
        Integer isolatedQty = exceptionItemRepository.sumIsolatedQtyByInboundItemId(item.getId());
        if (isolatedQty == null) isolatedQty = 0;

        // 计算待收货数量（预期数量 - 已收货数量 - 已隔离数量）
        Integer alreadyReceived = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
        if (alreadyReceived == null) alreadyReceived = 0;
        int pendingQty = item.getExpectedQty() - alreadyReceived - isolatedQty;

        // 如果待收货数量为0，说明该商品已全部收货或隔离
        if (pendingQty <= 0) {
            throw new IllegalStateException("该商品已全部收货或已隔离，不能再收货。已收货: " + alreadyReceived + "，已隔离: " + isolatedQty);
        }

        // 如果收货数量超过待收货数量，提示用户
        if (dto.getReceivedQty() > pendingQty) {
            throw new IllegalArgumentException("收货数量不能超过待收货数量。待收货: " + pendingQty + "（已收货: " + alreadyReceived + "，已隔离: " + isolatedQty + "）");
        }

        int diff = dto.getReceivedQty() - pendingQty;
        int shortageQty = diff < 0 ? Math.abs(diff) : 0; // 短缺数量

        // 差异校验（基于待收货数量）
        validateDifference(pendingQty, dto.getReceivedQty(), dto.getDiffReason());

        // 收货数量大于0时，自动生成批次号
        String batchNo = null;
        if (dto.getReceivedQty() > 0) {
            batchNo = generateBatchNo(order.getOrderNo(), item.getSkuCode());
        }

        // 创建收货记录
        ReceiveRecord record = new ReceiveRecord();
        record.setInboundOrderId(dto.getOrderId());
        record.setInboundOrderNo(order.getOrderNo());
        record.setInboundItemId(dto.getItemId());
        record.setProductId(item.getProductId());
        record.setSkuCode(item.getSkuCode());
        record.setProductName(item.getProductName());
        record.setReceiveQty(dto.getReceivedQty());
        record.setDiffQty(diff);
        record.setDiffReason(dto.getDiffReason());
        record.setHasException(shortageQty > 0 && StringUtils.hasText(dto.getDiffReason()) ? 1 : 0);
        record.setBatchNo(batchNo);
        record.setProductionDate(dto.getProductionDate());
        record.setExpiryDate(dto.getExpiryDate());
        record.setReceiveTime(LocalDateTime.now());
        record.setReceiveUser(userId);
        record.setReceiveUserName(username != null ? username : "操作员");
        receiveRecordRepository.insert(record);

        // 更新入库明细的收货数量和批次号
        if (dto.getReceivedQty() > 0) {
            // 累加收货数量
            Integer currentReceived = item.getReceivedQty() != null ? item.getReceivedQty() : 0;
            item.setReceivedQty(currentReceived + dto.getReceivedQty());
            if (batchNo != null) {
                item.setBatchNo(batchNo);
            }
            if (dto.getProductionDate() != null) {
                item.setProductionDate(dto.getProductionDate());
            }
            if (dto.getExpiryDate() != null) {
                item.setExpiryDate(dto.getExpiryDate());
            }
            // 更新状态为已收货
            if (item.getStatus() == InboundOrderItem.STATUS_PENDING) {
                item.setStatus(InboundOrderItem.STATUS_RECEIVED);
            }
            itemRepository.updateById(item);
        }

        // 如果有短缺差异且原因属于异常类型，自动创建异常处理单
        if (shortageQty > 0 && StringUtils.hasText(dto.getDiffReason()) &&
            (EXCEPTION_REASONS_CN.contains(dto.getDiffReason()) || EXCEPTION_REASONS_EN.contains(dto.getDiffReason()))) {
            try {
                createExceptionOrder(order, item, dto.getDiffReason(), shortageQty, userId, username);
                log.info("自动创建异常处理单: 入库单={}, 商品={}, 短缺数量={}, 原因={}",
                    order.getOrderNo(), item.getSkuCode(), shortageQty, dto.getDiffReason());
            } catch (Exception e) {
                log.error("创建异常处理单失败: {}", e.getMessage());
                // 不影响收货流程，继续执行
            }
        }

        // 更新入库单状态和进度
        updateOrderAfterReceive(dto.getOrderId(), order);
    }

    /**
     * 自动创建异常处理单
     */
    private void createExceptionOrder(InboundOrder order, InboundOrderItem item, String diffReason, int shortageQty, Long userId, String username) {
        // 根据差异原因确定异常类型
        int exceptionType = mapDiffReasonToExceptionType(diffReason);

        // 查找隔离库区（名称包含"隔离"或"异常"的库区）
        BaseZone isolationZone = findOrCreateIsolationZone(order.getWarehouseId(), order.getWarehouseCode());

        ExceptionOrderCreateDTO dto = new ExceptionOrderCreateDTO();
        dto.setInboundOrderId(order.getId());
        dto.setInboundOrderNo(order.getOrderNo());
        dto.setSupplierId(order.getSupplierId());
        dto.setSupplierCode(order.getSupplierCode());
        dto.setSupplierName(order.getSupplierName());
        dto.setWarehouseId(order.getWarehouseId());
        dto.setWarehouseCode(order.getWarehouseCode());
        dto.setZoneId(isolationZone.getId());
        dto.setZoneCode(isolationZone.getCode());
        dto.setExceptionType(exceptionType);
        dto.setExceptionReason(diffReason + "，短缺" + shortageQty + "件");
        dto.setSourceType(1); // 收货异常

        // 创建异常商品明细
        ExceptionOrderCreateDTO.ExceptionItemDTO itemDTO = new ExceptionOrderCreateDTO.ExceptionItemDTO();
        itemDTO.setProductId(item.getProductId());
        itemDTO.setSkuCode(item.getSkuCode());
        itemDTO.setProductName(item.getProductName());
        itemDTO.setExceptionQty(shortageQty);
        itemDTO.setExceptionType(exceptionType);
        itemDTO.setExceptionReason(diffReason);
        itemDTO.setInboundItemId(item.getId());
        dto.setItems(List.of(itemDTO));

        exceptionService.createExceptionOrder(dto, userId, username);
    }

    /**
     * 查找或创建隔离库区
     */
    private BaseZone findOrCreateIsolationZone(Long warehouseId, String warehouseCode) {
        // 查找隔离库区
        LambdaQueryWrapper<BaseZone> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseZone::getWarehouseId, warehouseId);
        wrapper.and(w -> w.like(BaseZone::getName, "隔离").or().like(BaseZone::getName, "异常"));
        List<BaseZone> zones = zoneRepository.selectList(wrapper);

        if (!zones.isEmpty()) {
            return zones.get(0);
        }

        // 如果没有隔离库区，使用仓库的第一个库区
        LambdaQueryWrapper<BaseZone> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(BaseZone::getWarehouseId, warehouseId);
        wrapper2.last("LIMIT 1");
        BaseZone zone = zoneRepository.selectOne(wrapper2);

        if (zone != null) {
            return zone;
        }

        // 如果没有任何库区，创建一个默认的隔离库区
        BaseZone newZone = new BaseZone();
        newZone.setWarehouseId(warehouseId);
        newZone.setWarehouseCode(warehouseCode);
        newZone.setCode(warehouseCode + "-ISO-01");
        newZone.setName("隔离库区");
        newZone.setType(3); // 存储区
        newZone.setStatus(1);
        newZone.setCreateTime(LocalDateTime.now());
        zoneRepository.insert(newZone);
        return newZone;
    }

    /**
     * 将差异原因映射到异常类型
     */
    private int mapDiffReasonToExceptionType(String diffReason) {
        // 中文原因
        if ("包装破损".equals(diffReason) || "Package damaged".equals(diffReason)) {
            return 1; // 破损
        }
        if ("运输损耗".equals(diffReason) || "Transport damage".equals(diffReason)) {
            return 1; // 破损（运输损耗也算破损）
        }
        if ("质量不合格".equals(diffReason) || "Quality issue".equals(diffReason)) {
            return 3; // 质量不合格
        }
        if ("供应商少货".equals(diffReason) || "Supplier shortage".equals(diffReason)) {
            return 2; // 短缺
        }
        return 5; // 其他
    }

    /**
     * 收货参数校验
     */
    private void validateReceiveDTO(ReceiveDTO dto) {
        if (dto.getOrderId() == null) {
            throw new IllegalArgumentException("入库单ID不能为空");
        }
        if (dto.getItemId() == null) {
            throw new IllegalArgumentException("入库明细ID不能为空");
        }
        // 收货数量可以为0（表示全部拒收），但必须填写差异原因
        if (dto.getReceivedQty() == null || dto.getReceivedQty() < 0) {
            throw new IllegalArgumentException("收货数量不能为负数");
        }
        if (dto.getReceivedQty() == 0 && !StringUtils.hasText(dto.getDiffReason())) {
            throw new IllegalArgumentException("收货数量为0时必须填写差异原因");
        }
    }

    /**
     * 入库单状态校验
     */
    private void validateOrderStatus(InboundOrder order) {
        if (order.getStatus() == InboundOrder.STATUS_CANCELLED) {
            throw new IllegalStateException("已取消的入库单不能收货");
        }
        if (order.getStatus() == InboundOrder.STATUS_COMPLETED) {
            throw new IllegalStateException("已完成的入库单不能收货");
        }
    }

    /**
     * 差异校验
     * TC-2.6.6 差异超过阈值需确认
     *
     * @param pendingQty 待收货数量
     * @param receivedQty 本次收货数量
     * @param diffReason 差异原因
     */
    private void validateDifference(Integer pendingQty, Integer receivedQty, String diffReason) {
        int diff = Math.abs(pendingQty - receivedQty);
        double diffPercent = pendingQty > 0 ? (double) diff / pendingQty : 0;

        // 差异超过阈值(10%)必须填写原因
        if (diffPercent > DIFF_THRESHOLD && !StringUtils.hasText(diffReason)) {
            throw new IllegalArgumentException(
                String.format("差异%.1f%%超过阈值%.0f%%，请填写差异原因",
                    diffPercent * 100, DIFF_THRESHOLD * 100));
        }
    }

    /**
     * 生成批次号
     * 格式：入库单号-SKU后3位-序号
     * 例如：IN202605100001-009-01
     */
    private String generateBatchNo(String orderNo, String skuCode) {
        String skuSuffix = skuCode.length() >= 3 ?
            skuCode.substring(skuCode.length() - 3) : skuCode;
        // 获取当天该SKU的序号（简化处理，实际可查询已有批次号）
        int seq = 1;
        return orderNo + "-" + skuSuffix + "-" + String.format("%02d", seq);
    }

    /**
     * 收货后更新入库单状态和进度
     */
    private void updateOrderAfterReceive(Long orderId, InboundOrder order) {
        // 使用统一的状态计算服务更新入库单状态和进度
        inboundStatusService.recalculateStatus(orderId);
    }

    /**
     * 内部方法：检查并更新入库单状态
     * 已废弃，使用 InboundStatusService.recalculateStatus 替代
     * @deprecated 使用 {@link InboundStatusService#recalculateStatus(Long)} 替代
     */
    @Deprecated
    private void checkAndUpdateOrderStatusInternal(Long orderId, InboundOrder order) {
        // 委托给统一的状态计算服务
        inboundStatusService.recalculateStatus(orderId);
    }

    /**
     * 检查并更新入库单状态
     * TC-2.5.5 全部商品收货或隔离完成后状态变为验收中
     */
    @Transactional
    public void checkAndUpdateOrderStatus(Long orderId) {
        InboundOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        // 查询所有明细
        LambdaQueryWrapper<InboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrderItem::getOrderId, orderId);
        List<InboundOrderItem> items = itemRepository.selectList(wrapper);

        // 检查是否全部已收货或隔离
        boolean allProcessed = items.stream()
            .allMatch(item -> {
                Integer received = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
                if (received == null) received = 0;
                Integer isolated = exceptionItemRepository.sumIsolatedQtyByInboundItemId(item.getId());
                if (isolated == null) isolated = 0;
                return received + isolated >= item.getExpectedQty();
            });

        if (allProcessed) {
            order.setStatus(InboundOrder.STATUS_INSPECTING);
            orderRepository.updateById(order);
        }
    }

    /**
     * 更新入库单进度
     */
    @Transactional
    public void updateOrderProgress(Long orderId) {
        InboundOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }
        updateOrderProgressInternal(orderId, order);
        orderRepository.updateById(order);
    }

    /**
     * 内部方法：更新入库单进度
     * 进度计算包含隔离数量：(已收货 + 已隔离) / 预期数量
     */
    private void updateOrderProgressInternal(Long orderId, InboundOrder order) {
        LambdaQueryWrapper<InboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrderItem::getOrderId, orderId);
        List<InboundOrderItem> items = itemRepository.selectList(wrapper);

        // 计算总预期数量
        int totalExpected = items.stream()
            .mapToInt(InboundOrderItem::getExpectedQty)
            .sum();

        // 计算总收货数量（从记录表汇总）
        Integer totalReceived = receiveRecordRepository.sumReceiveQtyByOrderId(orderId);
        if (totalReceived == null) totalReceived = 0;

        // 计算总隔离数量（从异常商品表汇总）
        Integer totalIsolated = exceptionItemRepository.sumIsolatedQtyByOrderId(orderId);
        if (totalIsolated == null) totalIsolated = 0;

        // 计算进度百分比：(已收货 + 已隔离) / 预期数量
        int totalProcessed = totalReceived + totalIsolated;
        int progress = totalExpected > 0 ? (totalProcessed * 100 / totalExpected) : 0;

        order.setTotalExpectedQty(totalExpected);
        order.setTotalReceivedQty(totalReceived);
        order.setProgressReceive(progress);
    }

    /**
     * 查询待收货的入库单列表
     */
    public Map<String, Object> listPendingOrders(int page, int limit) {
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        // 待收货或收货中状态
        wrapper.in(InboundOrder::getStatus, InboundOrder.STATUS_PENDING, InboundOrder.STATUS_RECEIVING);
        wrapper.orderByDesc(InboundOrder::getCreateTime);

        // 分页查询
        com.baomidou.mybatisplus.core.metadata.IPage<InboundOrder> pageResult =
            orderRepository.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, limit), wrapper);

        // 转换为Map列表，添加隔离数量信息
        List<Map<String, Object>> list = pageResult.getRecords().stream()
            .map(this::orderToMapWithIsolation)
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", pageResult.getTotal());
        return result;
    }

    /**
     * 入库单转换为Map，包含隔离数量信息
     */
    private Map<String, Object> orderToMapWithIsolation(InboundOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("orderType", order.getOrderType());
        map.put("supplierName", order.getSupplierName());
        map.put("warehouseName", order.getWarehouseName());
        map.put("status", order.getStatus());
        map.put("createTime", order.getCreateTime());

        // 查询入库明细，添加隔离数量
        LambdaQueryWrapper<InboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InboundOrderItem::getOrderId, order.getId());
        List<InboundOrderItem> items = itemRepository.selectList(itemWrapper);

        List<Map<String, Object>> itemMaps = items.stream()
            .map(item -> {
                Map<String, Object> itemMap = new LinkedHashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("skuCode", item.getSkuCode());
                itemMap.put("productName", item.getProductName());
                itemMap.put("expectedQty", item.getExpectedQty());

                // 已收货数量
                Integer receivedQty = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
                itemMap.put("receivedQty", receivedQty != null ? receivedQty : 0);

                // 已隔离数量
                Integer isolatedQty = exceptionItemRepository.sumIsolatedQtyByInboundItemId(item.getId());
                itemMap.put("isolatedQty", isolatedQty != null ? isolatedQty : 0);

                // 待收货数量 = 预期 - 已收货 - 已隔离
                int pendingQty = item.getExpectedQty() - (receivedQty != null ? receivedQty : 0) - (isolatedQty != null ? isolatedQty : 0);
                itemMap.put("pendingQty", pendingQty);

                return itemMap;
            })
            .collect(Collectors.toList());

        map.put("items", itemMaps);
        return map;
    }

    /**
     * 查询入库明细的收货记录
     */
    public List<ReceiveRecord> getReceiveRecords(Long itemId) {
        return receiveRecordRepository.findByItemId(itemId);
    }

    /**
     * 查询入库单的所有收货记录
     */
    public List<ReceiveRecord> getOrderReceiveRecords(Long orderId) {
        return receiveRecordRepository.findByOrderId(orderId);
    }

    /**
     * 查询最近的收货记录（全局）
     */
    public List<ReceiveRecord> getRecentReceiveRecords(int limit) {
        return receiveRecordRepository.findRecent(limit);
    }

    /**
     * 入库单转Map
     */
    private Map<String, Object> orderToMap(InboundOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("deliveryBatchNo", order.getDeliveryBatchNo());
        map.put("orderType", order.getOrderType());
        map.put("poNo", order.getPoNo());
        map.put("supplierId", order.getSupplierId());
        map.put("supplierName", order.getSupplierName());
        map.put("warehouseId", order.getWarehouseId());
        map.put("warehouseName", order.getWarehouseName());
        map.put("status", order.getStatus());
        map.put("totalExpectedQty", order.getTotalExpectedQty());
        map.put("totalReceivedQty", order.getTotalReceivedQty());
        map.put("progressReceive", order.getProgressReceive());
        map.put("createTime", order.getCreateTime());
        return map;
    }

    /**
     * 修复入库单状态
     * 将已全部收货但仍处于"收货中"状态的入库单更新为"验收中"
     */
    @Transactional
    public int fixOrderStatus() {
        // 查询所有收货中状态的入库单
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrder::getStatus, InboundOrder.STATUS_RECEIVING);
        List<InboundOrder> orders = orderRepository.selectList(wrapper);

        int fixedCount = 0;
        for (InboundOrder order : orders) {
            // 检查是否全部收货完成
            checkAndUpdateOrderStatusInternal(order.getId(), order);
            if (order.getStatus() == InboundOrder.STATUS_INSPECTING) {
                orderRepository.updateById(order);
                fixedCount++;
                log.info("修复入库单 {} 状态: 收货中 -> 验收中", order.getOrderNo());
            }
        }
        return fixedCount;
    }

    /**
     * 清零已取消异常的差异数量
     * 用于修复历史数据：当收货异常被取消后，收货记录中的差异数量应该清零
     */
    @Transactional
    public int clearCancelledExceptionDiff(Long orderId) {
        // 查询该入库单的所有已取消异常订单（sourceType=1 表示收货异常）
        LambdaQueryWrapper<ExceptionOrder> exWrapper = new LambdaQueryWrapper<>();
        exWrapper.eq(ExceptionOrder::getInboundOrderId, orderId)
                 .eq(ExceptionOrder::getSourceType, ExceptionOrder.SOURCE_RECEIVE)
                 .eq(ExceptionOrder::getStatus, ExceptionOrder.STATUS_CANCELLED);
        List<ExceptionOrder> cancelledExceptions = exceptionOrderRepository.selectList(exWrapper);

        if (cancelledExceptions.isEmpty()) {
            log.info("入库单 {} 没有已取消的收货异常", orderId);
            return 0;
        }

        // 收集所有异常明细的入库明细ID
        Set<Long> inboundItemIds = new HashSet<>();
        for (ExceptionOrder exOrder : cancelledExceptions) {
            LambdaQueryWrapper<ExceptionItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(ExceptionItem::getOrderId, exOrder.getId());
            List<ExceptionItem> items = exceptionItemRepository.selectList(itemWrapper);
            for (ExceptionItem item : items) {
                if (item.getInboundItemId() != null) {
                    inboundItemIds.add(item.getInboundItemId());
                }
            }
        }

        // 清零这些入库明细的差异数量
        int clearedCount = 0;
        for (Long inboundItemId : inboundItemIds) {
            LambdaQueryWrapper<ReceiveRecord> recordWrapper = new LambdaQueryWrapper<>();
            recordWrapper.eq(ReceiveRecord::getInboundItemId, inboundItemId)
                         .lt(ReceiveRecord::getDiffQty, 0);
            List<ReceiveRecord> records = receiveRecordRepository.selectList(recordWrapper);
            for (ReceiveRecord record : records) {
                record.setDiffQty(0);
                record.setDiffReason(null);
                record.setRemark((record.getRemark() != null ? record.getRemark() : "") + " | 异常取消，差异数量清零(修复)");
                receiveRecordRepository.updateById(record);
                clearedCount++;
                log.info("清零收货记录差异: 记录ID={}, 入库明细ID={}", record.getId(), inboundItemId);
            }
        }

        // 更新入库单进度和状态
        if (clearedCount > 0) {
            InboundOrder order = orderRepository.selectById(orderId);
            if (order != null) {
                // 重新计算收货进度
                updateOrderProgressInternal(orderId, order);

                // 检查是否有待收货数量
                LambdaQueryWrapper<InboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(InboundOrderItem::getOrderId, orderId);
                List<InboundOrderItem> items = itemRepository.selectList(itemWrapper);

                boolean hasPending = items.stream().anyMatch(item -> {
                    Integer received = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
                    if (received == null) received = 0;
                    Integer isolated = exceptionItemRepository.sumIsolatedQtyByInboundItemId(item.getId());
                    if (isolated == null) isolated = 0;
                    return received + isolated < item.getExpectedQty();
                });

                // 如果有待收货数量，将状态改回"收货中"
                if (hasPending && order.getStatus() != InboundOrder.STATUS_PENDING && order.getStatus() != InboundOrder.STATUS_RECEIVING) {
                    order.setStatus(InboundOrder.STATUS_RECEIVING);
                    log.info("入库单 {} 有待收货数量，状态改回收货中", order.getOrderNo());
                }

                orderRepository.updateById(order);
            }
        }

        return clearedCount;
    }

    /**
     * 重置入库单状态为收货中
     * 用于修复历史数据：当入库单已完成但有未收货数量时，将状态改回收货中
     */
    @Transactional
    public void resetOrderToReceiving(Long orderId) {
        InboundOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        // 检查是否有待收货数量
        LambdaQueryWrapper<InboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InboundOrderItem::getOrderId, orderId);
        List<InboundOrderItem> items = itemRepository.selectList(itemWrapper);

        boolean hasPending = items.stream().anyMatch(item -> {
            Integer received = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
            if (received == null) received = 0;
            Integer isolated = exceptionItemRepository.sumIsolatedQtyByInboundItemId(item.getId());
            if (isolated == null) isolated = 0;
            return received + isolated < item.getExpectedQty();
        });

        if (!hasPending) {
            throw new RuntimeException("入库单没有待收货数量，无法重置");
        }

        // 将状态改回"收货中"
        order.setStatus(InboundOrder.STATUS_RECEIVING);
        order.setCompleteTime(null);
        orderRepository.updateById(order);
        log.info("入库单 {} 状态已重置为收货中", order.getOrderNo());
    }
}
