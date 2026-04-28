package com.wms.inbound.controller;

import com.wms.inbound.service.InboundProgressService;
import com.wms.system.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 入库进度控制器
 * 阶段七: 进度展示 + 统计报表
 */
@RestController
@RequestMapping("/api/v1/inbound/progress")
@RequiredArgsConstructor
public class InboundProgressController {

    private final InboundProgressService progressService;

    /**
     * 查询入库进度
     * TC-2.13
     */
    @GetMapping("/{orderId}")
    public Result<Map<String, Object>> getInboundProgress(@PathVariable Long orderId) {
        Map<String, Object> data = progressService.getInboundProgress(orderId);
        return Result.success(data);
    }

    /**
     * 按供应商+送货批次号查询入库进度
     * TC-2.13.1
     */
    @GetMapping("/by-batch")
    public Result<Map<String, Object>> getProgressByBatch(
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String deliveryBatchNo) {
        Map<String, Object> data = progressService.getProgressByBatch(supplierName, deliveryBatchNo);
        return Result.success(data);
    }

    /**
     * 获取入库生命周期时间轴
     * TC-2.14
     */
    @GetMapping("/timeline/{orderId}")
    public Result<List<Map<String, Object>>> getLifecycleTimeline(@PathVariable Long orderId) {
        List<Map<String, Object>> timeline = progressService.getLifecycleTimeline(orderId);
        return Result.success(timeline);
    }
}
