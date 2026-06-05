package com.wms.outbound.controller;

import com.wms.outbound.dto.*;
import com.wms.outbound.service.ShipService;
import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 发货管理控制器
 *
 * 实现功能：
 * 1. 扫描出库单号或包裹号确认发货
 * 2. 选择物流公司和录入物流单号
 * 3. 批量确认发货
 */
@RestController
@RequestMapping("/api/v1/outbound/ship")
@RequiredArgsConstructor
public class ShipController {

    private final ShipService shipService;

    /**
     * 确认发货（单个订单）
     */
    @PostMapping("/confirm")
    @OperationLog(module = "出库管理", action = "CONFIRM", description = "确认发货")
    public Result<ShipResultDTO> confirmShip(@Validated @RequestBody ShipConfirmDTO dto) {
        ShipResultDTO result = shipService.confirmShip(dto);
        return Result.success("发货成功", result);
    }

    /**
     * 批量发货
     */
    @PostMapping("/batch")
    @OperationLog(module = "出库管理", action = "BATCH_SHIP", description = "批量发货")
    public Result<ShipResultDTO> batchShip(@Validated @RequestBody ShipConfirmDTO dto) {
        ShipResultDTO result = shipService.batchShip(dto);
        if (result.getFailCount() > 0) {
            return Result.success(
                String.format("批量发货完成：成功%d个，失败%d个", result.getSuccessCount(), result.getFailCount()),
                result
            );
        }
        return Result.success("批量发货成功", result);
    }

    /**
     * 根据包裹号查询发货信息
     */
    @GetMapping("/by-package/{packageNo}")
    public Result<Map<String, Object>> getShipmentByPackageNo(@PathVariable String packageNo) {
        // 这里可以扩展实现查询功能
        return Result.success(Map.of("packageNo", packageNo));
    }

    /**
     * 根据出库单号查询发货信息
     */
    @GetMapping("/by-order/{orderNo}")
    public Result<Map<String, Object>> getShipmentByOrderNo(@PathVariable String orderNo) {
        // 这里可以扩展实现查询功能
        return Result.success(Map.of("orderNo", orderNo));
    }
}
