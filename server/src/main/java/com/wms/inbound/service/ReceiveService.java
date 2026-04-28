package com.wms.inbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.inbound.dto.ReceiveDTO;
import com.wms.inbound.entity.InboundOrder;
import com.wms.inbound.entity.InboundOrderItem;
import com.wms.inbound.entity.ReceiveRecord;
import com.wms.inbound.repository.InboundOrderItemRepository;
import com.wms.inbound.repository.InboundOrderRepository;
import com.wms.inbound.repository.ReceiveRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收货服务
 * 阶段三: 收货作业 + 差异处理
 *
 * 改为记录式存储：
 * - 每次收货插入一条记录到 wms_receive_record
 * - 入库明细的 receivedQty 通过汇总计算
 */
@Service
@RequiredArgsConstructor
public class ReceiveService {

    private final InboundOrderRepository orderRepository;
    private final InboundOrderItemRepository itemRepository;
    private final ReceiveRecordRepository receiveRecordRepository;

    /** 差异阈值百分比(10%) */
    private static final double DIFF_THRESHOLD = 0.10;

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

        // 计算待收货数量和差异
        Integer alreadyReceived = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
        if (alreadyReceived == null) alreadyReceived = 0;
        int pendingQty = item.getExpectedQty() - alreadyReceived;
        int diff = dto.getReceivedQty() - pendingQty;

        // 差异校验（基于待收货数量）
        validateDifference(pendingQty, dto.getReceivedQty(), dto.getDiffReason());

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
        record.setHasException(0);
        record.setReceiveTime(LocalDateTime.now());
        record.setReceiveUser(userId);
        // TODO: 从用户服务获取用户名
        record.setReceiveUserName(username != null ? username : "操作员");
        receiveRecordRepository.insert(record);

        // 更新入库单状态和进度
        updateOrderAfterReceive(dto.getOrderId(), order);
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
        if (dto.getReceivedQty() == null || dto.getReceivedQty() <= 0) {
            throw new IllegalArgumentException("收货数量必须大于0");
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
     * 收货后更新入库单状态和进度
     */
    private void updateOrderAfterReceive(Long orderId, InboundOrder order) {
        // 更新入库单状态为收货中
        if (order.getStatus() == InboundOrder.STATUS_PENDING) {
            order.setStatus(InboundOrder.STATUS_RECEIVING);
        }

        // 更新进度
        updateOrderProgressInternal(orderId, order);

        orderRepository.updateById(order);
    }

    /**
     * 检查并更新入库单状态
     * TC-2.5.5 全部商品收货完成后状态变为验收中
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

        // 检查是否全部已收货（通过记录表汇总）
        boolean allReceived = items.stream()
            .allMatch(item -> {
                Integer received = receiveRecordRepository.sumReceiveQtyByItemId(item.getId());
                return received != null && received >= item.getExpectedQty();
            });

        if (allReceived) {
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

        // 计算进度百分比
        int progress = totalExpected > 0 ? (totalReceived * 100 / totalExpected) : 0;

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

        List<Map<String, Object>> list = pageResult.getRecords().stream()
            .map(this::orderToMap)
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", pageResult.getTotal());
        return result;
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
}
