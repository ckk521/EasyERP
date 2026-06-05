package com.wms.outbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.inbound.dto.InboundOrderDTO;
import com.wms.inbound.dto.InboundOrderItemDTO;
import com.wms.inbound.entity.InboundOrder;
import com.wms.inbound.repository.InboundOrderRepository;
import com.wms.inbound.service.InboundOrderService;
import com.wms.outbound.dto.OutboundOrderDTO;
import com.wms.outbound.dto.OutboundOrderItemDTO;
import com.wms.outbound.dto.OutboundOrderQueryDTO;
import com.wms.outbound.entity.OutboundOrder;
import com.wms.outbound.entity.OutboundOrderItem;
import com.wms.outbound.entity.PickRecord;
import com.wms.outbound.repository.OutboundOrderRepository;
import com.wms.outbound.repository.OutboundOrderItemRepository;
import com.wms.outbound.repository.PickRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 出库单服务
 *
 * 实现功能：
 * 1. 出库单创建（销售出库、手工创建）
 * 2. 出库单查询（列表、详情）
 * 3. 出库单取消（待分配、已分配状态）
 * 4. 库存校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundOrderService {

    private final OutboundOrderRepository orderRepository;
    private final OutboundOrderItemRepository itemRepository;
    private final InboundOrderService inboundOrderService;
    private final InboundOrderRepository inboundOrderRepository;
    private final PickRecordRepository pickRecordRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 有效的出库类型
    private static final Set<Integer> VALID_ORDER_TYPES = Set.of(
        OutboundOrder.TYPE_SALES,
        OutboundOrder.TYPE_TRANSFER,
        OutboundOrder.TYPE_RETURN,
        OutboundOrder.TYPE_SCRAP,
        OutboundOrder.TYPE_SAMPLE
    );

    // 有效的来源类型
    private static final Set<Integer> VALID_SOURCE_TYPES = Set.of(
        OutboundOrder.SOURCE_ERP,
        OutboundOrder.SOURCE_MANUAL,
        OutboundOrder.SOURCE_TRANSFER
    );

    /**
     * 创建出库单
     */
    @Transactional
    public Long createOrder(OutboundOrderDTO dto) {
        // 参数校验
        validateCreateOrder(dto);

        // 生成出库单号
        String orderNo = generateOrderNo();

        // 销售出库自动生成销售订单号（目前未集成ERP）
        String soNo = dto.getSoNo();
        if (dto.getOrderType().equals(OutboundOrder.TYPE_SALES) && !StringUtils.hasText(soNo)) {
            soNo = generateSoNo();
        }

        // 构建出库单实体
        OutboundOrder order = new OutboundOrder();
        order.setOrderNo(orderNo);
        order.setOrderType(dto.getOrderType());
        order.setSourceType(dto.getSourceType());
        order.setSoNo(soNo);
        order.setCustomerId(dto.getCustomerId());
        order.setCustomerCode(dto.getCustomerCode());
        order.setCustomerName(dto.getCustomerName());
        order.setCustomerPhone(dto.getCustomerPhone());
        order.setCustomerAddress(dto.getCustomerAddress());
        order.setSupplierId(dto.getSupplierId());
        order.setSupplierCode(dto.getSupplierCode());
        order.setSupplierName(dto.getSupplierName());
        order.setTargetWarehouseId(dto.getTargetWarehouseId());
        order.setTargetWarehouseCode(dto.getTargetWarehouseCode());
        order.setTargetWarehouseName(dto.getTargetWarehouseName());
        order.setWarehouseId(dto.getWarehouseId());
        order.setWarehouseCode(dto.getWarehouseCode());
        order.setWarehouseName(dto.getWarehouseName());
        order.setPriority(dto.getPriority() != null ? dto.getPriority() : OutboundOrder.PRIORITY_NORMAL);
        order.setLogisticsCompany(dto.getLogisticsCompany());
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setStatus(OutboundOrder.STATUS_PENDING);
        order.setRemark(dto.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 计算总出库数量
        int totalQty = dto.getItems().stream()
            .mapToInt(OutboundOrderItemDTO::getQty)
            .sum();
        order.setTotalQty(totalQty);
        order.setTotalPickedQty(0);
        order.setTotalPackedQty(0);
        order.setTotalShippedQty(0);

        // 保存出库单
        orderRepository.insert(order);

        // 保存出库单明细
        for (OutboundOrderItemDTO itemDTO : dto.getItems()) {
            OutboundOrderItem item = new OutboundOrderItem();
            item.setOrderId(order.getId());
            item.setOrderNo(orderNo);
            item.setProductId(itemDTO.getProductId());
            item.setSkuCode(itemDTO.getSkuCode());
            item.setProductName(itemDTO.getProductName());
            item.setBarcode(itemDTO.getBarcode());
            item.setQty(itemDTO.getQty());
            item.setPickedQty(0);
            item.setPackedQty(0);
            item.setShippedQty(0);
            item.setLocationId(itemDTO.getLocationId());
            item.setLocationCode(itemDTO.getLocationCode());
            item.setBatchNo(itemDTO.getBatchNo());
            item.setStatus(OutboundOrderItem.STATUS_PENDING);
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            itemRepository.insert(item);
        }

        log.info("出库单创建成功: orderNo={}, totalQty={}", orderNo, totalQty);

        // 调拨出库：自动创建调拨入库单
        if (dto.getOrderType().equals(OutboundOrder.TYPE_TRANSFER)) {
            Long inboundId = createTransferInboundOrder(order, dto);
            // 更新出库单的调拨入库单关联字段
            InboundOrder inboundOrder = inboundOrderRepository.selectById(inboundId);
            order.setTransferInboundId(inboundId);
            order.setTransferInboundNo(inboundOrder.getOrderNo());
            orderRepository.updateById(order);
            log.info("调拨出库自动创建入库单: outboundNo={}, inboundNo={}", orderNo, inboundOrder.getOrderNo());
        }

        return order.getId();
    }

    /**
     * 创建调拨入库单
     */
    private Long createTransferInboundOrder(OutboundOrder outboundOrder, OutboundOrderDTO outboundDTO) {
        InboundOrderDTO inboundDTO = new InboundOrderDTO();
        inboundDTO.setOrderType(InboundOrder.TYPE_TRANSFER);
        inboundDTO.setWarehouseId(outboundDTO.getTargetWarehouseId());
        inboundDTO.setWarehouseCode(outboundDTO.getTargetWarehouseCode());
        inboundDTO.setWarehouseName(outboundDTO.getTargetWarehouseName());
        inboundDTO.setSourceOutboundId(outboundOrder.getId());
        inboundDTO.setSourceOutboundNo(outboundOrder.getOrderNo());
        inboundDTO.setRemark("调拨入库，来源出库单：" + outboundOrder.getOrderNo());

        // 转换商品明细
        List<InboundOrderItemDTO> inboundItems = outboundDTO.getItems().stream()
            .map(item -> {
                InboundOrderItemDTO inboundItem = new InboundOrderItemDTO();
                inboundItem.setProductId(item.getProductId());
                inboundItem.setSkuCode(item.getSkuCode());
                inboundItem.setProductName(item.getProductName());
                inboundItem.setBarcode(item.getBarcode());
                inboundItem.setExpectedQty(item.getQty());
                return inboundItem;
            })
            .collect(Collectors.toList());
        inboundDTO.setItems(inboundItems);

        return inboundOrderService.createOrder(inboundDTO);
    }

    /**
     * 分页查询出库单列表
     */
    public Map<String, Object> listOrders(OutboundOrderQueryDTO queryDTO) {
        LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<>();

        // 出库单号精确查询
        if (StringUtils.hasText(queryDTO.getOrderNo())) {
            wrapper.eq(OutboundOrder::getOrderNo, queryDTO.getOrderNo());
        }

        // 销售订单号查询
        if (StringUtils.hasText(queryDTO.getSoNo())) {
            wrapper.eq(OutboundOrder::getSoNo, queryDTO.getSoNo());
        }

        // 客户筛选
        if (queryDTO.getCustomerId() != null) {
            wrapper.eq(OutboundOrder::getCustomerId, queryDTO.getCustomerId());
        }

        // 仓库筛选
        if (queryDTO.getWarehouseId() != null) {
            wrapper.eq(OutboundOrder::getWarehouseId, queryDTO.getWarehouseId());
        }

        // 出库类型筛选
        if (queryDTO.getOrderType() != null) {
            wrapper.eq(OutboundOrder::getOrderType, queryDTO.getOrderType());
        }

        // 优先级筛选
        if (queryDTO.getPriority() != null) {
            wrapper.eq(OutboundOrder::getPriority, queryDTO.getPriority());
        }

        // 波次号查询
        if (StringUtils.hasText(queryDTO.getWaveNo())) {
            wrapper.eq(OutboundOrder::getWaveNo, queryDTO.getWaveNo());
        }

        // 物流公司筛选
        if (StringUtils.hasText(queryDTO.getLogisticsCompany())) {
            wrapper.eq(OutboundOrder::getLogisticsCompany, queryDTO.getLogisticsCompany());
        }

        // 状态筛选（支持多个状态，逗号分隔）
        if (StringUtils.hasText(queryDTO.getStatus())) {
            String[] statusArr = queryDTO.getStatus().split(",");
            if (statusArr.length == 1) {
                wrapper.eq(OutboundOrder::getStatus, Integer.parseInt(statusArr[0].trim()));
            } else {
                wrapper.in(OutboundOrder::getStatus,
                    Arrays.stream(statusArr)
                        .map(s -> Integer.parseInt(s.trim()))
                        .collect(Collectors.toList()));
            }
        }

        // 关键字搜索（出库单号/销售单号/客户名称）
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w
                .like(OutboundOrder::getOrderNo, queryDTO.getKeyword())
                .or().like(OutboundOrder::getSoNo, queryDTO.getKeyword())
                .or().like(OutboundOrder::getCustomerName, queryDTO.getKeyword())
            );
        }

        // 收货人信息搜索
        if (StringUtils.hasText(queryDTO.getReceiverName())) {
            wrapper.like(OutboundOrder::getReceiverName, queryDTO.getReceiverName());
        }
        if (StringUtils.hasText(queryDTO.getReceiverPhone())) {
            wrapper.like(OutboundOrder::getReceiverPhone, queryDTO.getReceiverPhone());
        }

        // 日期范围
        if (StringUtils.hasText(queryDTO.getStartDate())) {
            wrapper.ge(OutboundOrder::getCreateTime,
                LocalDate.parse(queryDTO.getStartDate()).atStartOfDay());
        }
        if (StringUtils.hasText(queryDTO.getEndDate())) {
            wrapper.le(OutboundOrder::getCreateTime,
                LocalDate.parse(queryDTO.getEndDate()).plusDays(1).atStartOfDay());
        }

        // 按创建时间倒序
        wrapper.orderByDesc(OutboundOrder::getCreateTime);

        // 分页查询
        IPage<OutboundOrder> page = orderRepository.selectPage(
            new Page<>(queryDTO.getPage(), queryDTO.getLimit()), wrapper);

        // 转换为Map列表
        List<Map<String, Object>> list = page.getRecords().stream()
            .map(this::orderToMap)
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        result.put("page", queryDTO.getPage());
        result.put("limit", queryDTO.getLimit());
        return result;
    }

    /**
     * 获取出库单详情
     */
    public Map<String, Object> getOrderDetail(Long id) {
        OutboundOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new RuntimeException("出库单不存在");
        }

        Map<String, Object> result = orderToMap(order);

        // 查询出库单明细
        LambdaQueryWrapper<OutboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OutboundOrderItem::getOrderId, id);
        List<OutboundOrderItem> items = itemRepository.selectList(itemWrapper);

        List<Map<String, Object>> itemList = items.stream()
            .map(this::itemToMap)
            .collect(Collectors.toList());

        result.put("items", itemList);
        return result;
    }

    /**
     * 取消出库单
     */
    @Transactional
    public void cancelOrder(Long id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("取消原因不能为空");
        }

        OutboundOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new RuntimeException("出库单不存在");
        }

        // 已发货的出库单不可取消
        if (order.getStatus() == OutboundOrder.STATUS_SHIPPED) {
            throw new IllegalStateException("已发货的出库单不可取消");
        }

        // 已分配状态需要释放锁定库存
        if (order.getStatus() == OutboundOrder.STATUS_ALLOCATED) {
            releaseLockedStock(order);
        }

        order.setStatus(OutboundOrder.STATUS_CANCELLED);
        order.setCancelReason(reason);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);

        log.info("出库单已取消: orderNo={}, reason={}", order.getOrderNo(), reason);
    }

    /**
     * 更新出库单
     */
    @Transactional
    public void updateOrder(Long id, OutboundOrderDTO dto) {
        OutboundOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new RuntimeException("出库单不存在");
        }

        // 只有待分配状态可以编辑
        if (!order.getStatus().equals(OutboundOrder.STATUS_PENDING)) {
            throw new IllegalStateException("已开始拣货的出库单不可编辑");
        }

        // 参数校验
        validateCreateOrder(dto);

        // 更新出库单基本信息
        order.setOrderType(dto.getOrderType());
        order.setSourceType(dto.getSourceType());
        order.setSoNo(dto.getSoNo());
        order.setCustomerId(dto.getCustomerId());
        order.setCustomerCode(dto.getCustomerCode());
        order.setCustomerName(dto.getCustomerName());
        order.setPriority(dto.getPriority());
        order.setLogisticsCompany(dto.getLogisticsCompany());
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setRemark(dto.getRemark());
        order.setUpdateTime(LocalDateTime.now());

        // 重新计算总出库数量
        int totalQty = dto.getItems().stream()
            .mapToInt(OutboundOrderItemDTO::getQty)
            .sum();
        order.setTotalQty(totalQty);

        orderRepository.updateById(order);

        // 删除原有明细
        LambdaQueryWrapper<OutboundOrderItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(OutboundOrderItem::getOrderId, id);
        itemRepository.delete(deleteWrapper);

        // 重新插入明细
        for (OutboundOrderItemDTO itemDTO : dto.getItems()) {
            OutboundOrderItem item = new OutboundOrderItem();
            item.setOrderId(id);
            item.setOrderNo(order.getOrderNo());
            item.setProductId(itemDTO.getProductId());
            item.setSkuCode(itemDTO.getSkuCode());
            item.setProductName(itemDTO.getProductName());
            item.setBarcode(itemDTO.getBarcode());
            item.setQty(itemDTO.getQty());
            item.setPickedQty(0);
            item.setPackedQty(0);
            item.setShippedQty(0);
            item.setLocationId(itemDTO.getLocationId());
            item.setLocationCode(itemDTO.getLocationCode());
            item.setBatchNo(itemDTO.getBatchNo());
            item.setStatus(OutboundOrderItem.STATUS_PENDING);
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            itemRepository.insert(item);
        }

        log.info("出库单更新成功: orderNo={}", order.getOrderNo());
    }

    /**
     * 删除出库单
     */
    @Transactional
    public void deleteOrder(Long id) {
        OutboundOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new RuntimeException("出库单不存在");
        }

        // 只有待分配状态可以删除
        if (!order.getStatus().equals(OutboundOrder.STATUS_PENDING)) {
            throw new IllegalStateException("只有待分配状态的出库单可以删除");
        }

        // 删除出库单明细
        LambdaQueryWrapper<OutboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OutboundOrderItem::getOrderId, id);
        itemRepository.delete(itemWrapper);

        // 删除出库单
        orderRepository.deleteById(id);

        log.info("出库单已删除: orderNo={}", order.getOrderNo());
    }

    /**
     * 设置优先级
     */
    @Transactional
    public void setPriority(Long id, Integer priority) {
        if (priority == null || priority < 1 || priority > 4) {
            throw new IllegalArgumentException("优先级无效，必须为1-4");
        }

        OutboundOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new RuntimeException("出库单不存在");
        }

        order.setPriority(priority);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);

        log.info("出库单优先级已更新: orderNo={}, priority={}", order.getOrderNo(), priority);
    }

    /**
     * 释放锁定库存
     */
    private void releaseLockedStock(OutboundOrder order) {
        // 查询出库单明细
        LambdaQueryWrapper<OutboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutboundOrderItem::getOrderId, order.getId());
        List<OutboundOrderItem> items = itemRepository.selectList(wrapper);

        // 释放每个商品的锁定库存
        for (OutboundOrderItem item : items) {
            if (item.getProductId() != null && item.getQty() != null && item.getQty() > 0) {
                boolean released = itemRepository.releaseLockedStock(item.getProductId(), item.getQty());
                if (!released) {
                    log.warn("释放库存失败: productId={}, qty={}", item.getProductId(), item.getQty());
                }
            }
        }

        log.info("出库单库存释放完成: orderNo={}", order.getOrderNo());
    }

    /**
     * 生成出库单号
     * 格式: OB + 年月日 + 4位序号
     * 示例: OB202606010001
     */
    private String generateOrderNo() {
        String datePrefix = "OB" + LocalDate.now().format(DATE_FORMATTER);
        Integer maxSeq = orderRepository.getMaxSeqByDate(datePrefix);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;
        return datePrefix + String.format("%04d", nextSeq);
    }

    /**
     * 生成销售订单号
     * 格式: SO + 年月日 + 4位序号
     * 示例: SO202606010001
     */
    private String generateSoNo() {
        String datePrefix = "SO" + LocalDate.now().format(DATE_FORMATTER);
        Integer maxSeq = orderRepository.getMaxSoSeqByDate(datePrefix);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;
        return datePrefix + String.format("%04d", nextSeq);
    }

    /**
     * 创建出库单参数校验
     */
    private void validateCreateOrder(OutboundOrderDTO dto) {
        // 出库类型校验
        if (!VALID_ORDER_TYPES.contains(dto.getOrderType())) {
            throw new IllegalArgumentException("出库类型无效");
        }

        // 来源类型校验
        if (!VALID_SOURCE_TYPES.contains(dto.getSourceType())) {
            throw new IllegalArgumentException("来源类型无效");
        }

        // 按出库类型校验关联方
        Integer orderType = dto.getOrderType();
        if (orderType == OutboundOrder.TYPE_SALES || orderType == OutboundOrder.TYPE_SAMPLE) {
            if (!StringUtils.hasText(dto.getCustomerName())) {
                throw new IllegalArgumentException("销售出库和样品出库必须选择客户");
            }
        } else if (orderType == OutboundOrder.TYPE_TRANSFER) {
            if (dto.getTargetWarehouseId() == null) {
                throw new IllegalArgumentException("调拨出库必须选择目标仓库");
            }
            if (dto.getTargetWarehouseId().equals(dto.getWarehouseId())) {
                throw new IllegalArgumentException("目标仓库不能与出库仓库相同");
            }
        } else if (orderType == OutboundOrder.TYPE_RETURN) {
            if (dto.getSupplierId() == null) {
                throw new IllegalArgumentException("退货出库必须选择供应商");
            }
        }

        // 仓库ID必填
        if (dto.getWarehouseId() == null) {
            throw new IllegalArgumentException("仓库不能为空");
        }

        // 商品明细校验
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("商品明细不能为空");
        }

        // 商品明细逐条校验
        for (OutboundOrderItemDTO item : dto.getItems()) {
            if (!StringUtils.hasText(item.getSkuCode())) {
                throw new IllegalArgumentException("SKU编码不能为空");
            }
            if (item.getQty() == null || item.getQty() <= 0) {
                throw new IllegalArgumentException("数量必须大于0");
            }

            // 库存校验
            if (item.getProductId() != null) {
                Integer availableStock = itemRepository.getAvailableStock(item.getProductId());
                if (availableStock != null && availableStock < item.getQty()) {
                    throw new IllegalStateException("库存不足，当前可用库存：" + availableStock + "件");
                }
            }
        }
    }

    /**
     * 出库单实体转Map
     */
    private Map<String, Object> orderToMap(OutboundOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("orderType", order.getOrderType());
        map.put("orderTypeName", getOrderTypeName(order.getOrderType()));
        map.put("sourceType", order.getSourceType());
        map.put("sourceTypeName", getSourceTypeName(order.getSourceType()));
        map.put("soNo", order.getSoNo());
        map.put("customerId", order.getCustomerId());
        map.put("customerCode", order.getCustomerCode());
        map.put("customerName", order.getCustomerName());
        map.put("customerPhone", order.getCustomerPhone());
        map.put("customerAddress", order.getCustomerAddress());
        map.put("supplierId", order.getSupplierId());
        map.put("supplierCode", order.getSupplierCode());
        map.put("supplierName", order.getSupplierName());
        map.put("targetWarehouseId", order.getTargetWarehouseId());
        map.put("targetWarehouseCode", order.getTargetWarehouseCode());
        map.put("targetWarehouseName", order.getTargetWarehouseName());
        map.put("transferInboundId", order.getTransferInboundId());
        map.put("transferInboundNo", order.getTransferInboundNo());
        map.put("warehouseId", order.getWarehouseId());
        map.put("warehouseCode", order.getWarehouseCode());
        map.put("warehouseName", order.getWarehouseName());
        map.put("priority", order.getPriority());
        map.put("priorityName", getPriorityName(order.getPriority()));
        map.put("logisticsCompany", order.getLogisticsCompany());
        map.put("trackingNo", order.getTrackingNo());
        map.put("receiverName", order.getReceiverName());
        map.put("receiverPhone", order.getReceiverPhone());
        map.put("receiverAddress", order.getReceiverAddress());
        map.put("status", order.getStatus());
        map.put("statusName", getStatusName(order.getStatus()));
        map.put("totalQty", order.getTotalQty());
        map.put("totalPickedQty", order.getTotalPickedQty());
        map.put("totalPackedQty", order.getTotalPackedQty());
        map.put("totalShippedQty", order.getTotalShippedQty());
        map.put("waveId", order.getWaveId());
        map.put("waveNo", order.getWaveNo());
        map.put("shipUserName", order.getShipUserName());
        map.put("shipTime", order.getShipTime());
        map.put("shippedTime", order.getShippedTime());
        map.put("remark", order.getRemark());
        map.put("cancelReason", order.getCancelReason());
        map.put("createTime", order.getCreateTime());
        map.put("completeTime", order.getCompleteTime());

        // 计算进度百分比
        int progressPick = order.getTotalQty() > 0 ?
            (order.getTotalPickedQty() * 100 / order.getTotalQty()) : 0;
        int progressPack = order.getTotalPickedQty() > 0 ?
            (order.getTotalPackedQty() * 100 / order.getTotalPickedQty()) : 0;
        int progressShip = order.getTotalPackedQty() > 0 ?
            (order.getTotalShippedQty() * 100 / order.getTotalPackedQty()) : 0;

        map.put("progressPick", progressPick);
        map.put("progressPack", progressPack);
        map.put("progressShip", progressShip);

        return map;
    }

    /**
     * 出库单明细实体转Map
     */
    private Map<String, Object> itemToMap(OutboundOrderItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("orderId", item.getOrderId());
        map.put("orderNo", item.getOrderNo());
        map.put("productId", item.getProductId());
        map.put("skuCode", item.getSkuCode());
        map.put("productName", item.getProductName());
        map.put("barcode", item.getBarcode());
        map.put("qty", item.getQty());
        map.put("pickedQty", item.getPickedQty() != null ? item.getPickedQty() : 0);
        map.put("packedQty", item.getPackedQty() != null ? item.getPackedQty() : 0);
        map.put("shippedQty", item.getShippedQty() != null ? item.getShippedQty() : 0);
        map.put("locationId", item.getLocationId());
        map.put("locationCode", item.getLocationCode());
        map.put("batchNo", item.getBatchNo());
        map.put("status", item.getStatus());
        map.put("statusName", getItemStatusName(item.getStatus()));
        map.put("diffReason", item.getDiffReason());
        map.put("pickTime", item.getPickTime());
        map.put("packTime", item.getPackTime());
        map.put("shipTime", item.getShipTime());
        return map;
    }

    /**
     * 获取出库类型名称
     */
    private String getOrderTypeName(Integer orderType) {
        if (orderType == null) return "";
        switch (orderType) {
            case 1: return "销售出库";
            case 2: return "调拨出库";
            case 3: return "退货出库";
            case 4: return "报废出库";
            case 5: return "样品出库";
            default: return "";
        }
    }

    /**
     * 获取来源类型名称
     */
    private String getSourceTypeName(Integer sourceType) {
        if (sourceType == null) return "";
        switch (sourceType) {
            case 1: return "ERP推送";
            case 2: return "手工创建";
            case 3: return "调拨申请";
            default: return "";
        }
    }

    /**
     * 获取优先级名称
     */
    private String getPriorityName(Integer priority) {
        if (priority == null) return "";
        switch (priority) {
            case 1: return "紧急";
            case 2: return "高";
            case 3: return "中";
            case 4: return "低";
            default: return "";
        }
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待分配";
            case 1: return "已分配";
            case 2: return "拣货中";
            case 3: return "待打包";
            case 4: return "待发货";
            case 5: return "已发货";
            case 9: return "已取消";
            default: return "";
        }
    }

    /**
     * 获取明细状态名称
     */
    private String getItemStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待拣货";
            case 1: return "拣货中";
            case 2: return "已拣货";
            case 3: return "已打包";
            case 4: return "已发货";
            case 9: return "已取消";
            default: return "";
        }
    }

    /**
     * 分配出库单（直接进入拣货状态）
     * 简化流程：创建波次并立即释放，跳过手动波次管理步骤
     */
    @Transactional
    public Map<String, Object> allocateOrder(Long id) {
        OutboundOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new RuntimeException("出库单不存在");
        }

        // 只有待分配状态可以分配
        if (order.getStatus() != OutboundOrder.STATUS_PENDING) {
            throw new IllegalStateException("只有待分配状态的出库单可以分配");
        }

        // 查询出库单明细
        LambdaQueryWrapper<OutboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OutboundOrderItem::getOrderId, id);
        List<OutboundOrderItem> items = itemRepository.selectList(itemWrapper);

        if (items.isEmpty()) {
            throw new IllegalStateException("出库单明细为空，无法分配");
        }

        // 验证库存是否充足
        for (OutboundOrderItem item : items) {
            if (item.getProductId() != null) {
                Integer availableStock = itemRepository.getAvailableStock(item.getProductId());
                if (availableStock == null || availableStock < item.getQty()) {
                    throw new IllegalStateException(
                        "商品 " + item.getSkuCode() + " 库存不足，可用库存: " + (availableStock != null ? availableStock : 0) + "件");
                }
            }
        }

        // 更新出库单状态为拣货中
        order.setStatus(OutboundOrder.STATUS_PICKING);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);

        // 更新明细状态为拣货中，并生成拣货记录
        for (OutboundOrderItem item : items) {
            item.setStatus(OutboundOrderItem.STATUS_PICKING);
            item.setUpdateTime(LocalDateTime.now());
            itemRepository.updateById(item);

            // 生成拣货记录
            createPickRecord(order, item);
        }

        log.info("出库单分配成功，进入拣货状态: orderNo={}, status=拣货中", order.getOrderNo());

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", id);
        result.put("orderNo", order.getOrderNo());
        result.put("newStatus", OutboundOrder.STATUS_PICKING);
        result.put("statusName", "拣货中");
        return result;
    }

    /**
     * 创建拣货记录
     */
    private void createPickRecord(OutboundOrder order, OutboundOrderItem item) {
        PickRecord record = new PickRecord();
        record.setOutboundOrderId(order.getId());
        record.setOutboundOrderNo(order.getOrderNo());
        record.setOutboundItemId(item.getId());
        record.setProductId(item.getProductId());
        record.setSkuCode(item.getSkuCode());
        record.setProductName(item.getProductName());
        // 如果没有条码，使用SKU编码作为条码
        record.setBarcode(item.getBarcode() != null ? item.getBarcode() : item.getSkuCode());
        record.setWarehouseId(order.getWarehouseId());
        // 如果明细有库位则使用，否则使用默认库位
        record.setLocationId(item.getLocationId() != null ? item.getLocationId() : 1L);
        record.setLocationCode(item.getLocationCode() != null ? item.getLocationCode() : "ZN-A-01-01-01");
        record.setBatchNo(item.getBatchNo());
        record.setPlanQty(item.getQty());
        record.setActualQty(null);
        record.setDiffQty(null);
        record.setLocationScanned(PickRecord.SCAN_NO);
        record.setProductScanned(PickRecord.SCAN_NO);
        record.setStatus(PickRecord.STATUS_PENDING);
        record.setIsException(0);
        record.setSortOrder(0);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        pickRecordRepository.insert(record);
        log.debug("生成拣货记录: orderId={}, sku={}, qty={}", order.getId(), item.getSkuCode(), item.getQty());
    }
}