package com.wms.outbound.controller;

import com.wms.outbound.dto.*;
import com.wms.outbound.service.PickService;
import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 拣货管理控制器
 *
 * 实现功能：
 * 1. 查看分配给自己的拣货任务列表
 * 2. 领取拣货任务
 * 3. 放弃任务（返回任务池）
 * 4. 扫码拣货（库位码、商品条码）
 * 5. 拣货确认（数量录入、差异处理）
 * 6. 完成订单拣货
 */
@RestController
@RequestMapping("/api/v1/outbound/pick")
@RequiredArgsConstructor
public class PickController {

    private final PickService pickService;

    /**
     * 查看分配给自己的拣货任务列表
     */
    @GetMapping("/tasks")
    public Result<List<PickTaskDetailDTO>> getAssignedTasks(@RequestParam Long userId) {
        List<PickTaskDetailDTO> tasks = pickService.getAssignedTasks(userId);
        return Result.success(tasks);
    }

    /**
     * 根据出库单ID获取拣货记录
     */
    @GetMapping("/records/{orderId}")
    public Result<PickTaskDetailDTO> getPickRecordsByOrderId(@PathVariable Long orderId) {
        PickTaskDetailDTO result = pickService.getPickRecordsByOrderId(orderId);
        return Result.success(result);
    }

    /**
     * 领取拣货任务
     */
    @PostMapping("/claim")
    @OperationLog(module = "出库管理", action = "CLAIM", description = "领取拣货任务")
    public Result<PickTaskDetailDTO> claimTask(@RequestBody PickClaimDTO dto) {
        PickTaskDetailDTO result = pickService.claimTask(dto.getWaveId(), dto);
        return Result.success("任务领取成功", result);
    }

    /**
     * 放弃任务（返回任务池）
     */
    @PostMapping("/abandon")
    @OperationLog(module = "出库管理", action = "ABANDON", description = "放弃拣货任务")
    public Result<Void> abandonTask(@RequestBody Map<String, Long> request) {
        Long taskId = request.get("taskId");
        pickService.abandonTask(taskId);
        return Result.success("任务已放弃", null);
    }

    /**
     * 扫描库位码
     */
    @PostMapping("/scan-location")
    @OperationLog(module = "出库管理", action = "SCAN", description = "扫描库位码")
    public Result<Boolean> scanLocation(@RequestBody Map<String, Object> request) {
        Long recordId = Long.valueOf(request.get("recordId").toString());
        String locationCode = (String) request.get("locationCode");

        boolean result = pickService.scanLocation(recordId, locationCode);
        return Result.success("库位扫码成功", result);
    }

    /**
     * 扫描商品条码
     */
    @PostMapping("/scan-product")
    @OperationLog(module = "出库管理", action = "SCAN", description = "扫描商品条码")
    public Result<Boolean> scanProduct(@RequestBody Map<String, Object> request) {
        Long recordId = Long.valueOf(request.get("recordId").toString());
        String barcode = (String) request.get("barcode");

        boolean result = pickService.scanProduct(recordId, barcode);
        return Result.success("商品扫码成功", result);
    }

    /**
     * 确认拣货
     */
    @PostMapping("/confirm")
    @OperationLog(module = "出库管理", action = "CONFIRM", description = "确认拣货")
    public Result<PickCompleteResultDTO> confirmPick(@Validated @RequestBody PickConfirmDTO dto) {
        PickCompleteResultDTO result = pickService.confirmPick(dto);
        return Result.success("拣货确认成功", result);
    }

    /**
     * 完成订单拣货
     */
    @PostMapping("/complete-order")
    @OperationLog(module = "出库管理", action = "COMPLETE", description = "完成订单拣货")
    public Result<PickCompleteResultDTO> completeOrderPick(@RequestBody Map<String, Long> request) {
        Long orderId = request.get("orderId");
        PickCompleteResultDTO result = pickService.completeOrderPick(orderId);
        return Result.success("订单拣货完成", result);
    }
}