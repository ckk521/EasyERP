package com.wms.inbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.inbound.dto.InspectDTO;
import com.wms.inbound.entity.InboundOrder;
import com.wms.inbound.entity.InboundOrderItem;
import com.wms.inbound.entity.InspectRecord;
import com.wms.inbound.entity.RejectRecord;
import com.wms.inbound.repository.InboundOrderItemRepository;
import com.wms.inbound.repository.InboundOrderRepository;
import com.wms.inbound.repository.InspectRecordRepository;
import com.wms.inbound.repository.ReceiveRecordRepository;
import com.wms.inbound.repository.RejectRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 验收服务
 * 阶段四: 验收作业 + 不合格处理
 *
 * 改为记录式存储：
 * - 每次验收插入一条记录到 wms_inspect_record
 * - 入库明细的 qualifiedQty/rejectedQty 通过汇总计算
 */
@Service
@RequiredArgsConstructor
public class InspectService {

    private final InboundOrderRepository orderRepository;
    private final InboundOrderItemRepository itemRepository;
    private final InspectRecordRepository inspectRecordRepository;
    private final ReceiveRecordRepository receiveRecordRepository;
    private final RejectRecordRepository rejectRecordRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 最小保质期天数 */
    private static final int MIN_SHELF_LIFE_DAYS = 30;

    /**
     * 执行验收
     * TC-2.8 质量验收
     *
     * 改为记录式：插入验收记录，不覆盖
     */
    @Transactional
    public void inspectItem(InspectDTO dto, Long userId, String username) {
        // 参数校验
        validateInspectDTO(dto);

        // 查询入库单和明细
        InboundOrder order = orderRepository.selectById(dto.getOrderId());
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        InboundOrderItem item = itemRepository.selectById(dto.getItemId());
        if (item == null) {
            throw new RuntimeException("入库明细不存在");
        }

        // 计算已验收数量（从记录表汇总）
        Integer alreadyQualified = inspectRecordRepository.sumQualifiedQtyByItemId(item.getId());
        Integer alreadyRejected = inspectRecordRepository.sumRejectedQtyByItemId(item.getId());
        if (alreadyQualified == null) alreadyQualified = 0;
        if (alreadyRejected == null) alreadyRejected = 0;

        // 计算已收货数量（从记录表汇总）
        Integer totalReceived = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
        if (totalReceived == null) totalReceived = 0;

        // 计算待验收数量
        int pendingQty = totalReceived - alreadyQualified - alreadyRejected;

        // 校验验收数量
        int totalInspect = dto.getQualifiedQty() + dto.getRejectedQty();
        if (totalInspect > pendingQty) {
            throw new IllegalArgumentException("验收数量超过待验收数量");
        }

        // 效期商品校验
        if (dto.getExpiryDate() != null) {
            validateExpiryDate(dto.getExpiryDate());
        }

        // 生成批次号(验收通过后)
        String batchNo = null;
        if (dto.getQualifiedQty() > 0) {
            batchNo = generateBatchNo(order.getOrderNo(), item.getSkuCode());
        }

        // 创建验收记录
        InspectRecord record = new InspectRecord();
        record.setInboundOrderId(dto.getOrderId());
        record.setInboundOrderNo(order.getOrderNo());
        record.setInboundItemId(dto.getItemId());
        record.setProductId(item.getProductId());
        record.setSkuCode(item.getSkuCode());
        record.setProductName(item.getProductName());
        record.setInspectQty(totalInspect);
        record.setQualifiedQty(dto.getQualifiedQty());
        record.setRejectedQty(dto.getRejectedQty());
        record.setBatchNo(batchNo);
        record.setProductionDate(dto.getProductionDate());
        record.setExpiryDate(dto.getExpiryDate());
        record.setInspectTime(LocalDateTime.now());
        record.setInspectUser(userId);
        record.setInspectUserName(username != null ? username : "操作员");
        inspectRecordRepository.insert(record);

        // 更新入库单明细的批次号和效期信息
        if (batchNo != null) {
            item.setBatchNo(batchNo);
            item.setProductionDate(dto.getProductionDate());
            item.setExpiryDate(dto.getExpiryDate());
            itemRepository.updateById(item);
        }

        // 记录不合格品
        if (dto.getRejectedQty() != null && dto.getRejectedQty() > 0) {
            createRejectRecord(order, item, dto, userId);
        }

        // 更新入库单状态和进度
        updateOrderAfterInspect(dto.getOrderId(), order);
    }

