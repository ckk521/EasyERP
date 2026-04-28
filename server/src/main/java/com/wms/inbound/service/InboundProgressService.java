package com.wms.inbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.inbound.entity.InboundOrder;
import com.wms.inbound.entity.InboundOrderItem;
import com.wms.inbound.entity.RejectRecord;
import com.wms.inbound.repository.InboundOrderItemRepository;
import com.wms.inbound.repository.InboundOrderRepository;
import com.wms.inbound.repository.RejectRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 入库进度服务
 * 阶段七: 进度展示 + 统计报表
 */
@Service
@RequiredArgsConstructor
public class InboundProgressService {

    private final InboundOrderRepository orderRepository;
    private final InboundOrderItemRepository itemRepository;
    private final RejectRecordRepository rejectRecordRepository;

    /**
     * 查询入库进度
     * TC-2.13
     */
    public Map<String, Object> getInboundProgress(Long orderId) {
        InboundOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("deliveryBatchNo", order.getDeliveryBatchNo());
        result.put("supplierName", order.getSupplierName());
        result.put("status", order.getStatus());
        result.put("statusName", getStatusName(order.getStatus()));

        // 入库进度
        Map<String, Object> progress = new HashMap<>();
        progress.put("receive", buildProgressItem(
            order.getTotalReceivedQty(), order.getTotalExpectedQty()));
        progress.put("inspect", buildProgressItem(
            order.getTotalQualifiedQty() + order.getTotalRejectedQty(), order.getTotalReceivedQty()));
        progress.put("putaway", buildProgressItem(
            order.getTotalPutawayQty(), order.getTotalQualifiedQty()));
        progress.put("return", buildProgressItem(
            order.getTotalReturnQty(), order.getTotalExpectedQty()));
        result.put("progress", progress);

        // 商品明细进度
        List<Map<String, Object>> itemProgress = getItemProgress(orderId);
        result.put("items", itemProgress);

        // 不合格记录
        List<Map<String, Object>> rejects = getRejectRecords(orderId);
        result.put("rejects", rejects);

        return result;
    }

    /**
     * 按供应商+送货批次号查询入库进度
     * TC-2.13.1
     */
    public Map<String, Object> getProgressByBatch(String supplierName, String deliveryBatchNo) {
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (supplierName != null) {
            wrapper.like(InboundOrder::getSupplierName, supplierName);
        }
        if (deliveryBatchNo != null) {
            wrapper.eq(InboundOrder::getDeliveryBatchNo, deliveryBatchNo);
        }

        List<InboundOrder> orders = orderRepository.selectList(wrapper);

        // 汇总统计
        int totalExpected = orders.stream().mapToInt(o -> o.getTotalExpectedQty() != null ? o.getTotalExpectedQty() : 0).sum();
        int totalReceived = orders.stream().mapToInt(o -> o.getTotalReceivedQty() != null ? o.getTotalReceivedQty() : 0).sum();
        int totalQualified = orders.stream().mapToInt(o -> o.getTotalQualifiedQty() != null ? o.getTotalQualifiedQty() : 0).sum();
        int totalRejected = orders.stream().mapToInt(o -> o.getTotalRejectedQty() != null ? o.getTotalRejectedQty() : 0).sum();
        int totalPutaway = orders.stream().mapToInt(o -> o.getTotalPutawayQty() != null ? o.getTotalPutawayQty() : 0).sum();
        int totalReturn = orders.stream().mapToInt(o -> o.getTotalReturnQty() != null ? o.getTotalReturnQty() : 0).sum();

        Map<String, Object> result = new HashMap<>();
        result.put("deliveryBatchNo", deliveryBatchNo);
        result.put("supplierName", supplierName);
        result.put("orderCount", orders.size());

        // 汇总进度
        Map<String, Object> progress = new HashMap<>();
        progress.put("receive", buildProgressItem(totalReceived, totalExpected));
        progress.put("inspect", buildProgressItem(totalQualified + totalRejected, totalReceived));
        progress.put("putaway", buildProgressItem(totalPutaway, totalQualified));
        progress.put("return", buildProgressItem(totalReturn, totalExpected));
        result.put("progress", progress);

        // 入库单列表
        List<Map<String, Object>> orderList = orders.stream()
            .map(this::orderToSimpleMap)
            .collect(Collectors.toList());
        result.put("orders", orderList);

        return result;
    }

    /**
     * 构建进度项
     */
    private Map<String, Object> buildProgressItem(Integer actual, Integer target) {
        int actualVal = actual != null ? actual : 0;
        int targetVal = target != null ? target : 0;
        int percent = targetVal > 0 ? (actualVal * 100 / targetVal) : 0;

        Map<String, Object> item = new HashMap<>();
        item.put("actual", actualVal);
        item.put("target", targetVal);
        item.put("percent", percent);
        item.put("complete", actualVal >= targetVal && targetVal > 0);
        return item;
    }

