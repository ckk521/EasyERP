package com.wms.inbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.inbound.dto.InboundOrderDTO;
import com.wms.inbound.dto.InboundOrderItemDTO;
import com.wms.inbound.dto.InboundOrderQueryDTO;
import com.wms.inbound.entity.InboundOrder;
import com.wms.inbound.entity.InboundOrderItem;
import com.wms.inbound.repository.InboundOrderItemRepository;
import com.wms.inbound.repository.InboundOrderRepository;
import com.wms.inbound.repository.ReceiveRecordRepository;
import com.wms.inbound.repository.InspectRecordRepository;
import com.wms.inbound.repository.PutawayRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 入库单服务
 * 实现阶段二: 入库单CRUD + 送货批次号
 */
@Service
@RequiredArgsConstructor
public class InboundOrderService {

    private final InboundOrderRepository orderRepository;
    private final InboundOrderItemRepository itemRepository;
    private final ReceiveRecordRepository receiveRecordRepository;
    private final InspectRecordRepository inspectRecordRepository;
    private final PutawayRecordRepository putawayRecordRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 有效的入库类型
    private static final Set<Integer> VALID_ORDER_TYPES = Set.of(
        InboundOrder.TYPE_PURCHASE,
        InboundOrder.TYPE_RETURN,
        InboundOrder.TYPE_TRANSFER,
        InboundOrder.TYPE_GIFT,
        InboundOrder.TYPE_OTHER
    );

    /**
     * 创建入库单
     */
    @Transactional
    public Long createOrder(InboundOrderDTO dto) {
        // 参数校验
        validateCreateOrder(dto);

        // 生成入库单号
        String orderNo = generateOrderNo();

        // 构建入库单实体
        InboundOrder order = new InboundOrder();
        order.setOrderNo(orderNo);
        order.setDeliveryBatchNo(dto.getDeliveryBatchNo());
        order.setOrderType(dto.getOrderType());
        order.setPoNo(dto.getPoNo());
        order.setSupplierId(dto.getSupplierId());
        order.setSupplierCode(dto.getSupplierCode());
        order.setSupplierName(dto.getSupplierName());
        order.setWarehouseId(dto.getWarehouseId());
        order.setWarehouseCode(dto.getWarehouseCode());
        order.setWarehouseName(dto.getWarehouseName());
        order.setExpectedDate(dto.getExpectedDate());
        order.setStatus(InboundOrder.STATUS_PENDING);
        order.setRemark(dto.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 计算总预期数量
        int totalExpectedQty = dto.getItems().stream()
            .mapToInt(InboundOrderItemDTO::getExpectedQty)
            .sum();
        order.setTotalExpectedQty(totalExpectedQty);
        order.setTotalReceivedQty(0);
        order.setTotalQualifiedQty(0);
        order.setTotalRejectedQty(0);
        order.setTotalPutawayQty(0);
        order.setTotalReturnQty(0);
        order.setProgressReceive(0);
        order.setProgressInspect(0);
        order.setProgressPutaway(0);

        // 保存入库单
        orderRepository.insert(order);

        // 保存入库单明细
        for (InboundOrderItemDTO itemDTO : dto.getItems()) {
            InboundOrderItem item = new InboundOrderItem();
            item.setOrderId(order.getId());
            item.setOrderNo(orderNo);
            item.setProductId(itemDTO.getProductId());
            item.setSkuCode(itemDTO.getSkuCode());
            item.setProductName(itemDTO.getProductName());
            item.setBarcode(itemDTO.getBarcode());
            item.setExpectedQty(itemDTO.getExpectedQty());
            item.setReceivedQty(0);
            item.setQualifiedQty(0);
            item.setRejectedQty(0);
            item.setPutawayQty(0);
            item.setReturnQty(0);
            item.setStatus(InboundOrderItem.STATUS_PENDING);
            itemRepository.insert(item);
        }

        return order.getId();
    }

    /**
     * 分页查询入库单列表
     */
    public Map<String, Object> listOrders(InboundOrderQueryDTO queryDTO) {
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();

        // 入库单号精确查询
        if (StringUtils.hasText(queryDTO.getOrderNo())) {
            wrapper.eq(InboundOrder::getOrderNo, queryDTO.getOrderNo());
        }

        // 采购订单号查询
        if (StringUtils.hasText(queryDTO.getPoNo())) {
            wrapper.eq(InboundOrder::getPoNo, queryDTO.getPoNo());
        }

        // 送货批次号查询
        if (StringUtils.hasText(queryDTO.getDeliveryBatchNo())) {
            wrapper.eq(InboundOrder::getDeliveryBatchNo, queryDTO.getDeliveryBatchNo());
        }

        // 供应商筛选
        if (queryDTO.getSupplierId() != null) {
            wrapper.eq(InboundOrder::getSupplierId, queryDTO.getSupplierId());
        }

        // 仓库筛选
        if (queryDTO.getWarehouseId() != null) {
            wrapper.eq(InboundOrder::getWarehouseId, queryDTO.getWarehouseId());
        }

        // 入库类型筛选
        if (queryDTO.getOrderType() != null) {
            wrapper.eq(InboundOrder::getOrderType, queryDTO.getOrderType());
        }

        // 状态筛选（支持多个状态，逗号分隔）
        if (StringUtils.hasText(queryDTO.getStatus())) {
            String[] statusArr = queryDTO.getStatus().split(",");
            if (statusArr.length == 1) {
                wrapper.eq(InboundOrder::getStatus, Integer.parseInt(statusArr[0].trim()));
            } else {
                wrapper.in(InboundOrder::getStatus,
                    java.util.Arrays.stream(statusArr)
                        .map(s -> Integer.parseInt(s.trim()))
                        .collect(java.util.stream.Collectors.toList()));
            }
        }

        // 关键字搜索（入库单号/采购单号/供应商名称）
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w
                .like(InboundOrder::getOrderNo, queryDTO.getKeyword())
                .or().like(InboundOrder::getPoNo, queryDTO.getKeyword())
                .or().like(InboundOrder::getSupplierName, queryDTO.getKeyword())
            );
        }