    /**
     * 批量验收
     * TC-2.8.6
     */
    @Transactional
    public void batchInspect(InspectDTO dto, Long userId, String username) {
        if (dto.getItemIds() == null || dto.getItemIds().isEmpty()) {
            throw new IllegalArgumentException("请选择要验收的商品");
        }

        for (Long itemId : dto.getItemIds()) {
            InboundOrderItem item = itemRepository.selectById(itemId);
            if (item != null) {
                // 计算待验收数量
                Integer totalReceived = receiveRecordRepository.sumReceiveQtyByItemId(itemId);
                Integer alreadyQualified = inspectRecordRepository.sumQualifiedQtyByItemId(itemId);
                Integer alreadyRejected = inspectRecordRepository.sumRejectedQtyByItemId(itemId);
                if (totalReceived == null) totalReceived = 0;
                if (alreadyQualified == null) alreadyQualified = 0;
                if (alreadyRejected == null) alreadyRejected = 0;
                int pendingQty = totalReceived - alreadyQualified - alreadyRejected;

                if (pendingQty > 0) {
                    InspectDTO singleDto = new InspectDTO();
                    singleDto.setOrderId(dto.getOrderId());
                    singleDto.setItemId(itemId);
                    singleDto.setQualifiedQty(pendingQty);
                    singleDto.setRejectedQty(0);
                    inspectItem(singleDto, userId, username);
                }
            }
        }
    }

    /**
     * 验收参数校验
     */
    private void validateInspectDTO(InspectDTO dto) {
        if (dto.getOrderId() == null) {
            throw new IllegalArgumentException("入库单ID不能为空");
        }
        if (dto.getItemId() == null) {
            throw new IllegalArgumentException("入库明细ID不能为空");
        }
        if (dto.getQualifiedQty() == null || dto.getQualifiedQty() < 0) {
            throw new IllegalArgumentException("合格数量不能为空");
        }
        if (dto.getRejectedQty() == null || dto.getRejectedQty() < 0) {
            throw new IllegalArgumentException("不合格数量不能为空");
        }
    }

