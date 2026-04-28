package com.wms.inbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.inbound.dto.RejectHandleDTO;
import com.wms.inbound.entity.InboundOrder;
import com.wms.inbound.entity.InboundOrderItem;
import com.wms.inbound.entity.RejectRecord;
import com.wms.inbound.repository.InboundOrderItemRepository;
import com.wms.inbound.repository.InboundOrderRepository;
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
import java.util.stream.Collectors;

/**
 * 退货服务
 * 阶段六: 退货处理 + 数据关联查询
 */
@Service
@RequiredArgsConstructor
public class ReturnService {

    private final RejectRecordRepository rejectRecordRepository;
    private final InboundOrderRepository orderRepository;
    private final InboundOrderItemRepository itemRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 查询不合格品列表
     */
    public Map<String, Object> listRejectRecords(Long orderId, Integer handleStatus, int page, int limit) {
        LambdaQueryWrapper<RejectRecord> wrapper = new LambdaQueryWrapper<>();

        if (orderId != null) {
            wrapper.eq(RejectRecord::getInboundOrderId, orderId);
        }
        if (handleStatus != null) {
            wrapper.eq(RejectRecord::getHandleStatus, handleStatus);
        }
        wrapper.orderByDesc(RejectRecord::getCreateTime);

        // 分页查询
        com.baomidou.mybatisplus.core.metadata.IPage<RejectRecord> pageResult =
            rejectRecordRepository.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, limit), wrapper);

        List<Map<String, Object>> list = pageResult.getRecords().stream()
            .map(this::rejectRecordToMap)
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", pageResult.getTotal());
        return result;
    }

    /**
     * 获取不合格品详情(含关联的入库单信息)
     */
    public Map<String, Object> getRejectDetail(Long rejectId) {
        RejectRecord record = rejectRecordRepository.selectById(rejectId);
        if (record == null) {
            throw new RuntimeException("不合格品记录不存在");
        }

        Map<String, Object> result = rejectRecordToMap(record);

        // 查询关联的入库单信息
        InboundOrder order = orderRepository.selectById(record.getInboundOrderId());
        if (order != null) {
            Map<String, Object> orderInfo = new HashMap<>();
            orderInfo.put("id", order.getId());
            orderInfo.put("orderNo", order.getOrderNo());
            orderInfo.put("deliveryBatchNo", order.getDeliveryBatchNo());
            orderInfo.put("supplierName", order.getSupplierName());
            orderInfo.put("warehouseName", order.getWarehouseName());
            result.put("inboundOrder", orderInfo);
        }

        // 查询关联的入库明细信息
        InboundOrderItem item = itemRepository.selectById(record.getInboundItemId());
        if (item != null) {
            Map<String, Object> itemInfo = new HashMap<>();
            itemInfo.put("id", item.getId());
            itemInfo.put("skuCode", item.getSkuCode());
            itemInfo.put("productName", item.getProductName());
            itemInfo.put("expectedQty", item.getExpectedQty());
            itemInfo.put("receivedQty", item.getReceivedQty());
            itemInfo.put("rejectedQty", item.getRejectedQty());
            result.put("inboundItem", itemInfo);
        }

        return result;
    }

    /**
     * 处理不合格品
     * TC-2.9
     */
    @Transactional
    public void handleReject(RejectHandleDTO dto, Long userId) {
        // 参数校验
        if (dto.getRejectId() == null) {
            throw new IllegalArgumentException("不合格记录ID不能为空");
        }
        if (dto.getHandleType() == null) {
            throw new IllegalArgumentException("处理方式不能为空");
        }
        if (dto.getHandleQty() == null || dto.getHandleQty() <= 0) {
            throw new IllegalArgumentException("处理数量必须大于0");
        }

        // 查询不合格记录
        RejectRecord record = rejectRecordRepository.selectById(dto.getRejectId());
        if (record == null) {
            throw new RuntimeException("不合格品记录不存在");
        }

        if (record.getHandleStatus() == RejectRecord.HANDLE_STATUS_DONE) {
            throw new IllegalStateException("该不合格品已处理");
        }

        // 检查处理数量
        if (dto.getHandleQty() > record.getRejectQty()) {
            throw new IllegalArgumentException("处理数量不能超过不合格数量");
        }

        // 更新处理信息
        record.setHandleType(dto.getHandleType());
        record.setHandleQty(dto.getHandleQty());
        record.setHandleRemark(dto.getHandleRemark());
        record.setHandleTime(LocalDateTime.now());
        record.setHandleUser(userId);
        record.setHandleStatus(RejectRecord.HANDLE_STATUS_DONE);

        // 如果是退货供应商，生成退货单号
        if (dto.getHandleType() == RejectRecord.HANDLE_TYPE_RETURN) {
            String returnOrderNo = generateReturnOrderNo();
            record.setReturnOrderNo(returnOrderNo);
            record.setReturnTime(LocalDateTime.now());
        }

        rejectRecordRepository.updateById(record);

        // 更新入库明细的退货数量
        updateItemReturnQty(record.getInboundItemId(), dto.getHandleQty());
    }

    /**
     * 生成退货单号
     * 格式: RT + 年月日 + 4位序号
     */
    private String generateReturnOrderNo() {
        String datePrefix = "RT" + LocalDate.now().format(DATE_FORMATTER);
        Integer maxSeq = rejectRecordRepository.getMaxReturnSeqByDate(datePrefix);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;
        return datePrefix + String.format("%04d", nextSeq);
    }

    /**
     * 更新入库明细退货数量
     */
    private void updateItemReturnQty(Long itemId, Integer returnQty) {
        InboundOrderItem item = itemRepository.selectById(itemId);
        if (item != null) {
            int currentReturn = item.getReturnQty() != null ? item.getReturnQty() : 0;
            item.setReturnQty(currentReturn + returnQty);

            // 更新状态
            if (item.getReturnQty() >= item.getRejectedQty()) {
                item.setStatus(InboundOrderItem.STATUS_FULL_RETURN);
            } else {
                item.setStatus(InboundOrderItem.STATUS_PARTIAL_RETURN);
            }

            itemRepository.updateById(item);
        }
    }

    /**
     * 按入库单号查询退货记录
     */
    public List<Map<String, Object>> listByInboundOrderNo(String orderNo) {
        LambdaQueryWrapper<RejectRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RejectRecord::getInboundOrderNo, orderNo);
        wrapper.orderByDesc(RejectRecord::getCreateTime);

        List<RejectRecord> records = rejectRecordRepository.selectList(wrapper);
        return records.stream()
            .map(this::rejectRecordToMap)
            .collect(Collectors.toList());
    }

    /**
     * 不合格记录转Map
     */
    private Map<String, Object> rejectRecordToMap(RejectRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getId());
        map.put("inboundOrderId", record.getInboundOrderId());
        map.put("inboundOrderNo", record.getInboundOrderNo());
        map.put("inboundItemId", record.getInboundItemId());
        map.put("deliveryBatchNo", record.getDeliveryBatchNo());
        map.put("skuCode", record.getSkuCode());
        map.put("productName", record.getProductName());
        map.put("supplierName", record.getSupplierName());
        map.put("rejectQty", record.getRejectQty());
        map.put("rejectType", record.getRejectType());
        map.put("rejectTypeName", getRejectTypeName(record.getRejectType()));
        map.put("rejectReason", record.getRejectReason());
        map.put("rejectImages", record.getRejectImages());
        map.put("discoverStage", record.getDiscoverStage());
        map.put("handleStatus", record.getHandleStatus());
        map.put("handleStatusName", record.getHandleStatus() == 0 ? "待处理" : "已处理");
        map.put("handleType", record.getHandleType());
        map.put("handleTypeName", getHandleTypeName(record.getHandleType()));
        map.put("handleQty", record.getHandleQty());
        map.put("handleRemark", record.getHandleRemark());
        map.put("handleTime", record.getHandleTime());
        map.put("returnOrderNo", record.getReturnOrderNo());
        map.put("returnTime", record.getReturnTime());
        map.put("createTime", record.getCreateTime());
        return map;
    }

    /**
     * 获取不合格类型名称
     */
    private String getRejectTypeName(Integer rejectType) {
        if (rejectType == null) return "";
        switch (rejectType) {
            case 1: return "包装破损";
            case 2: return "商品损坏";
            case 3: return "错货";
            case 4: return "规格不符";
            case 5: return "效期问题";
            case 6: return "其他";
            default: return "";
        }
    }

    /**
     * 获取处理方式名称
     */
    private String getHandleTypeName(Integer handleType) {
        if (handleType == null) return "";
        switch (handleType) {
            case 1: return "退货供应商";
            case 2: return "降价销售";
            case 3: return "报损";
            case 4: return "内部使用";
            case 5: return "销毁";
            default: return "";
        }
    }
}