    /**
     * 获取商品明细进度
     */
    private List<Map<String, Object>> getItemProgress(Long orderId) {
        LambdaQueryWrapper<InboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrderItem::getOrderId, orderId);
        List<InboundOrderItem> items = itemRepository.selectList(wrapper);

        return items.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("skuCode", item.getSkuCode());
            map.put("productName", item.getProductName());
            map.put("expectedQty", item.getExpectedQty());
            map.put("receivedQty", item.getReceivedQty() != null ? item.getReceivedQty() : 0);
            map.put("qualifiedQty", item.getQualifiedQty() != null ? item.getQualifiedQty() : 0);
            map.put("rejectedQty", item.getRejectedQty() != null ? item.getRejectedQty() : 0);
            map.put("putawayQty", item.getPutawayQty() != null ? item.getPutawayQty() : 0);
            map.put("returnQty", item.getReturnQty() != null ? item.getReturnQty() : 0);
            map.put("status", item.getStatus());
            map.put("statusName", getItemStatusName(item.getStatus()));
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 获取不合格记录
     */
    private List<Map<String, Object>> getRejectRecords(Long orderId) {
        LambdaQueryWrapper<RejectRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RejectRecord::getInboundOrderId, orderId);
        List<RejectRecord> records = rejectRecordRepository.selectList(wrapper);

        return records.stream().map(record -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", record.getId());
            map.put("skuCode", record.getSkuCode());
            map.put("productName", record.getProductName());
            map.put("rejectQty", record.getRejectQty());
            map.put("rejectType", record.getRejectType());
            map.put("rejectReason", record.getRejectReason());
            map.put("handleStatus", record.getHandleStatus());
            map.put("handleStatusName", record.getHandleStatus() == 0 ? "待处理" : "已处理");
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 入库单转简单Map
     */
    private Map<String, Object> orderToSimpleMap(InboundOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("status", order.getStatus());
        map.put("statusName", getStatusName(order.getStatus()));
        map.put("totalExpectedQty", order.getTotalExpectedQty());
        map.put("totalReceivedQty", order.getTotalReceivedQty());
        map.put("totalQualifiedQty", order.getTotalQualifiedQty());
        map.put("totalPutawayQty", order.getTotalPutawayQty());
        map.put("createTime", order.getCreateTime());
        return map;
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

    /**
     * 获取入库生命周期时间轴
     * TC-2.14
     */
    public List<Map<String, Object>> getLifecycleTimeline(Long orderId) {
        InboundOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        LambdaQueryWrapper<InboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrderItem::getOrderId, orderId);
        List<InboundOrderItem> items = itemRepository.selectList(wrapper);

        java.util.ArrayList<Map<String, Object>> timeline = new java.util.ArrayList<>();

        // 创建
        timeline.add(createTimelineItem("创建入库单", order.getCreateTime(), null, null));

        // 收货
        java.time.LocalDateTime firstReceive = items.stream()
            .filter(i -> i.getReceiveTime() != null)
            .map(InboundOrderItem::getReceiveTime)
            .min(java.util.Comparator.naturalOrder())
            .orElse(null);
        if (firstReceive != null) {
            timeline.add(createTimelineItem("开始收货", firstReceive, null, null));
        }

        // 验收
        java.time.LocalDateTime firstInspect = items.stream()
            .filter(i -> i.getInspectTime() != null)
            .map(InboundOrderItem::getInspectTime)
            .min(java.util.Comparator.naturalOrder())
            .orElse(null);
        if (firstInspect != null) {
            timeline.add(createTimelineItem("开始验收", firstInspect, null, null));
        }

        // 上架
        java.time.LocalDateTime firstPutaway = items.stream()
            .filter(i -> i.getPutawayTime() != null)
            .map(InboundOrderItem::getPutawayTime)
            .min(java.util.Comparator.naturalOrder())
            .orElse(null);
        if (firstPutaway != null) {
            timeline.add(createTimelineItem("开始上架", firstPutaway, null, null));
        }

        // 完成
        if (order.getCompleteTime() != null) {
            timeline.add(createTimelineItem("入库完成", order.getCompleteTime(), null, null));
        }

        return timeline;
    }

    /**
     * 创建时间轴项
     */
    private Map<String, Object> createTimelineItem(String action, java.time.LocalDateTime time,
                                                   Long userId, String userName) {
        Map<String, Object> item = new HashMap<>();
        item.put("action", action);
        item.put("time", time);
        item.put("userId", userId);
        item.put("userName", userName);
        return item;
    }
}
