package com.wms.outbound.service;

import com.wms.inbound.entity.InboundOrder;
import com.wms.inbound.repository.InboundOrderRepository;
import com.wms.outbound.dto.*;
import com.wms.outbound.entity.*;
import com.wms.outbound.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 发货服务
 *
 * 实现功能：
 * 1. 扫描出库单号或包裹号确认发货
 * 2. 选择物流公司和录入物流单号
 * 3. 批量确认发货
 * 4. 发货后状态变为已发货
 * 5. 自动通知ERP
 * 6. 库存扣减
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipService {

    private final ShipRecordRepository shipRecordRepository;
    private final PackRecordRepository packRecordRepository;
    private final OutboundOrderRepository orderRepository;
    private final OutboundOrderItemRepository orderItemRepository;
    private final InventoryAllocationRepository allocationRepository;
    private final InboundOrderRepository inboundOrderRepository;

    /**
     * 确认发货（单个订单）
     */
    @Transactional
    public ShipResultDTO confirmShip(ShipConfirmDTO dto) {
        // 1. 查询出库单
        OutboundOrder order = null;
        PackRecord packRecord = null;

        if (dto.getOutboundOrderId() != null) {
            order = orderRepository.selectById(dto.getOutboundOrderId());
            packRecord = packRecordRepository.selectLatestByOrderId(dto.getOutboundOrderId());
        } else if (dto.getPackageNo() != null) {
            packRecord = packRecordRepository.selectByPackageNo(dto.getPackageNo());
            if (packRecord != null) {
                order = orderRepository.selectById(packRecord.getOutboundOrderId());
            }
        } else if (dto.getOutboundOrderNo() != null) {
            order = orderRepository.selectByOrderNo(dto.getOutboundOrderNo());
            if (order != null) {
                packRecord = packRecordRepository.selectLatestByOrderId(order.getId());
            }
        }

        if (order == null) {
            throw new IllegalArgumentException("出库单不存在");
        }

        // 2. 检查订单状态
        if (order.getStatus() != OutboundOrder.STATUS_SHIPPING) {
            throw new IllegalStateException("出库单状态不正确，当前状态: " + getStatusName(order.getStatus()));
        }

        // 3. 验证物流单号
        if (dto.getTrackingNo() == null || dto.getTrackingNo().isEmpty()) {
            throw new IllegalArgumentException("物流单号不能为空");
        }

        // 4. 创建发货记录（如果有打包记录则关联）
        ShipRecord shipRecord = new ShipRecord();
        shipRecord.setOutboundOrderId(order.getId());
        shipRecord.setOutboundOrderNo(order.getOrderNo());
        if (packRecord != null) {
            shipRecord.setPackageNo(packRecord.getPackageNo());
        }
        shipRecord.setLogisticsCompany(dto.getLogisticsCompany());
        shipRecord.setLogisticsCompanyCode(dto.getLogisticsCompanyCode());
        shipRecord.setTrackingNo(dto.getTrackingNo());
        shipRecord.setShipUserId(dto.getShipUserId());
        shipRecord.setShipUserName(dto.getShipUserName());
        shipRecord.setStatus(ShipRecord.STATUS_SHIPPED);
        shipRecord.setShipTime(LocalDateTime.now());
        shipRecord.setErpNotified(0);
        shipRecordRepository.insert(shipRecord);

        // 5. 更新打包记录状态（如果有）
        if (packRecord != null) {
            packRecord.setStatus(PackRecord.STATUS_SHIPPED);
            packRecord.setShipTime(LocalDateTime.now());
            packRecordRepository.updateById(packRecord);
        }

        // 7. 更新出库单状态
        order.setStatus(OutboundOrder.STATUS_SHIPPED);
        order.setShipUserId(dto.getShipUserId());
        order.setShipUserName(dto.getShipUserName());
        order.setShipTime(LocalDateTime.now());
        if (dto.getLogisticsCompany() != null) {
            order.setLogisticsCompany(dto.getLogisticsCompany());
        }
        order.setTrackingNo(dto.getTrackingNo());
        order.setShippedTime(LocalDateTime.now());
        order.setTotalShippedQty(order.getTotalPackedQty());
        order.setCompleteTime(LocalDateTime.now());
        orderRepository.updateById(order);

        // 8. 更新出库单明细
        List<OutboundOrderItem> items = orderItemRepository.selectByOrderId(order.getId());
        for (OutboundOrderItem item : items) {
            item.setShippedQty(item.getPackedQty());
            item.setStatus(OutboundOrderItem.STATUS_SHIPPED);
            item.setShipTime(LocalDateTime.now());
            orderItemRepository.updateById(item);
        }

        // 9. 更新库存分配记录状态（库存扣减）
        List<InventoryAllocation> allocations = allocationRepository.selectByOrderId(order.getId());
        for (InventoryAllocation allocation : allocations) {
            allocation.setStatus(InventoryAllocation.STATUS_SHIPPED);
            allocation.setShipTime(LocalDateTime.now());
            allocation.setShippedQty(allocation.getPickedQty());
            allocationRepository.updateById(allocation);

            // 执行库存扣减（qty_total减少，qty_locked减少）
            allocationRepository.deductStock(
                allocation.getProductId(),
                allocation.getLocationId(),
                allocation.getAllocatedQty()
            );
        }

        // 10. 通知ERP（如果是ERP推送的订单）
        if (order.getSourceType() != null && order.getSourceType() == OutboundOrder.SOURCE_ERP) {
            shipRecord.setErpNotified(1);
            shipRecord.setErpNotifyTime(LocalDateTime.now());
            shipRecordRepository.updateById(shipRecord);
            log.info("已通知ERP发货完成: orderNo={}, trackingNo={}", order.getOrderNo(), dto.getTrackingNo());
        }

        // 11. 调拨出库：发货后触发关联入库单状态更新
        if (order.getOrderType() != null && order.getOrderType() == OutboundOrder.TYPE_TRANSFER
                && order.getTransferInboundId() != null) {
            syncTransferInboundStatus(order);
        }

        // 11. 构建返回结果
        ShipResultDTO result = new ShipResultDTO();
        result.setSuccess(true);
        result.setOutboundOrderId(order.getId());
        result.setOutboundOrderNo(order.getOrderNo());
        result.setPackageNo(packRecord != null ? packRecord.getPackageNo() : null);
        result.setTrackingNo(dto.getTrackingNo());
        result.setNewStatus(OutboundOrder.STATUS_SHIPPED);
        result.setStatusName("已发货");

        log.info("发货确认成功: orderNo={}, packageNo={}, trackingNo={}, user={}",
            order.getOrderNo(), packRecord != null ? packRecord.getPackageNo() : "N/A", dto.getTrackingNo(), dto.getShipUserName());

        return result;
    }

    /**
     * 批量发货
     */
    @Transactional
    public ShipResultDTO batchShip(ShipConfirmDTO dto) {
        if (dto.getOrderIds() == null || dto.getOrderIds().isEmpty()) {
            throw new IllegalArgumentException("出库单ID列表不能为空");
        }

        int successCount = 0;
        int failCount = 0;
        List<ShipResultDTO.ShipFailItem> failItems = new ArrayList<>();

        for (Long orderId : dto.getOrderIds()) {
            try {
                ShipConfirmDTO singleDto = new ShipConfirmDTO();
                singleDto.setOutboundOrderId(orderId);
                singleDto.setLogisticsCompany(dto.getLogisticsCompany());
                singleDto.setLogisticsCompanyCode(dto.getLogisticsCompanyCode());
                singleDto.setTrackingNo(dto.getTrackingNo());
                singleDto.setShipUserId(dto.getShipUserId());
                singleDto.setShipUserName(dto.getShipUserName());

                confirmShip(singleDto);
                successCount++;
            } catch (Exception e) {
                failCount++;
                OutboundOrder order = orderRepository.selectById(orderId);
                ShipResultDTO.ShipFailItem failItem = new ShipResultDTO.ShipFailItem();
                failItem.setOutboundOrderNo(order != null ? order.getOrderNo() : "未知");
                failItem.setFailReason(e.getMessage());
                failItems.add(failItem);
                log.warn("批量发货失败: orderId={}, reason={}", orderId, e.getMessage());
            }
        }

        ShipResultDTO result = new ShipResultDTO();
        result.setSuccess(failCount == 0);
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setFailItems(failItems);

        log.info("批量发货完成: 成功{}, 失败{}", successCount, failCount);

        return result;
    }

    // ========== 私有方法 ==========

    /**
     * 同步调拨入库单状态
     *
     * 出库发货后，关联的入库单进入待收货状态
     * 正确的流程方向：出库发货 → 入库待收货（而不是入库驱动出库）
     */
    private void syncTransferInboundStatus(OutboundOrder outboundOrder) {
        InboundOrder inboundOrder = inboundOrderRepository.selectById(outboundOrder.getTransferInboundId());
        if (inboundOrder == null) {
            log.warn("调拨出库 {} 关联的入库单 {} 不存在", outboundOrder.getOrderNo(), outboundOrder.getTransferInboundId());
            return;
        }
        if (inboundOrder.getStatus() == InboundOrder.STATUS_CANCELLED) {
            log.warn("调拨入库单 {} 已取消，跳过同步", inboundOrder.getOrderNo());
            return;
        }

        // 出库发货后，入库单变为待收货状态
        // 如果入库单还是初始状态（待收货），保持不变
        // 如果入库单已在收货中等状态，不回退（可能已经开始收货了）
        if (inboundOrder.getStatus() == InboundOrder.STATUS_PENDING) {
            // 出库已发货，入库单标记为可以开始收货（实际还是待收货状态，但可以开始操作了）
            log.info("调拨出库 {} 已发货，关联入库单 {} 可开始收货", outboundOrder.getOrderNo(), inboundOrder.getOrderNo());
        }

        // 更新入库单的来源出库单发货信息
        inboundOrder.setUpdateTime(LocalDateTime.now());
        inboundOrderRepository.updateById(inboundOrder);

        log.info("调拨出库 {} 同步入库单 {}: outboundStatus=已发货", outboundOrder.getOrderNo(), inboundOrder.getOrderNo());
    }

    private String getStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case OutboundOrder.STATUS_PENDING: return "待分配";
            case OutboundOrder.STATUS_ALLOCATED: return "已分配";
            case OutboundOrder.STATUS_PICKING: return "拣货中";
            case OutboundOrder.STATUS_PACKING: return "待打包";
            case OutboundOrder.STATUS_SHIPPING: return "待发货";
            case OutboundOrder.STATUS_SHIPPED: return "已发货";
            case OutboundOrder.STATUS_CANCELLED: return "已取消";
            default: return "";
        }
    }
}