        // 日期范围
        if (StringUtils.hasText(queryDTO.getStartDate())) {
            wrapper.ge(InboundOrder::getCreateTime,
                LocalDate.parse(queryDTO.getStartDate()).atStartOfDay());
        }
        if (StringUtils.hasText(queryDTO.getEndDate())) {
            wrapper.le(InboundOrder::getCreateTime,
                LocalDate.parse(queryDTO.getEndDate()).plusDays(1).atStartOfDay());
        }

        // 按创建时间倒序
        wrapper.orderByDesc(InboundOrder::getCreateTime);

        // 分页查询
        IPage<InboundOrder> page = orderRepository.selectPage(
            new Page<>(queryDTO.getPage(), queryDTO.getLimit()), wrapper);

        // 转换为Map列表
        List<Map<String, Object>> list = page.getRecords().stream()
            .map(this::orderToMap)
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        return result;
    }

    /**
     * 获取入库单详情
     */
    public Map<String, Object> getOrderDetail(Long id) {
        InboundOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        Map<String, Object> result = orderToMap(order);

        // 查询入库单明细
        LambdaQueryWrapper<InboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InboundOrderItem::getOrderId, id);
        List<InboundOrderItem> items = itemRepository.selectList(itemWrapper);

        List<Map<String, Object>> itemList = items.stream()
            .map(this::itemToMap)
            .collect(Collectors.toList());

        result.put("items", itemList);
        return result;
    }

    /**
     * 更新入库单
     */
    @Transactional
    public void updateOrder(Long id, InboundOrderDTO dto) {
        InboundOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        // 只有待收货状态可以编辑
        if (!order.getStatus().equals(InboundOrder.STATUS_PENDING)) {
            throw new IllegalStateException("已开始收货的入库单不可编辑");
        }

        // 参数校验
        validateCreateOrder(dto);

        // 更新入库单基本信息
        order.setOrderType(dto.getOrderType());
        order.setPoNo(dto.getPoNo());
        order.setDeliveryBatchNo(dto.getDeliveryBatchNo());
        order.setSupplierId(dto.getSupplierId());
        order.setSupplierCode(dto.getSupplierCode());
        order.setSupplierName(dto.getSupplierName());
        order.setExpectedDate(dto.getExpectedDate());
        order.setRemark(dto.getRemark());
        order.setUpdateTime(LocalDateTime.now());

        // 重新计算总预期数量
        int totalExpectedQty = dto.getItems().stream()
            .mapToInt(InboundOrderItemDTO::getExpectedQty)
            .sum();
        order.setTotalExpectedQty(totalExpectedQty);

        orderRepository.updateById(order);

        // 删除原有明细
        LambdaQueryWrapper<InboundOrderItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(InboundOrderItem::getOrderId, id);
        itemRepository.delete(deleteWrapper);

        // 重新插入明细
        for (InboundOrderItemDTO itemDTO : dto.getItems()) {
            InboundOrderItem item = new InboundOrderItem();
            item.setOrderId(id);
            item.setOrderNo(order.getOrderNo());
            item.setProductId(itemDTO.getProductId());
            item.setSkuCode(itemDTO.getSkuCode());
            item.setProductName(itemDTO.getProductName());
            item.setBarcode(itemDTO.getBarcode());
            item.setExpectedQty(itemDTO.getExpectedQty());
            item.setReceivedQty(0);
            item.setQualifiedQty(0);
            item.setRejectedQty(0);
            item.setPutawayQty(0);
            item.setReturnQty(0);
            item.setStatus(InboundOrderItem.STATUS_PENDING);
            itemRepository.insert(item);
        }
    }

    /**
     * 取消入库单
     */
    @Transactional
    public void cancelOrder(Long id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("取消原因不能为空");
        }

        InboundOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        // 只有待收货状态可以取消
        if (!order.getStatus().equals(InboundOrder.STATUS_PENDING)) {
            throw new IllegalStateException("只有待收货状态的入库单可以取消");
        }

        order.setStatus(InboundOrder.STATUS_CANCELLED);
        order.setCancelReason(reason);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);
    }

    /**
     * 重新激活已取消的入库单
     */
    @Transactional
    public void reactivateOrder(Long id) {
        InboundOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        // 只有已取消状态可以重新激活
        if (!order.getStatus().equals(InboundOrder.STATUS_CANCELLED)) {
            throw new IllegalStateException("只有已取消的入库单可以重新激活");
        }

        order.setStatus(InboundOrder.STATUS_PENDING);
        order.setCancelReason(null);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);
    }

    /**
     * 删除入库单
     */
    @Transactional
    public void deleteOrder(Long id) {
        InboundOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        // 只有待收货状态可以删除
        if (!order.getStatus().equals(InboundOrder.STATUS_PENDING)) {
            throw new IllegalStateException("只有待收货状态的入库单可以删除");
        }

        // 删除入库单明细
        LambdaQueryWrapper<InboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InboundOrderItem::getOrderId, id);
        itemRepository.delete(itemWrapper);

        // 删除入库单
        orderRepository.deleteById(id);
    }

    /**
     * 生成入库单号
     * 格式: IN + 年月日 + 4位序号
     * 示例: IN202604230001
     */
    private String generateOrderNo() {
        String datePrefix = "IN" + LocalDate.now().format(DATE_FORMATTER);
        Integer maxSeq = orderRepository.getMaxSeqByDate(datePrefix);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;
        return datePrefix + String.format("%04d", nextSeq);
    }

    /**
     * 创建入库单参数校验
     */
    private void validateCreateOrder(InboundOrderDTO dto) {
        // 入库类型校验
        if (!VALID_ORDER_TYPES.contains(dto.getOrderType())) {
            throw new IllegalArgumentException("入库类型无效");
        }

        // 采购入库必须有采购订单号
        if (dto.getOrderType().equals(InboundOrder.TYPE_PURCHASE) && !StringUtils.hasText(dto.getPoNo())) {
            throw new IllegalArgumentException("采购入库必须填写采购订单号");
        }

        // 仓库ID必填
        if (dto.getWarehouseId() == null) {
            throw new IllegalArgumentException("仓库不能为空");
        }

        // 送货批次号长度校验
        if (dto.getDeliveryBatchNo() != null && dto.getDeliveryBatchNo().length() > 50) {
            throw new IllegalArgumentException("送货批次号长度不能超过50");
        }

        // 商品明细校验
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("商品明细不能为空");
        }

        // 商品明细逐条校验
        for (InboundOrderItemDTO item : dto.getItems()) {
            if (!StringUtils.hasText(item.getSkuCode())) {
                throw new IllegalArgumentException("SKU编码不能为空");
            }
            if (item.getExpectedQty() == null || item.getExpectedQty() <= 0) {
                throw new IllegalArgumentException("预期数量必须大于0");
            }
        }
    }

    /**
     * 入库单实体转Map
     */
    private Map<String, Object> orderToMap(InboundOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("deliveryBatchNo", order.getDeliveryBatchNo());
        map.put("orderType", order.getOrderType());
        map.put("orderTypeName", getOrderTypeName(order.getOrderType()));
        map.put("poNo", order.getPoNo());
        map.put("supplierId", order.getSupplierId());
        map.put("supplierCode", order.getSupplierCode());
        map.put("supplierName", order.getSupplierName());
        map.put("warehouseId", order.getWarehouseId());
        map.put("warehouseCode", order.getWarehouseCode());
        map.put("warehouseName", order.getWarehouseName());
        map.put("expectedDate", order.getExpectedDate());
        map.put("actualArrivalDate", order.getActualArrivalDate());
        map.put("status", order.getStatus());
        map.put("statusName", getStatusName(order.getStatus()));
        map.put("progressReceive", order.getProgressReceive());
        map.put("progressInspect", order.getProgressInspect());
        map.put("progressPutaway", order.getProgressPutaway());
        map.put("totalExpectedQty", order.getTotalExpectedQty());
        map.put("totalReceivedQty", order.getTotalReceivedQty());
        map.put("totalQualifiedQty", order.getTotalQualifiedQty());
        map.put("totalRejectedQty", order.getTotalRejectedQty());
        map.put("totalPutawayQty", order.getTotalPutawayQty());
        map.put("totalReturnQty", order.getTotalReturnQty());
        map.put("remark", order.getRemark());
        map.put("cancelReason", order.getCancelReason());
        map.put("createTime", order.getCreateTime());
        map.put("completeTime", order.getCompleteTime());
        return map;
    }

    /**
     * 入库单明细实体转Map
     */
    private Map<String, Object> itemToMap(InboundOrderItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("orderId", item.getOrderId());
        map.put("orderNo", item.getOrderNo());
        map.put("productId", item.getProductId());
        map.put("skuCode", item.getSkuCode());
        map.put("productName", item.getProductName());
        map.put("barcode", item.getBarcode());
        map.put("expectedQty", item.getExpectedQty());

        // 从记录表汇总计算数量
        Integer receivedQty = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
        Integer qualifiedQty = inspectRecordRepository.sumQualifiedQtyByItemId(item.getId());
        Integer rejectedQty = inspectRecordRepository.sumRejectedQtyByItemId(item.getId());
        Integer putawayQty = putawayRecordRepository.sumPutawayQtyByItemId(item.getId());

        map.put("receivedQty", receivedQty != null ? receivedQty : 0);
        map.put("qualifiedQty", qualifiedQty != null ? qualifiedQty : 0);
        map.put("rejectedQty", rejectedQty != null ? rejectedQty : 0);
        map.put("putawayQty", putawayQty != null ? putawayQty : 0);
        map.put("returnQty", item.getReturnQty());
        map.put("batchNo", item.getBatchNo());
        map.put("productionDate", item.getProductionDate());
        map.put("expiryDate", item.getExpiryDate());
        map.put("status", item.getStatus());
        map.put("statusName", getItemStatusName(item.getStatus()));
        map.put("diffReason", item.getDiffReason());
        map.put("receiveTime", item.getReceiveTime());
        map.put("inspectTime", item.getInspectTime());
        map.put("putawayTime", item.getPutawayTime());
        return map;
    }

    /**
     * 获取入库类型名称
     */
    private String getOrderTypeName(Integer orderType) {
        if (orderType == null) return "";
        switch (orderType) {
            case 1: return "采购入库";
            case 2: return "退货入库";
            case 3: return "调拨入库";
            case 4: return "赠品入库";
            case 5: return "其他入库";
            default: return "";
        }
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待收货";
            case 1: return "收货中";
            case 2: return "验收中";
            case 3: return "待上架";
            case 4: return "已完成";
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
            case 0: return "待收货";
            case 1: return "已收货";
            case 2: return "已验收";
            case 3: return "已上架";
            case 4: return "部分退货";
            case 5: return "全部退货";
            default: return "";
        }
    }
}