    /**
     * 效期校验
     * TC-2.8.4
     */
    private void validateExpiryDate(LocalDate expiryDate) {
        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today)) {
            throw new IllegalArgumentException("过期日期不能早于当前日期");
        }
        if (expiryDate.isBefore(today.plusDays(MIN_SHELF_LIFE_DAYS))) {
            throw new IllegalArgumentException("过期日期必须至少" + MIN_SHELF_LIFE_DAYS + "天后");
        }
    }

    /**
     * 生成批次号
     * 格式: 入库单号 + SKU后4位 + 2位序号
     */
    private String generateBatchNo(String orderNo, String skuCode) {
        String skuSuffix = skuCode.length() >= 4 ?
            skuCode.substring(skuCode.length() - 4) : skuCode;
        // 获取当天该SKU的序号
        int seq = 1; // 简化处理，实际应该查询已有批次号
        return orderNo + "-" + skuSuffix + "-" + String.format("%02d", seq);
    }

    /**
     * 创建不合格品记录
     */
    private void createRejectRecord(InboundOrder order, InboundOrderItem item,
                                    InspectDTO dto, Long userId) {
        RejectRecord record = new RejectRecord();
        record.setInboundOrderId(order.getId());
        record.setInboundOrderNo(order.getOrderNo());
        record.setInboundItemId(item.getId());
        record.setDeliveryBatchNo(order.getDeliveryBatchNo());
        record.setProductId(item.getProductId());
        record.setSkuCode(item.getSkuCode());
        record.setProductName(item.getProductName());
        record.setSupplierId(order.getSupplierId());
        record.setSupplierCode(order.getSupplierCode());
        record.setSupplierName(order.getSupplierName());
        record.setRejectQty(dto.getRejectedQty());
        // 设置不合格类型，默认为"其他"
        record.setRejectType(dto.getRejectType() != null ? dto.getRejectType() : 6);
        record.setRejectReason(dto.getRejectReason());
        record.setRejectImages(dto.getRejectImages());
        record.setDiscoverStage(RejectRecord.DISCOVER_STAGE_INSPECT);
        record.setHandleStatus(RejectRecord.HANDLE_STATUS_PENDING);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        rejectRecordRepository.insert(record);
    }

    /**
     * 验收后更新入库单状态和进度
     */
    private void updateOrderAfterInspect(Long orderId, InboundOrder order) {
        // 查询所有明细
        LambdaQueryWrapper<InboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrderItem::getOrderId, orderId);
        List<InboundOrderItem> items = itemRepository.selectList(wrapper);

        // 从记录表汇总计算
        int totalQualified = inspectRecordRepository.sumQualifiedQtyByOrderId(orderId);
        int totalRejected = inspectRecordRepository.sumRejectedQtyByOrderId(orderId);
        int totalReceived = receiveRecordRepository.sumReceiveQtyByOrderId(orderId);

        // 计算进度
        int progress = totalReceived > 0 ? ((totalQualified + totalRejected) * 100 / totalReceived) : 0;

        order.setTotalQualifiedQty(totalQualified);
        order.setTotalRejectedQty(totalRejected);
        order.setProgressInspect(progress);

        // 检查是否全部已验收（通过记录表判断）
        boolean allInspected = items.stream()
            .allMatch(item -> {
                Integer received = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
                Integer inspected = inspectRecordRepository.sumQualifiedQtyByItemId(item.getId());
                inspected += inspectRecordRepository.sumRejectedQtyByItemId(item.getId());
                return inspected >= (received != null ? received : 0);
            });

        // 状态更新逻辑：
        // 1. 全部验收完成 -> 待上架(3)
        // 2. 有合格商品但未全部验收 -> 验收中(2)，允许部分上架
        // 3. 无合格商品且未全部验收 -> 保持当前状态(收货中)
        if (allInspected) {
            order.setStatus(InboundOrder.STATUS_PUTAWAY);
        } else if (totalQualified > 0) {
            // 有合格商品，可以开始上架，状态改为验收中
            order.setStatus(InboundOrder.STATUS_INSPECTING);
        }

        orderRepository.updateById(order);
    }

    /**
     * 查询待验收的商品列表
     */
    public Map<String, Object> listPendingItems(Long orderId) {
        LambdaQueryWrapper<InboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrderItem::getOrderId, orderId);
        List<InboundOrderItem> items = itemRepository.selectList(wrapper);

        // 过滤出有待验收数量的商品
        List<Map<String, Object>> pendingItems = items.stream()
            .map(item -> {
                Integer totalReceived = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
                Integer alreadyQualified = inspectRecordRepository.sumQualifiedQtyByItemId(item.getId());
                Integer alreadyRejected = inspectRecordRepository.sumRejectedQtyByItemId(item.getId());
                if (totalReceived == null) totalReceived = 0;
                if (alreadyQualified == null) alreadyQualified = 0;
                if (alreadyRejected == null) alreadyRejected = 0;
                int pendingQty = totalReceived - alreadyQualified - alreadyRejected;

                Map<String, Object> map = new HashMap<>();
                map.put("id", item.getId());
                map.put("skuCode", item.getSkuCode());
                map.put("productName", item.getProductName());
                map.put("receivedQty", totalReceived);
                map.put("qualifiedQty", alreadyQualified);
                map.put("rejectedQty", alreadyRejected);
                map.put("pendingQty", pendingQty);
                return map;
            })
            .filter(m -> (int) m.get("pendingQty") > 0)
            .collect(java.util.stream.Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", pendingItems);
        result.put("total", pendingItems.size());
        return result;
    }

    /**
     * 查询入库明细的验收记录
     */
    public List<InspectRecord> getInspectRecords(Long itemId) {
        return inspectRecordRepository.findByItemId(itemId);
    }

    /**
     * 查询入库单的所有验收记录
     */
    public List<InspectRecord> getOrderInspectRecords(Long orderId) {
        return inspectRecordRepository.findByOrderId(orderId);
    }

    /**
     * 查询最近的验收记录（全局）
     */
    public List<InspectRecord> getRecentInspectRecords(int limit) {
        return inspectRecordRepository.findRecent(limit);
    }
}
