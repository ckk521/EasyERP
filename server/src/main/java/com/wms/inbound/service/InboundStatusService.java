package com.wms.inbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.exception.repository.ExceptionItemRepository;
import com.wms.inbound.entity.InboundOrder;
import com.wms.inbound.entity.InboundOrderItem;
import com.wms.inbound.repository.InboundOrderItemRepository;
import com.wms.inbound.repository.InboundOrderRepository;
import com.wms.inbound.repository.InspectRecordRepository;
import com.wms.inbound.repository.PutawayRecordRepository;
import com.wms.inbound.repository.ReceiveRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 入库单状态计算服务
 *
 * 核心原则：
 * 1. 所有数量数据从记录表实时计算
 * 2. 状态根据实际进度自动确定
 * 3. 提供统一的状态计算入口，避免各服务独立计算导致不一致
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InboundStatusService {

    private final InboundOrderRepository orderRepository;
    private final InboundOrderItemRepository itemRepository;
    private final ReceiveRecordRepository receiveRecordRepository;
    private final InspectRecordRepository inspectRecordRepository;
    private final PutawayRecordRepository putawayRecordRepository;
    private final ExceptionItemRepository exceptionItemRepository;

    /**
     * 重新计算入库单的状态和进度
     *
     * 业务规则：
     * 1. 待收货：没有任何收货记录
     * 2. 收货中：有收货记录，但收货+隔离 < 预期
     * 3. 验收中：收货完成，但验收 < 收货
     * 4. 待上架：验收完成，但上架 < 合格
     * 5. 已完成：上架完成
     *
     * @param orderId 入库单ID
     * @return 更新后的入库单
     */
    @Transactional
    public InboundOrder recalculateStatus(Long orderId) {
        InboundOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        // 1. 计算预期数量
        LambdaQueryWrapper<InboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InboundOrderItem::getOrderId, orderId);
        var items = itemRepository.selectList(itemWrapper);
        int totalExpected = items.stream().mapToInt(InboundOrderItem::getExpectedQty).sum();

        // 2. 从记录表汇总各项数量
        Integer totalReceived = receiveRecordRepository.sumReceiveQtyByOrderId(orderId);
        Integer totalQualified = inspectRecordRepository.sumQualifiedQtyByOrderId(orderId);
        Integer totalRejected = inspectRecordRepository.sumRejectedQtyByOrderId(orderId);
        Integer totalPutaway = putawayRecordRepository.sumPutawayQtyByOrderId(orderId);
        Integer totalIsolated = exceptionItemRepository.sumIsolatedQtyByOrderId(orderId);

        if (totalReceived == null) totalReceived = 0;
        if (totalQualified == null) totalQualified = 0;
        if (totalRejected == null) totalRejected = 0;
        if (totalPutaway == null) totalPutaway = 0;
        if (totalIsolated == null) totalIsolated = 0;

        int totalInspected = totalQualified + totalRejected;

        // 3. 计算进度百分比
        int progressReceive = totalExpected > 0 ? ((totalReceived + totalIsolated) * 100 / totalExpected) : 0;
        int progressInspect = totalReceived > 0 ? (totalInspected * 100 / totalReceived) : 0;
        int progressPutaway = totalQualified > 0 ? (totalPutaway * 100 / totalQualified) : 0;

        // 4. 更新数量和进度
        order.setTotalExpectedQty(totalExpected);
        order.setTotalReceivedQty(totalReceived);
        order.setTotalQualifiedQty(totalQualified);
        order.setTotalRejectedQty(totalRejected);
        order.setTotalPutawayQty(totalPutaway);
        order.setProgressReceive(progressReceive);
        order.setProgressInspect(progressInspect);
        order.setProgressPutaway(progressPutaway);

        // 5. 根据实际进度确定状态
        int newStatus = calculateStatus(totalExpected, totalReceived, totalIsolated, totalInspected, totalQualified, totalPutaway);
        int oldStatus = order.getStatus();

        // 更新状态
        order.setStatus(newStatus);

        // 处理完成时间：只有真正完成时才设置，否则清除
        if (newStatus == InboundOrder.STATUS_COMPLETED) {
            if (oldStatus != InboundOrder.STATUS_COMPLETED) {
                order.setCompleteTime(LocalDateTime.now());
                log.info("入库单 {} 状态变更: {} -> 已完成", order.getOrderNo(), getStatusName(oldStatus));
            }
        } else {
            // 非完成状态时，清除完成时间
            order.setCompleteTime(null);
            if (oldStatus == InboundOrder.STATUS_COMPLETED) {
                log.info("入库单 {} 状态回退: 已完成 -> {}", order.getOrderNo(), getStatusName(newStatus));
            } else if (newStatus != oldStatus) {
                log.info("入库单 {} 状态变更: {} -> {}", order.getOrderNo(), getStatusName(oldStatus), getStatusName(newStatus));
            }
        }

        orderRepository.updateById(order);
        return order;
    }

    /**
     * 根据实际进度计算状态
     *
     * 核心原则：从后向前判断，但必须确保前置阶段完成才能进入下一阶段
     */
    private int calculateStatus(int totalExpected, int totalReceived, int totalIsolated,
                                 int totalInspected, int totalQualified, int totalPutaway) {
        // 判断各阶段是否完成
        boolean receiveComplete = (totalReceived + totalIsolated) >= totalExpected;
        boolean inspectComplete = totalInspected >= totalReceived && totalReceived > 0;
        boolean putawayComplete = totalPutaway >= totalQualified && totalQualified > 0;

        // 已完成：收货完成 && 验收完成 && 上架完成
        // 关键：必须确保收货完成，否则不能算已完成
        if (receiveComplete && inspectComplete && putawayComplete) {
            return InboundOrder.STATUS_COMPLETED;
        }

        // 待上架：验收完成，但上架未完成
        // 注意：即使收货未完成，如果已有验收数据，也应该在待上架状态
        if (inspectComplete && totalQualified > 0 && !putawayComplete) {
            return InboundOrder.STATUS_PUTAWAY;
        }

        // 验收中：有收货记录，但验收未完成
        if (totalReceived > 0 && !inspectComplete) {
            return InboundOrder.STATUS_INSPECTING;
        }

        // 收货中：有收货记录或隔离记录，但收货未完成
        if ((totalReceived > 0 || totalIsolated > 0) && !receiveComplete) {
            return InboundOrder.STATUS_RECEIVING;
        }

        // 待收货：没有任何收货记录
        return InboundOrder.STATUS_PENDING;
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(int status) {
        switch (status) {
            case 0: return "待收货";
            case 1: return "收货中";
            case 2: return "验收中";
            case 3: return "待上架";
            case 4: return "已完成";
            case 9: return "已取消";
            default: return "未知";
        }
    }

    /**
     * 获取入库单的当前进度摘要
     * 用于调试和日志
     */
    public String getProgressSummary(Long orderId) {
        InboundOrder order = orderRepository.selectById(orderId);
        if (order == null) return "入库单不存在";

        LambdaQueryWrapper<InboundOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InboundOrderItem::getOrderId, orderId);
        var items = itemRepository.selectList(itemWrapper);
        int totalExpected = items.stream().mapToInt(InboundOrderItem::getExpectedQty).sum();

        Integer totalReceived = receiveRecordRepository.sumReceiveQtyByOrderId(orderId);
        Integer totalQualified = inspectRecordRepository.sumQualifiedQtyByOrderId(orderId);
        Integer totalPutaway = putawayRecordRepository.sumPutawayQtyByOrderId(orderId);
        Integer totalIsolated = exceptionItemRepository.sumIsolatedQtyByOrderId(orderId);

        return String.format("入库单 %s: 预期=%d, 收货=%d, 隔离=%d, 合格=%d, 上架=%d, 状态=%s",
            order.getOrderNo(),
            totalExpected,
            totalReceived != null ? totalReceived : 0,
            totalIsolated != null ? totalIsolated : 0,
            totalQualified != null ? totalQualified : 0,
            totalPutaway != null ? totalPutaway : 0,
            getStatusName(order.getStatus())
        );
    }
}
