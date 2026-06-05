package com.wms.outbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.outbound.dto.*;
import com.wms.outbound.entity.*;
import com.wms.outbound.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 拣货服务
 *
 * 实现功能：
 * 1. 拣货任务领取
 * 2. 扫码拣货（库位码、商品条码）
 * 3. 拣货确认（数量录入、差异处理）
 * 4. 拣货异常标记
 * 5. 拣货完成确认
 * 6. 智能拣货路径优化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PickService {

    private final PickTaskRepository pickTaskRepository;
    private final PickRecordRepository pickRecordRepository;
    private final OutboundOrderRepository orderRepository;
    private final OutboundOrderItemRepository orderItemRepository;
    private final WaveRepository waveRepository;
    private final InventoryAllocationRepository allocationRepository;

    /**
     * 查看分配给自己的拣货任务列表
     */
    public List<PickTaskDetailDTO> getAssignedTasks(Long userId) {
        // 查询待领取的任务
        List<PickTask> tasks = pickTaskRepository.selectPendingByUserId(userId);

        return tasks.stream()
            .map(this::convertToTaskDetail)
            .collect(Collectors.toList());
    }

    /**
     * 根据出库单ID获取拣货记录
     * 简化流程：直接从出库单获取拣货记录，不依赖波次
     */
    public PickTaskDetailDTO getPickRecordsByOrderId(Long orderId) {
        OutboundOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("出库单不存在");
        }

        // 查询该出库单的拣货记录
        List<PickRecord> records = pickRecordRepository.selectByOrderId(orderId);

        // 按路径排序
        List<PickRecord> sortedRecords = sortPickPath(records);

        // 构建返回结果
        PickTaskDetailDTO dto = new PickTaskDetailDTO();
        dto.setTaskId(orderId); // 使用订单ID作为临时任务ID
        dto.setTaskNo(order.getOrderNo());
        dto.setWaveId(null);
        dto.setWaveNo(null);
        dto.setStatus(order.getStatus());
        dto.setStatusName(getOrderStatusName(order.getStatus()));

        // 计算统计信息
        int totalItems = records.size();
        int completedItems = (int) records.stream().filter(r -> r.getStatus() == PickRecord.STATUS_COMPLETED || r.getStatus() == PickRecord.STATUS_EXCEPTION).count();
        int totalQty = records.stream().mapToInt(PickRecord::getPlanQty).sum();
        int pickedQty = records.stream().filter(r -> r.getActualQty() != null).mapToInt(PickRecord::getActualQty).sum();

        dto.setTotalItems(totalItems);
        dto.setCompletedItems(completedItems);
        dto.setTotalQty(totalQty);
        dto.setPickedQty(pickedQty);

        // 转换拣货明细
        List<PickTaskDetailDTO.PickItemDTO> items = sortedRecords.stream()
            .map(this::convertToPickItem)
            .collect(Collectors.toList());
        dto.setPickItems(items);

        return dto;
    }

    /**
     * 领取拣货任务
     */
    @Transactional
    public PickTaskDetailDTO claimTask(Long taskId, PickClaimDTO dto) {
        // 1. 查询任务
        PickTask task = pickTaskRepository.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        if (task.getStatus() != PickTask.STATUS_PENDING) {
            throw new IllegalStateException("任务状态不允许领取");
        }

        // 2. 查询拣货明细
        List<PickRecord> records = pickRecordRepository.selectByWaveId(task.getWaveId());

        // 3. 优化拣货路径
        List<PickRecord> sortedRecords = sortPickPath(records);

        // 更新拣货记录的排序序号
        for (int i = 0; i < sortedRecords.size(); i++) {
            PickRecord record = sortedRecords.get(i);
            record.setSortOrder(i);
            record.setPickUserId(dto.getPickUserId());
            record.setPickUserName(dto.getPickUserName());
            record.setClaimTime(LocalDateTime.now());
            pickRecordRepository.updateById(record);
        }

        // 4. 更新任务状态
        task.setStatus(PickTask.STATUS_IN_PROGRESS);
        task.setPickUserId(dto.getPickUserId());
        task.setPickUserName(dto.getPickUserName());
        task.setClaimTime(LocalDateTime.now());
        pickTaskRepository.updateById(task);

        log.info("拣货任务领取成功: taskId={}, user={}", taskId, dto.getPickUserName());

        return convertToTaskDetail(task, sortedRecords);
    }

    /**
     * 放弃任务（返回任务池）
     */
    @Transactional
    public boolean abandonTask(Long taskId) {
        PickTask task = pickTaskRepository.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        if (task.getStatus() == PickTask.STATUS_COMPLETED) {
            throw new IllegalStateException("已完成的任务不能放弃");
        }

        // 更新任务状态
        task.setStatus(PickTask.STATUS_PENDING);
        task.setPickUserId(null);
        task.setPickUserName(null);
        pickTaskRepository.updateById(task);

        log.info("拣货任务已放弃: taskId={}", taskId);
        return true;
    }

    /**
     * 扫描库位码
     */
    @Transactional
    public boolean scanLocation(Long recordId, String locationCode) {
        PickRecord record = pickRecordRepository.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("拣货记录不存在");
        }

        // 检查库位是否匹配
        if (!record.getLocationCode().equals(locationCode)) {
            throw new IllegalArgumentException(
                "库位不匹配，请前往" + record.getLocationCode()
            );
        }

        // 如果已扫码，返回成功（幂等性）
        if (record.getLocationScanned() == PickRecord.SCAN_YES) {
            return true;
        }

        // 更新扫码状态
        record.setLocationScanned(PickRecord.SCAN_YES);
        record.setStatus(PickRecord.STATUS_PICKING);
        record.setStartTime(LocalDateTime.now());
        pickRecordRepository.updateById(record);

        log.info("库位扫码成功: recordId={}, location={}", recordId, locationCode);
        return true;
    }

    /**
     * 扫描商品条码
     */
    @Transactional
    public boolean scanProduct(Long recordId, String barcode) {
        PickRecord record = pickRecordRepository.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("拣货记录不存在");
        }

        // 检查库位是否已扫码
        if (record.getLocationScanned() != PickRecord.SCAN_YES) {
            throw new IllegalStateException("请先扫描库位码");
        }

        // 检查商品是否匹配
        if (!record.getBarcode().equals(barcode)) {
            throw new IllegalArgumentException(
                "商品不匹配，应拣商品条码：" + record.getBarcode()
            );
        }

        // 如果已扫码，返回成功（幂等性）
        if (record.getProductScanned() == PickRecord.SCAN_YES) {
            return true;
        }

        // 更新扫码状态
        record.setProductScanned(PickRecord.SCAN_YES);
        pickRecordRepository.updateById(record);

        log.info("商品扫码成功: recordId={}, barcode={}", recordId, barcode);
        return true;
    }

    /**
     * 确认拣货
     */
    @Transactional
    public PickCompleteResultDTO confirmPick(PickConfirmDTO dto) {
        PickRecord record = pickRecordRepository.selectById(dto.getRecordId());
        if (record == null) {
            throw new IllegalArgumentException("拣货记录不存在");
        }

        // 检查扫码状态
        if (record.getLocationScanned() != PickRecord.SCAN_YES) {
            throw new IllegalStateException("请先扫描库位码");
        }
        if (record.getProductScanned() != PickRecord.SCAN_YES) {
            throw new IllegalStateException("请先扫描商品条码");
        }

        // 检查拣货数量
        if (dto.getActualQty() == null || dto.getActualQty() < 0) {
            throw new IllegalArgumentException("拣货数量不能为空或负数");
        }

        if (dto.getActualQty() > record.getPlanQty()) {
            throw new IllegalArgumentException("拣货数量不能超过计划数量");
        }

        // 拣货数量为0必须标记异常
        if (dto.getActualQty() == 0 && !Boolean.TRUE.equals(dto.getIsException())) {
            throw new IllegalArgumentException("拣货数量为0时，必须标记异常");
        }

        PickCompleteResultDTO result = new PickCompleteResultDTO();
        result.setOutboundOrderId(record.getOutboundOrderId());
        result.setOutboundOrderNo(record.getOutboundOrderNo());

        // 处理异常情况
        if (Boolean.TRUE.equals(dto.getIsException())) {
            record.setIsException(1);
            record.setExceptionType(dto.getExceptionType());
            record.setExceptionQty(dto.getExceptionQty());
            record.setExceptionRemark(dto.getExceptionRemark());
            record.setStatus(PickRecord.STATUS_EXCEPTION);
        }

        // 更新拣货记录
        record.setActualQty(dto.getActualQty());
        record.setDiffQty(record.getPlanQty() - dto.getActualQty());
        record.setDiffReason(dto.getDiffReason());

        // 如果拣货数量等于计划数量且无异常，标记为已完成
        if (dto.getActualQty().equals(record.getPlanQty()) && !Boolean.TRUE.equals(dto.getIsException())) {
            record.setStatus(PickRecord.STATUS_COMPLETED);
            record.setCompleteTime(LocalDateTime.now());
        }

        pickRecordRepository.updateById(record);

        // 更新出库单明细的已拣数量
        updateOrderItemPickedQty(record);

        result.setSuccess(true);
        log.info("拣货确认成功: recordId={}, actualQty={}, diff={}",
            dto.getRecordId(), dto.getActualQty(), record.getDiffQty());

        return result;
    }

    /**
     * 完成订单拣货
     */
    @Transactional
    public PickCompleteResultDTO completeOrderPick(Long orderId) {
        OutboundOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("出库单不存在");
        }

        // 查询拣货记录
        List<PickRecord> records = pickRecordRepository.selectByOrderId(orderId);

        // 检查是否全部完成
        boolean allCompleted = records.stream()
            .allMatch(r -> r.getStatus() == PickRecord.STATUS_COMPLETED ||
                          r.getStatus() == PickRecord.STATUS_EXCEPTION);

        if (!allCompleted) {
            throw new IllegalStateException("还有未完成的拣货项");
        }

        // 计算差异
        List<PickCompleteResultDTO.PickDifferenceDTO> differences = new ArrayList<>();
        for (PickRecord record : records) {
            if (record.getDiffQty() != null && record.getDiffQty() > 0) {
                PickCompleteResultDTO.PickDifferenceDTO diff = new PickCompleteResultDTO.PickDifferenceDTO();
                diff.setProductId(record.getProductId());
                diff.setSkuCode(record.getSkuCode());
                diff.setProductName(record.getProductName());
                diff.setPlanQty(record.getPlanQty());
                diff.setActualQty(record.getActualQty());
                diff.setDiffQty(record.getDiffQty());
                diff.setDiffReason(record.getDiffReason());
                diff.setIsException(record.getIsException() == 1);
                diff.setExceptionType(record.getExceptionType());
                differences.add(diff);
            }
        }

        // 更新订单状态为待打包
        order.setStatus(OutboundOrder.STATUS_PACKING);
        orderRepository.updateById(order);

        PickCompleteResultDTO result = new PickCompleteResultDTO();
        result.setSuccess(true);
        result.setOutboundOrderId(orderId);
        result.setOutboundOrderNo(order.getOrderNo());
        result.setNewStatus(OutboundOrder.STATUS_PACKING);
        result.setHasDifference(!differences.isEmpty());
        result.setDifferenceList(differences);

        log.info("订单拣货完成: orderId={}, hasDiff={}", orderId, !differences.isEmpty());

        return result;
    }

    /**
     * 优化拣货路径
     * 按库区→巷道→层的顺序排序
     */
    public List<PickRecord> sortPickPath(List<PickRecord> records) {
        return records.stream()
            .sorted((r1, r2) -> {
                String loc1 = r1.getLocationCode();
                String loc2 = r2.getLocationCode();

                // 解析库位编码：A-R01-L01
                // 格式：库区-巷道-层
                String[] parts1 = loc1.split("-");
                String[] parts2 = loc2.split("-");

                // 先按库区排序
                int zoneCompare = parts1[0].compareTo(parts2[0]);
                if (zoneCompare != 0) {
                    return zoneCompare;
                }

                // 同库区按巷道排序
                if (parts1.length >= 2 && parts2.length >= 2) {
                    int aisleCompare = parts1[1].compareTo(parts2[1]);
                    if (aisleCompare != 0) {
                        return aisleCompare;
                    }
                }

                // 同巷道按层排序
                if (parts1.length >= 3 && parts2.length >= 3) {
                    return parts1[2].compareTo(parts2[2]);
                }

                return 0;
            })
            .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    private PickTaskDetailDTO convertToTaskDetail(PickTask task) {
        List<PickRecord> records = pickRecordRepository.selectByWaveId(task.getWaveId());
        return convertToTaskDetail(task, records);
    }

    private PickTaskDetailDTO convertToTaskDetail(PickTask task, List<PickRecord> records) {
        PickTaskDetailDTO dto = new PickTaskDetailDTO();
        dto.setTaskId(task.getId());
        dto.setTaskNo(task.getTaskNo());
        dto.setWaveId(task.getWaveId());
        dto.setWaveNo(task.getWaveNo());
        dto.setStatus(task.getStatus());
        dto.setStatusName(getStatusName(task.getStatus()));
        dto.setPickUserId(task.getPickUserId());
        dto.setPickUserName(task.getPickUserName());
        dto.setTotalItems(task.getTotalItems());
        dto.setCompletedItems(task.getCompletedItems());
        dto.setTotalQty(task.getTotalQty());
        dto.setPickedQty(task.getPickedQty());

        // 转换拣货明细
        List<PickTaskDetailDTO.PickItemDTO> items = records.stream()
            .map(this::convertToPickItem)
            .collect(Collectors.toList());
        dto.setPickItems(items);

        return dto;
    }

    private PickTaskDetailDTO.PickItemDTO convertToPickItem(PickRecord record) {
        PickTaskDetailDTO.PickItemDTO item = new PickTaskDetailDTO.PickItemDTO();
        item.setRecordId(record.getId());
        item.setOrderNo(record.getOutboundOrderNo());
        item.setProductId(record.getProductId());
        item.setSkuCode(record.getSkuCode());
        item.setProductName(record.getProductName());
        item.setBarcode(record.getBarcode());
        item.setLocationId(record.getLocationId());
        item.setLocationCode(record.getLocationCode());
        item.setBatchNo(record.getBatchNo());
        item.setPlanQty(record.getPlanQty());
        item.setActualQty(record.getActualQty());
        item.setDiffQty(record.getDiffQty());
        item.setStatus(record.getStatus());
        item.setStatusName(getRecordStatusName(record.getStatus()));
        item.setLocationScanned(record.getLocationScanned() != null && record.getLocationScanned() == PickRecord.SCAN_YES);
        item.setProductScanned(record.getProductScanned() != null && record.getProductScanned() == PickRecord.SCAN_YES);
        item.setSortOrder(record.getSortOrder());
        item.setIsException(record.getIsException() == 1);
        item.setExceptionType(record.getExceptionType());
        item.setExceptionQty(record.getExceptionQty());
        item.setExceptionRemark(record.getExceptionRemark());
        return item;
    }

    private void updateOrderItemPickedQty(PickRecord record) {
        if (record.getOutboundItemId() == null) {
            return;
        }

        OutboundOrderItem item = orderItemRepository.selectById(record.getOutboundItemId());
        if (item != null) {
            item.setPickedQty(record.getActualQty());
            if (record.getStatus() == PickRecord.STATUS_COMPLETED) {
                item.setStatus(OutboundOrderItem.STATUS_PICKED);
                item.setPickTime(LocalDateTime.now());
            }
            orderItemRepository.updateById(item);
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case PickTask.STATUS_PENDING: return "待领取";
            case PickTask.STATUS_IN_PROGRESS: return "进行中";
            case PickTask.STATUS_COMPLETED: return "已完成";
            case PickTask.STATUS_CANCELLED: return "已取消";
            default: return "";
        }
    }

    private String getRecordStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case PickRecord.STATUS_PENDING: return "待拣货";
            case PickRecord.STATUS_PICKING: return "拣货中";
            case PickRecord.STATUS_COMPLETED: return "已完成";
            case PickRecord.STATUS_EXCEPTION: return "异常";
            case PickRecord.STATUS_CANCELLED: return "已取消";
            default: return "";
        }
    }

    private String getOrderStatusName(Integer status) {
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
}