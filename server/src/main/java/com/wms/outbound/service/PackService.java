package com.wms.outbound.service;

import com.wms.outbound.dto.*;
import com.wms.outbound.entity.*;
import com.wms.outbound.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 打包服务
 *
 * 实现功能：
 * 1. 查看待打包任务列表
 * 2. 领取打包任务
 * 3. 系统推荐包装箱型
 * 4. 确认打包
 * 5. 打包完成状态变更
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackService {

    private final PackRecordRepository packRecordRepository;
    private final OutboundOrderRepository orderRepository;
    private final OutboundOrderItemRepository orderItemRepository;
    private final BoxTypeRepository boxTypeRepository;

    /**
     * 查看待打包任务列表
     */
    public List<PackTaskDetailDTO> getPendingPackTasks(Long warehouseId) {
        // 查询状态为"待打包"的出库单
        List<OutboundOrder> orders = orderRepository.selectByStatusAndWarehouse(
            OutboundOrder.STATUS_PACKING, warehouseId);

        return orders.stream()
            .map(this::convertToPackTaskDetail)
            .collect(Collectors.toList());
    }

    /**
     * 领取打包任务
     */
    @Transactional
    public PackTaskDetailDTO claimPackTask(String orderNo, Long userId, String userName) {
        // 1. 查询出库单
        OutboundOrder order = orderRepository.selectByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("出库单不存在: " + orderNo);
        }

        if (order.getStatus() != OutboundOrder.STATUS_PACKING) {
            throw new IllegalStateException("出库单状态不正确，当前状态: " + order.getStatus());
        }

        // 2. 检查是否已有进行中的打包任务
        PackRecord existingRecord = packRecordRepository.selectLatestByOrderId(order.getId());
        if (existingRecord != null && existingRecord.getStatus() == PackRecord.STATUS_PACKING) {
            throw new IllegalStateException("出库单已在打包中，打包人: " + existingRecord.getPackUserName());
        }

        // 3. 创建打包记录
        PackRecord packRecord = new PackRecord();
        packRecord.setOutboundOrderId(order.getId());
        packRecord.setOutboundOrderNo(orderNo);
        packRecord.setPackageNo(generatePackageNo());
        packRecord.setStatus(PackRecord.STATUS_PACKING);
        packRecord.setPackUserId(userId);
        packRecord.setPackUserName(userName);
        packRecord.setClaimTime(LocalDateTime.now());
        packRecordRepository.insert(packRecord);

        // 4. 查询商品明细
        List<OutboundOrderItem> items = orderItemRepository.selectByOrderId(order.getId());

        // 5. 构建返回结果
        PackTaskDetailDTO result = convertToPackTaskDetail(order, items);
        result.setRecommendedBoxes(recommendBoxType(items, BigDecimal.ZERO, BigDecimal.ZERO));

        log.info("领取打包任务成功: orderNo={}, user={}", orderNo, userName);

        return result;
    }

    /**
     * 推荐包装箱型
     */
    public List<PackTaskDetailDTO.RecommendedBoxDTO> recommendBoxType(
            List<OutboundOrderItem> items, BigDecimal totalWeight, BigDecimal totalVolume) {

        List<BoxType> boxTypes = boxTypeRepository.selectAllEnabled();

        // 计算总重量和总体积（如果未提供）
        if (totalWeight == null || totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            // 这里简化处理，实际应该根据商品重量计算
            totalWeight = new BigDecimal(items.size() * 2); // 假设每件商品2kg
        }
        if (totalVolume == null || totalVolume.compareTo(BigDecimal.ZERO) == 0) {
            totalVolume = new BigDecimal(items.size() * 0.01); // 假设每件商品0.01m³
        }

        final BigDecimal weight = totalWeight;
        final BigDecimal volume = totalVolume;

        // 筛选能装下的箱型并排序
        return boxTypes.stream()
            .filter(box -> box.getMaxWeight().compareTo(weight) >= 0)
            .map(box -> {
                PackTaskDetailDTO.RecommendedBoxDTO dto = new PackTaskDetailDTO.RecommendedBoxDTO();
                dto.setCode(box.getCode());
                dto.setName(box.getName());
                dto.setLength(box.getLength());
                dto.setWidth(box.getWidth());
                dto.setHeight(box.getHeight());
                dto.setVolume(box.getVolume());
                dto.setMaxWeight(box.getMaxWeight());

                // 计算推荐指数（越小越合适）
                int level = calculateRecommendLevel(box, weight, volume);
                dto.setRecommendLevel(level);
                dto.setReason(generateReason(box, weight, volume));

                return dto;
            })
            .sorted(Comparator.comparingInt(PackTaskDetailDTO.RecommendedBoxDTO::getRecommendLevel))
            .collect(Collectors.toList());
    }

    /**
     * 确认打包（支持单商品打包）
     */
    @Transactional
    public PackResultDTO confirmPack(PackConfirmDTO dto) {
        // 1. 查询出库单
        OutboundOrder order = orderRepository.selectById(dto.getOutboundOrderId());
        if (order == null) {
            throw new IllegalArgumentException("出库单不存在");
        }

        if (order.getStatus() != OutboundOrder.STATUS_PACKING) {
            throw new IllegalStateException("出库单状态不正确，当前状态: " + order.getStatus());
        }

        // 2. 查询箱型信息
        BoxType boxType = boxTypeRepository.selectByCode(dto.getBoxTypeCode());
        if (boxType == null) {
            // 使用默认箱型
            boxType = new BoxType();
            boxType.setName("标准箱");
            boxType.setCode(dto.getBoxTypeCode());
            boxType.setVolume(new BigDecimal("0.01"));
        }

        // 3. 如果指定了itemId，只打包该商品
        if (dto.getItemId() != null) {
            OutboundOrderItem item = orderItemRepository.selectById(dto.getItemId());
            if (item == null) {
                throw new IllegalArgumentException("商品明细不存在");
            }

            int packQty = dto.getPackQty() != null ? dto.getPackQty() : item.getPickedQty() - (item.getPackedQty() != null ? item.getPackedQty() : 0);
            if (packQty <= 0) {
                throw new IllegalArgumentException("打包数量必须大于0");
            }

            // 更新明细的打包数量
            item.setPackedQty((item.getPackedQty() != null ? item.getPackedQty() : 0) + packQty);
            if (item.getPackedQty() >= item.getPickedQty()) {
                item.setStatus(OutboundOrderItem.STATUS_PACKED);
                item.setPackTime(LocalDateTime.now());
            }
            orderItemRepository.updateById(item);

            // 创建打包记录
            PackRecord packRecord = new PackRecord();
            packRecord.setOutboundOrderId(order.getId());
            packRecord.setOutboundOrderNo(order.getOrderNo());
            packRecord.setPackageNo(generatePackageNo());
            packRecord.setBoxType(boxType.getName());
            packRecord.setBoxTypeCode(dto.getBoxTypeCode());
            packRecord.setWeight(dto.getWeight());
            packRecord.setVolume(boxType.getVolume());
            packRecord.setStatus(PackRecord.STATUS_PACKED);
            packRecord.setPackUserId(dto.getPackUserId());
            packRecord.setPackUserName(dto.getPackUserName());
            packRecord.setPackTime(LocalDateTime.now());
            packRecord.setRemark(dto.getRemark());
            packRecordRepository.insert(packRecord);

            // 更新出库单的打包数量
            int totalPacked = order.getTotalPackedQty() != null ? order.getTotalPackedQty() : 0;
            order.setTotalPackedQty(totalPacked + packQty);

            // 检查是否所有商品都已打包完成
            List<OutboundOrderItem> allItems = orderItemRepository.selectByOrderId(order.getId());
            boolean allPacked = allItems.stream().allMatch(item ->
                item.getPackedQty() != null && item.getPackedQty() >= item.getPickedQty());

            if (allPacked) {
                // 所有商品都已打包完成，自动更新状态为待发货
                order.setStatus(OutboundOrder.STATUS_SHIPPING);
                log.info("所有商品打包完成，订单状态自动更新为待发货: orderNo={}", order.getOrderNo());
            }

            orderRepository.updateById(order);
        } else {
            // 整单打包（原有逻辑）
            // 查询打包记录
            PackRecord packRecord = packRecordRepository.selectLatestByOrderId(order.getId());
            if (packRecord == null || packRecord.getStatus() != PackRecord.STATUS_PACKING) {
                // 创建打包记录
                packRecord = new PackRecord();
                packRecord.setOutboundOrderId(order.getId());
                packRecord.setOutboundOrderNo(order.getOrderNo());
                packRecord.setPackageNo(generatePackageNo());
                packRecord.setStatus(PackRecord.STATUS_PACKING);
                packRecord.setPackUserId(dto.getPackUserId());
                packRecord.setPackUserName(dto.getPackUserName());
                packRecord.setClaimTime(LocalDateTime.now());
                packRecordRepository.insert(packRecord);
            }

            // 更新打包记录
            packRecord.setBoxType(boxType.getName());
            packRecord.setBoxTypeCode(dto.getBoxTypeCode());
            packRecord.setWeight(dto.getWeight());
            packRecord.setVolume(boxType.getVolume());
            packRecord.setStatus(PackRecord.STATUS_PACKED);
            packRecord.setPackTime(LocalDateTime.now());
            packRecord.setRemark(dto.getRemark());
            packRecordRepository.updateById(packRecord);

            // 更新出库单明细的打包数量
            List<OutboundOrderItem> items = orderItemRepository.selectByOrderId(order.getId());
            for (OutboundOrderItem item : items) {
                item.setPackedQty(item.getPickedQty()); // 全部打包
                item.setStatus(OutboundOrderItem.STATUS_PACKED);
                item.setPackTime(LocalDateTime.now());
                orderItemRepository.updateById(item);
            }

            // 更新出库单状态为待发货
            order.setStatus(OutboundOrder.STATUS_SHIPPING);
            order.setTotalPackedQty(order.getTotalPickedQty());
            orderRepository.updateById(order);
        }

        // 4. 构建返回结果
        PackResultDTO result = new PackResultDTO();
        result.setSuccess(true);
        result.setOutboundOrderId(order.getId());
        result.setOutboundOrderNo(order.getOrderNo());
        result.setNewStatus(order.getStatus());
        result.setStatusName(getStatusName(order.getStatus()));

        log.info("打包确认成功: orderNo={}, itemId={}", order.getOrderNo(), dto.getItemId());

        return result;
    }

    /**
     * 完成订单打包（将状态改为待发货）
     */
    @Transactional
    public Map<String, Object> completeOrderPack(Long orderId) {
        OutboundOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("出库单不存在");
        }

        if (order.getStatus() != OutboundOrder.STATUS_PACKING) {
            throw new IllegalStateException("出库单状态不正确，当前状态: " + getStatusName(order.getStatus()));
        }

        // 检查是否所有商品都已打包
        List<OutboundOrderItem> items = orderItemRepository.selectByOrderId(orderId);
        boolean allPacked = items.stream().allMatch(item ->
            item.getPackedQty() != null && item.getPackedQty() >= item.getPickedQty());

        if (!allPacked) {
            throw new IllegalStateException("还有商品未完成打包");
        }

        // 更新出库单状态为待发货
        order.setStatus(OutboundOrder.STATUS_SHIPPING);
        orderRepository.updateById(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("orderNo", order.getOrderNo());
        result.put("newStatus", OutboundOrder.STATUS_SHIPPING);
        result.put("statusName", "待发货");

        log.info("订单打包完成: orderNo={}, status=待发货", order.getOrderNo());

        return result;
    }

    // ========== 私有方法 ==========

    private PackTaskDetailDTO convertToPackTaskDetail(OutboundOrder order) {
        List<OutboundOrderItem> items = orderItemRepository.selectByOrderId(order.getId());
        return convertToPackTaskDetail(order, items);
    }

    private PackTaskDetailDTO convertToPackTaskDetail(OutboundOrder order, List<OutboundOrderItem> items) {
        PackTaskDetailDTO dto = new PackTaskDetailDTO();
        dto.setOutboundOrderId(order.getId());
        dto.setOutboundOrderNo(order.getOrderNo());
        dto.setCustomerName(order.getCustomerName());
        dto.setReceiverName(order.getReceiverName());
        dto.setReceiverPhone(order.getReceiverPhone());
        dto.setReceiverAddress(order.getReceiverAddress());
        dto.setLogisticsCompany(order.getLogisticsCompany());
        dto.setTotalQty(order.getTotalQty());
        dto.setStatus(order.getStatus());
        dto.setStatusName(getStatusName(order.getStatus()));

        // 计算SKU种类数
        dto.setTotalSku(items.size());

        // 转换商品明细
        List<PackTaskDetailDTO.PackItemDTO> itemDTOs = items.stream()
            .map(this::convertToPackItem)
            .collect(Collectors.toList());
        dto.setItems(itemDTOs);

        return dto;
    }

    private PackTaskDetailDTO.PackItemDTO convertToPackItem(OutboundOrderItem item) {
        PackTaskDetailDTO.PackItemDTO dto = new PackTaskDetailDTO.PackItemDTO();
        dto.setProductId(item.getProductId());
        dto.setSkuCode(item.getSkuCode());
        dto.setProductName(item.getProductName());
        dto.setBarcode(item.getBarcode());
        dto.setQty(item.getQty());
        dto.setPackedQty(item.getPackedQty() != null ? item.getPackedQty() : 0);
        return dto;
    }

    private String generatePackageNo() {
        String datePrefix = "PK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Integer maxSeq = packRecordRepository.selectLatestByOrderId(0L) != null ?
            Integer.parseInt(packRecordRepository.selectLatestByOrderId(0L).getPackageNo().substring(10)) : 0;
        return datePrefix + String.format("%03d", (maxSeq == null ? 0 : maxSeq) + 1);
    }

    private int calculateRecommendLevel(BoxType box, BigDecimal weight, BigDecimal volume) {
        // 计算空间利用率（越高越好，但不要太挤）
        BigDecimal volumeRatio = volume.divide(box.getVolume(), 2, RoundingMode.HALF_UP);

        // 利用率在70%-90%之间是最好的（5星）
        if (volumeRatio.compareTo(new BigDecimal("0.7")) >= 0 &&
            volumeRatio.compareTo(new BigDecimal("0.9")) <= 0) {
            return 5;
        }
        // 利用率在50%-70%或90%-100%是较好的（4星）
        if (volumeRatio.compareTo(new BigDecimal("0.5")) >= 0) {
            return 4;
        }
        // 利用率在30%-50%是一般的（3星）
        if (volumeRatio.compareTo(new BigDecimal("0.3")) >= 0) {
            return 3;
        }
        // 利用率低于30%是不推荐的（2星）
        return 2;
    }

    private String generateReason(BoxType box, BigDecimal weight, BigDecimal volume) {
        BigDecimal volumeRatio = volume.divide(box.getVolume(), 2, RoundingMode.HALF_UP);
        int ratioPercent = volumeRatio.multiply(new BigDecimal(100)).intValue();

        return String.format("空间利用率%d%%，承重%.1fkg，适合%s包裹",
            ratioPercent, box.getMaxWeight().doubleValue(),
            box.getMaxWeight().compareTo(new BigDecimal("10")) < 0 ? "小型" :
            box.getMaxWeight().compareTo(new BigDecimal("20")) < 0 ? "中型" : "大型");
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
