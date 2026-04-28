package com.wms.inbound.controller;

import com.wms.inbound.dto.InspectDTO;
import com.wms.inbound.entity.InspectRecord;
import com.wms.inbound.service.InspectService;
import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import com.wms.system.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 验收作业控制器
 * 阶段四: 验收作业 + 不合格处理
 */
@RestController
@RequestMapping("/api/v1/inbound/inspect")
@RequiredArgsConstructor
public class InspectController {

    private final InspectService inspectService;

    /**
     * 执行验收
     * TC-2.8
     */
    @PostMapping("/execute")
    @OperationLog(module = "入库管理", action = "INSPECT", description = "执行验收")
    public Result<Void> inspectItem(@RequestBody InspectDTO dto) {
        Long userId = UserContext.requireCurrentUserId();
        String username = UserContext.getCurrentUsername();
        inspectService.inspectItem(dto, userId, username);
        return Result.success("验收成功", null);
    }

    /**
     * 批量验收
     * TC-2.8.6
     */
    @PostMapping("/batch")
    @OperationLog(module = "入库管理", action = "BATCH_INSPECT", description = "批量验收")
    public Result<Void> batchInspect(@RequestBody InspectDTO dto) {
        Long userId = UserContext.requireCurrentUserId();
        String username = UserContext.getCurrentUsername();
        inspectService.batchInspect(dto, userId, username);
        return Result.success("批量验收成功", null);
    }

    /**
     * 查询待验收商品
     */
    @GetMapping("/pending/{orderId}")
    public Result<Map<String, Object>> listPendingItems(@PathVariable Long orderId) {
        Map<String, Object> data = inspectService.listPendingItems(orderId);
        return Result.success(data);
    }

    /**
     * 查询入库明细的验收记录
     */
    @GetMapping("/records/item/{itemId}")
    public Result<List<InspectRecord>> getItemInspectRecords(@PathVariable Long itemId) {
        List<InspectRecord> records = inspectService.getInspectRecords(itemId);
        return Result.success(records);
    }

    /**
     * 查询入库单的所有验收记录
     */
    @GetMapping("/records/order/{orderId}")
    public Result<List<InspectRecord>> getOrderInspectRecords(@PathVariable Long orderId) {
        List<InspectRecord> records = inspectService.getOrderInspectRecords(orderId);
        return Result.success(records);
    }

    /**
     * 查询最近的验收记录（全局）
     */
    @GetMapping("/records/recent")
    public Result<Map<String, Object>> getRecentInspectRecords(
            @RequestParam(defaultValue = "20") int limit) {
        List<InspectRecord> records = inspectService.getRecentInspectRecords(limit);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("list", records);
        return Result.success(result);
    }
}
