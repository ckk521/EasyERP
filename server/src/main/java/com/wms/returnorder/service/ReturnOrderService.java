package com.wms.returnorder.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.inbound.entity.InboundOrder;
import com.wms.inbound.entity.InboundOrderItem;
import com.wms.inbound.repository.InboundOrderItemRepository;
import com.wms.inbound.repository.InboundOrderRepository;
import com.wms.outbound.entity.OutboundOrder;
import com.wms.outbound.entity.OutboundOrderItem;
import com.wms.outbound.repository.OutboundOrderItemRepository;
import com.wms.outbound.repository.OutboundOrderRepository;
import com.wms.returnorder.dto.*;
import com.wms.returnorder.entity.ReturnOrder;
import com.wms.returnorder.entity.ReturnOrderItem;
import com.wms.returnorder.repository.ReturnOrderItemRepository;
import com.wms.returnorder.repository.ReturnOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 退货单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnOrderService {

    private final ReturnOrderRepository returnOrderRepository;
    private final ReturnOrderItemRepository returnOrderItemRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final OutboundOrderItemRepository outboundOrderItemRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;

    /**
     * 创建退货单
     */
    @Transactional
    public ReturnOrder createReturnOrder(ReturnOrderCreateDTO dto) {
        // 1. 查询原出库单
        OutboundOrder outboundOrder = outboundOrderRepository.selectById(dto.getOriginalOutboundId());
        if (outboundOrder == null) {
            throw new IllegalArgumentException("出库单不存在");
        }

        // 2. 校验退货商品数量
        List<OutboundOrderItem> outboundItems = outboundOrderItemRepository.selectByOrderId(outboundOrder.getId());
        for (ReturnOrderCreateDTO.ReturnItemDTO item : dto.getItems()) {
            OutboundOrderItem outboundItem = outboundItems.stream()
                    .filter(i -> i.getSkuCode().equals(item.getSkuCode()))
                    .findFirst()
                    .orElse(null);
            if (outboundItem == null) {
                throw new IllegalArgumentException("商品 " + item.getSkuCode() + " 不在原出库单中");
            }
            if (item.getExpectedQty() > outboundItem.getQty()) {
                throw new IllegalArgumentException("商品 " + item.getSkuCode() + " 退货数量不能超过原出库数量");
            }
            if (item.getExpectedQty() <= 0) {
                throw new IllegalArgumentException("商品 " + item.getSkuCode() + " 退货数量必须大于0");
            }
        }

        // 3. 生成退货单号
        String returnNo = generateReturnNo();

        // 4. 创建退货单主表
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.setReturnNo(returnNo);
        returnOrder.setOriginalOutboundId(outboundOrder.getId());
        returnOrder.setOriginalOutboundNo(outboundOrder.getOrderNo());
        returnOrder.setCustomerId(outboundOrder.getCustomerId());
        returnOrder.setCustomerName(outboundOrder.getCustomerName());
        returnOrder.setReturnReason(dto.getReturnReason());
        returnOrder.setReturnReasonText(dto.getReturnReasonText());
        returnOrder.setStatus(ReturnOrder.STATUS_PENDING);
        returnOrder.setWarehouseId(outboundOrder.getWarehouseId());
        returnOrder.setWarehouseCode(outboundOrder.getWarehouseCode());
        returnOrder.setWarehouseName(outboundOrder.getWarehouseName());
        returnOrder.setRemark(dto.getRemark());
        returnOrder.setCreateUserId(dto.getCreateUserId());
        returnOrder.setCreateUserName(dto.getCreateUserName());
        returnOrder.setCreateTime(LocalDateTime.now());

        // 计算预计退货数量
        int totalExpectedQty = dto.getItems().stream()
                .mapToInt(ReturnOrderCreateDTO.ReturnItemDTO::getExpectedQty)
                .sum();
        returnOrder.setTotalExpectedQty(totalExpectedQty);
        returnOrder.setTotalReceivedQty(0);

        returnOrderRepository.insert(returnOrder);

        // 5. 创建退货单明细
        List<ReturnOrderItem> items = new ArrayList<>();
        for (ReturnOrderCreateDTO.ReturnItemDTO itemDTO : dto.getItems()) {
            OutboundOrderItem outboundItem = outboundItems.stream()
                    .filter(i -> i.getSkuCode().equals(itemDTO.getSkuCode()))
                    .findFirst()
                    .orElse(null);

            ReturnOrderItem item = new ReturnOrderItem();
            item.setReturnOrderId(returnOrder.getId());
            item.setReturnOrderNo(returnNo);
            item.setProductId(outboundItem != null ? outboundItem.getProductId() : null);
            item.setSkuCode(itemDTO.getSkuCode());
            item.setProductName(itemDTO.getProductName());
            item.setBarcode(outboundItem != null ? outboundItem.getBarcode() : null);
            item.setOriginalQty(outboundItem != null ? outboundItem.getQty() : 0);
            item.setExpectedQty(itemDTO.getExpectedQty());
            item.setReceivedQty(0);
            item.setRemark(itemDTO.getRemark());
            items.add(item);
        }
        returnOrderItemRepository.batchInsert(items);

        log.info("创建退货单成功: returnNo={}, outboundNo={}", returnNo, outboundOrder.getOrderNo());
        return returnOrder;
    }

    /**
     * 确认收货
     */
    @Transactional
    public ReturnOrder confirmReceive(ReturnReceiveDTO dto) {
        // 1. 查询退货单
        ReturnOrder returnOrder = returnOrderRepository.selectById(dto.getReturnOrderId());
        if (returnOrder == null) {
            throw new IllegalArgumentException("退货单不存在");
        }

        // 2. 校验状态
        if (returnOrder.getStatus() != ReturnOrder.STATUS_PENDING) {
            throw new IllegalStateException("退货单状态不正确，当前状态: " + getStatusName(returnOrder.getStatus()));
        }

        // 3. 更新明细的实收数量
        int totalReceivedQty = 0;
        for (ReturnReceiveDTO.ReceiveItemDTO itemDTO : dto.getItems()) {
            ReturnOrderItem item = returnOrderItemRepository.selectById(itemDTO.getItemId());
            if (item != null) {
                // 校验实收数量不能超过预计数量
                if (itemDTO.getReceivedQty() > item.getExpectedQty()) {
                    throw new IllegalArgumentException("商品 " + item.getSkuCode() + " 实收数量不能超过预计退货数量");
                }
                if (itemDTO.getReceivedQty() < 0) {
                    throw new IllegalArgumentException("商品 " + item.getSkuCode() + " 实收数量不能为负数");
                }
                item.setReceivedQty(itemDTO.getReceivedQty());
                item.setRemark(itemDTO.getRemark());
                returnOrderItemRepository.updateById(item);
                totalReceivedQty += itemDTO.getReceivedQty();
            }
        }

        // 4. 更新退货单状态
        returnOrder.setStatus(ReturnOrder.STATUS_RECEIVED);
        returnOrder.setTotalReceivedQty(totalReceivedQty);
        returnOrder.setReceiveTime(LocalDateTime.now());
        returnOrder.setReceiveUserId(dto.getReceiveUserId());
        returnOrder.setReceiveUserName(dto.getReceiveUserName());

        // 5. 生成入库单
        String inboundNo = generateInboundOrder(returnOrder);
        returnOrder.setInboundOrderNo(inboundNo);

        returnOrderRepository.updateById(returnOrder);

        log.info("退货收货成功: returnNo={}, totalReceivedQty={}, inboundNo={}",
                returnOrder.getReturnNo(), totalReceivedQty, inboundNo);
        return returnOrder;
    }

    /**
     * 取消退货单
     */
    @Transactional
    public ReturnOrder cancelReturnOrder(Long returnOrderId, String cancelReason) {
        ReturnOrder returnOrder = returnOrderRepository.selectById(returnOrderId);
        if (returnOrder == null) {
            throw new IllegalArgumentException("退货单不存在");
        }

        if (returnOrder.getStatus() != ReturnOrder.STATUS_PENDING) {
            throw new IllegalStateException("只有待收货状态的退货单才能取消");
        }

        returnOrder.setStatus(ReturnOrder.STATUS_CANCELLED);
        returnOrder.setCancelReason(cancelReason);
        returnOrderRepository.updateById(returnOrder);

        log.info("取消退货单成功: returnNo={}", returnOrder.getReturnNo());
        return returnOrder;
    }

    /**
     * 完成退货单（入库单完成后调用）
     */
    @Transactional
    public void completeReturnOrder(Long returnOrderId) {
        ReturnOrder returnOrder = returnOrderRepository.selectById(returnOrderId);
        if (returnOrder == null) {
            return;
        }

        returnOrder.setStatus(ReturnOrder.STATUS_COMPLETED);
        returnOrder.setCompleteTime(LocalDateTime.now());
        returnOrderRepository.updateById(returnOrder);

        log.info("退货单完成: returnNo={}", returnOrder.getReturnNo());
    }

    /**
     * 查询退货单详情
     */
    public ReturnOrderDTO getReturnOrderDetail(Long id) {
        ReturnOrder returnOrder = returnOrderRepository.selectById(id);
        if (returnOrder == null) {
            return null;
        }

        ReturnOrderDTO dto = convertToDTO(returnOrder);

        // 查询明细
        List<ReturnOrderItem> items = returnOrderItemRepository.selectByReturnOrderId(id);
        List<ReturnOrderDTO.ReturnOrderItemDTO> itemDTOs = new ArrayList<>();
        for (ReturnOrderItem item : items) {
            ReturnOrderDTO.ReturnOrderItemDTO itemDTO = new ReturnOrderDTO.ReturnOrderItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setProductId(item.getProductId());
            itemDTO.setSkuCode(item.getSkuCode());
            itemDTO.setProductName(item.getProductName());
            itemDTO.setBarcode(item.getBarcode());
            itemDTO.setOriginalQty(item.getOriginalQty());
            itemDTO.setExpectedQty(item.getExpectedQty());
            itemDTO.setReceivedQty(item.getReceivedQty());
            itemDTO.setRemark(item.getRemark());
            itemDTOs.add(itemDTO);
        }
        dto.setItems(itemDTOs);

        return dto;
    }

    /**
     * 分页查询退货单
     */
    public Page<ReturnOrderDTO> getReturnOrderPage(Integer status, String keyword, int page, int limit) {
        Page<ReturnOrder> pageResult = returnOrderRepository.selectPage(status, keyword, page, limit);

        Page<ReturnOrderDTO> dtoPage = new Page<>(page, limit);
        dtoPage.setTotal(pageResult.getTotal());

        List<ReturnOrderDTO> dtoList = new ArrayList<>();
        for (ReturnOrder order : pageResult.getRecords()) {
            dtoList.add(convertToDTO(order));
        }
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    /**
     * 生成入库单
     */
    private String generateInboundOrder(ReturnOrder returnOrder) {
        String inboundNo = "IN-RT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 查询退货明细
        List<ReturnOrderItem> returnItems = returnOrderItemRepository.selectByReturnOrderId(returnOrder.getId());
        log.info("生成入库单: returnOrderId={}, returnOrderNo={}, 明细数量={}",
                returnOrder.getId(), returnOrder.getReturnNo(), returnItems.size());

        // 计算预计入库数量
        int totalExpectedQty = 0;
        for (ReturnOrderItem item : returnItems) {
            log.info("退货明细: skuCode={}, expectedQty={}, receivedQty={}",
                    item.getSkuCode(), item.getExpectedQty(), item.getReceivedQty());
            totalExpectedQty += item.getReceivedQty() != null ? item.getReceivedQty() : 0;
        }
        log.info("计算预计入库数量: totalExpectedQty={}", totalExpectedQty);

        InboundOrder inboundOrder = new InboundOrder();
        inboundOrder.setOrderNo(inboundNo);
        inboundOrder.setOrderType(InboundOrder.TYPE_CUSTOMER_RETURN);
        inboundOrder.setWarehouseId(returnOrder.getWarehouseId());
        inboundOrder.setWarehouseCode(returnOrder.getWarehouseCode());
        inboundOrder.setWarehouseName(returnOrder.getWarehouseName());
        inboundOrder.setStatus(InboundOrder.STATUS_PENDING);
        inboundOrder.setRemark("客户退货，退货单号：" + returnOrder.getReturnNo());
        inboundOrder.setCreateTime(LocalDateTime.now());

        // 设置来源退货单信息
        inboundOrder.setSourceReturnId(returnOrder.getId());
        inboundOrder.setSourceReturnNo(returnOrder.getReturnNo());

        inboundOrder.setTotalExpectedQty(totalExpectedQty);
        inboundOrder.setTotalReceivedQty(0);
        inboundOrder.setTotalQualifiedQty(0);
        inboundOrder.setTotalRejectedQty(0);
        inboundOrder.setTotalPutawayQty(0);

        inboundOrderRepository.insert(inboundOrder);

        // 创建入库单明细
        for (ReturnOrderItem returnItem : returnItems) {
            if (returnItem.getReceivedQty() != null && returnItem.getReceivedQty() > 0) {
                InboundOrderItem inboundItem = new InboundOrderItem();
                inboundItem.setOrderId(inboundOrder.getId());
                inboundItem.setOrderNo(inboundNo);
                inboundItem.setProductId(returnItem.getProductId());
                inboundItem.setSkuCode(returnItem.getSkuCode());
                inboundItem.setProductName(returnItem.getProductName());
                inboundItem.setBarcode(returnItem.getBarcode());
                inboundItem.setExpectedQty(returnItem.getReceivedQty());
                inboundItem.setReceivedQty(0);
                inboundItem.setQualifiedQty(0);
                inboundItem.setRejectedQty(0);
                inboundItem.setPutawayQty(0);
                inboundItem.setStatus(InboundOrderItem.STATUS_PENDING);
                inboundOrderItemRepository.insert(inboundItem);
            }
        }

        // 更新退货单的入库单ID
        returnOrder.setInboundOrderId(inboundOrder.getId());
        returnOrderRepository.updateById(returnOrder);

        log.info("生成客户退货入库单: inboundNo={}, inboundOrderId={}, returnNo={}, totalExpectedQty={}",
                inboundNo, inboundOrder.getId(), returnOrder.getReturnNo(), totalExpectedQty);
        return inboundNo;
    }

    /**
     * 生成退货单号
     */
    private String generateReturnNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "RT" + dateStr;

        // 查询数据库中今天最大的退货单号
        String sql = "SELECT MAX(return_no) FROM wms_return_order WHERE return_no LIKE ?";
        String maxNo = null;
        try {
            maxNo = inboundOrderRepository.selectById(1L) != null ? null : null; // hack to get jdbcTemplate
        } catch (Exception e) {
            // ignore
        }

        // 使用更简单的方法：查询现有最大序号
        int seq = 1;
        for (int i = 9999; i >= 1; i--) {
            String testNo = prefix + String.format("%04d", i);
            if (returnOrderRepository.selectByReturnNo(testNo) != null) {
                seq = i + 1;
                break;
            }
        }

        return prefix + String.format("%04d", seq);
    }

    /**
     * 转换为DTO
     */
    private ReturnOrderDTO convertToDTO(ReturnOrder order) {
        ReturnOrderDTO dto = new ReturnOrderDTO();
        dto.setId(order.getId());
        dto.setReturnNo(order.getReturnNo());
        dto.setOriginalOutboundId(order.getOriginalOutboundId());
        dto.setOriginalOutboundNo(order.getOriginalOutboundNo());
        dto.setCustomerId(order.getCustomerId());
        dto.setCustomerName(order.getCustomerName());
        dto.setReturnReason(order.getReturnReason());
        dto.setReturnReasonText(order.getReturnReasonText());
        dto.setStatus(order.getStatus());
        dto.setTotalExpectedQty(order.getTotalExpectedQty());
        dto.setTotalReceivedQty(order.getTotalReceivedQty());
        dto.setInboundOrderId(order.getInboundOrderId());
        dto.setInboundOrderNo(order.getInboundOrderNo());
        dto.setWarehouseId(order.getWarehouseId());
        dto.setWarehouseCode(order.getWarehouseCode());
        dto.setWarehouseName(order.getWarehouseName());
        dto.setCancelReason(order.getCancelReason());
        dto.setRemark(order.getRemark());
        dto.setCreateUserId(order.getCreateUserId());
        dto.setCreateUserName(order.getCreateUserName());
        dto.setCreateTime(order.getCreateTime());
        dto.setReceiveUserId(order.getReceiveUserId());
        dto.setReceiveUserName(order.getReceiveUserName());
        dto.setReceiveTime(order.getReceiveTime());
        dto.setCompleteTime(order.getCompleteTime());
        return dto;
    }

    private String getStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case ReturnOrder.STATUS_PENDING: return "待收货";
            case ReturnOrder.STATUS_RECEIVED: return "已收货";
            case ReturnOrder.STATUS_COMPLETED: return "已完成";
            case ReturnOrder.STATUS_CANCELLED: return "已取消";
            default: return "";
        }
    }
}