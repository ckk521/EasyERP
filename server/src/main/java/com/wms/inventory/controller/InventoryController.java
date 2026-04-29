package com.wms.inventory.controller;

import com.wms.inventory.dto.*;
import com.wms.inventory.service.InventoryService;
import com.wms.system.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 库存管理控制器
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 实时库存查询（分页）
     * Story 4.1
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> queryInventory(InventoryQueryDTO query) {
        Map<String, Object> result = inventoryService.queryInventory(query);
        return Result.success(result);
    }

    /**
     * 库存明细查询（单个商品分布）
     * Story 4.2
     */
    @GetMapping("/detail/{productId}")
    public Result<List<InventoryVO>> getProductInventoryDetail(
            @PathVariable Long productId,
            @RequestParam(required = false) Long warehouseId) {
        List<InventoryVO> details = inventoryService.getProductInventoryDetail(productId, warehouseId);
        return Result.success(details);
    }

    /**
     * 批次库存查询
     * Story 4.3
     */
    @GetMapping("/batch")
    public Result<List<BatchInventoryVO>> queryBatchInventory(InventoryQueryDTO query) {
        List<BatchInventoryVO> result = inventoryService.queryBatchInventory(query);
        return Result.success(result);
    }

    /**
     * 效期预警监控
     * Story 4.17
     */
    @GetMapping("/expiry-warning")
    public Result<List<ExpiryWarningVO>> getExpiryWarnings(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer expiryStatus) {
        List<ExpiryWarningVO> warnings = inventoryService.getExpiryWarnings(warehouseId, expiryStatus);
        return Result.success(warnings);
    }

    /**
     * 库存汇总统计
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> getInventorySummary(
            @RequestParam(required = false) Long warehouseId) {
        Map<String, Object> summary = inventoryService.getInventorySummary(warehouseId);
        return Result.success(summary);
    }

    /**
     * 手动触发效期状态更新
     */
    @PostMapping("/expiry-status/update")
    public Result<Void> updateExpiryStatus() {
        inventoryService.updateExpiryStatus();
        return Result.success("效期状态已更新", null);
    }

    /**
     * 修复库存批次号
     */
    @PostMapping("/batch-no/fix")
    public Result<Integer> fixInventoryBatchNo() {
        int fixedCount = inventoryService.fixInventoryBatchNo();
        return Result.success("修复完成，共更新 " + fixedCount + " 条记录", fixedCount);
    }
}