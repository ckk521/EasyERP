package com.wms.inbound.controller;

import com.wms.inbound.dto.PutawayDTO;
import com.wms.inbound.entity.PutawayRecord;
import com.wms.inbound.service.PutawayService;
import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import com.wms.system.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 上架作业控制器
 * 阶段五: 上架作业 + 库存更新
 */
@RestController
@RequestMapping("/api/v1/inbound/putaway")
@RequiredArgsConstructor
public class PutawayController {

    private final PutawayService putawayService;

    /**
     * 智能库位推荐
     * TC-2.10
     */
    @GetMapping("/recommend")
    public Result<List<Map<String, Object>>> recommendLocations(
            @RequestParam Long productId,
            @RequestParam Long warehouseId,
            @RequestParam(required = false) Long zoneId) {
        List<Map<String, Object>> locations = putawayService.recommendLocations(productId, warehouseId, zoneId);
        return Result.success(locations);
    }

    /**
     * 执行上架
     * TC-2.11
     */
    @PostMapping("/execute")
    @OperationLog(module = "入库管理", action = "PUTAWAY", description = "执行上架")
    public Result<Void> putawayItem(@RequestBody PutawayDTO dto) {
        Long userId = UserContext.requireCurrentUserId();
        String username = UserContext.getCurrentUsername();
        putawayService.putawayItem(dto, userId, username);
        return Result.success("上架成功", null);
    }

    /**
     * 查询待上架商品
     */
    @GetMapping("/pending/{orderId}")
    public Result<Map<String, Object>> listPendingItems(@PathVariable Long orderId) {
        Map<String, Object> data = putawayService.listPendingItems(orderId);
        return Result.success(data);
    }

    /**
     * 查询入库明细的上架记录
     */
    @GetMapping("/records/item/{itemId}")
    public Result<List<PutawayRecord>> getItemPutawayRecords(@PathVariable Long itemId) {
        List<PutawayRecord> records = putawayService.getPutawayRecords(itemId);
        return Result.success(records);
    }

    /**
     * 查询入库单的所有上架记录
     */
    @GetMapping("/records/order/{orderId}")
    public Result<List<PutawayRecord>> getOrderPutawayRecords(@PathVariable Long orderId) {
        List<PutawayRecord> records = putawayService.getOrderPutawayRecords(orderId);
        return Result.success(records);
    }

    /**
     * 查询最近的上架记录（全局）
     */
    @GetMapping("/records/recent")
    public Result<Map<String, Object>> getRecentPutawayRecords(
            @RequestParam(defaultValue = "20") int limit) {
        List<PutawayRecord> records = putawayService.getRecentPutawayRecords(limit);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("list", records);
        return Result.success(result);
    }
}
