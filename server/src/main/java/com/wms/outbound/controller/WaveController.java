package com.wms.outbound.controller;

import com.wms.outbound.dto.*;
import com.wms.outbound.entity.Wave;
import com.wms.outbound.service.WaveService;
import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 波次管理控制器
 *
 * 实现功能：
 * 1. 波次列表查询
 * 2. 波次创建（按时间/物流/区域/商品策略）
 * 3. 波次释放（库存锁定）
 * 4. 波次取消（库存释放）
 */
@RestController
@RequestMapping("/api/v1/outbound/waves")
@RequiredArgsConstructor
public class WaveController {

    private final WaveService waveService;

    /**
     * 查询波次列表
     */
    @GetMapping
    public Result<Map<String, Object>> listWaves(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int limit) {
        // TODO: 实现分页查询，暂时返回空列表
        Map<String, Object> result = new HashMap<>();
        result.put("list", List.of());
        result.put("total", 0);
        result.put("page", page);
        result.put("limit", limit);
        return Result.success(result);
    }

    /**
     * 获取波次详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getWaveDetail(@PathVariable Long id) {
        // TODO: 实现详情查询
        return Result.success(new HashMap<>());
    }

    /**
     * 创建波次
     */
    @PostMapping
    @OperationLog(module = "波次管理", action = "CREATE", description = "创建波次")
    public Result<Map<String, Object>> createWave(@RequestBody WaveCreateDTO dto) {
        List<Long> waveIds = waveService.createWaveWithSplit(dto);

        Map<String, Object> data = new HashMap<>();
        data.put("waveIds", waveIds);
        data.put("count", waveIds.size());

        if (waveIds.size() == 1) {
            return Result.success("波次创建成功", data);
        } else {
            return Result.success("已拆分为" + waveIds.size() + "个波次", data);
        }
    }

    /**
     * 释放波次
     */
    @PostMapping("/{id}/release")
    @OperationLog(module = "波次管理", action = "RELEASE", description = "释放波次")
    public Result<WaveReleaseResultDTO> releaseWave(
            @PathVariable Long id,
            @RequestBody(required = false) WaveReleaseDTO dto) {
        if (dto == null) {
            dto = new WaveReleaseDTO();
        }
        dto.setWaveId(id);

        WaveReleaseResultDTO result = waveService.releaseWave(dto);

        if (result.getSuccess()) {
            return Result.success("波次释放成功", result);
        } else {
            return Result.error(400, result.getFailReason());
        }
    }

    /**
     * 取消波次
     */
    @PostMapping("/{id}/cancel")
    @OperationLog(module = "波次管理", action = "CANCEL", description = "取消波次")
    public Result<Void> cancelWave(@PathVariable Long id) {
        waveService.cancelWave(id);
        return Result.success("波次已取消", null);
    }

    /**
     * 获取支持的策略类型
     */
    @GetMapping("/strategies")
    public Result<List<Map<String, Object>>> getStrategies() {
        List<Map<String, Object>> strategies = List.of(
            Map.of("code", Wave.STRATEGY_TIME, "name", "按时间策略", "description", "按订单创建时间范围创建波次"),
            Map.of("code", Wave.STRATEGY_LOGISTICS, "name", "按物流策略", "description", "按物流公司创建波次"),
            Map.of("code", Wave.STRATEGY_REGION, "name", "按区域策略", "description", "按收货区域创建波次"),
            Map.of("code", Wave.STRATEGY_PRODUCT, "name", "按商品策略", "description", "按高频SKU创建波次"),
            Map.of("code", Wave.STRATEGY_CUSTOMER, "name", "按客户策略", "description", "按客户创建波次")
        );
        return Result.success(strategies);
    }
}
