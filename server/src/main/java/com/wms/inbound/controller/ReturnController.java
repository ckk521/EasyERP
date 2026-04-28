package com.wms.inbound.controller;

import com.wms.inbound.dto.RejectHandleDTO;
import com.wms.inbound.service.ReturnService;
import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 退货处理控制器
 * 阶段六: 退货处理 + 数据关联查询
 */
@RestController
@RequestMapping("/api/v1/inbound/return")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    /**
     * 查询不合格品列表
     */
    @GetMapping("/rejects")
    public Result<Map<String, Object>> listRejectRecords(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Integer handleStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Map<String, Object> data = returnService.listRejectRecords(orderId, handleStatus, page, limit);
        return Result.success(data);
    }

    /**
     * 获取不合格品详情
     */
    @GetMapping("/rejects/{rejectId}")
    public Result<Map<String, Object>> getRejectDetail(@PathVariable Long rejectId) {
        Map<String, Object> data = returnService.getRejectDetail(rejectId);
        return Result.success(data);
    }

    /**
     * 处理不合格品
     * TC-2.9
     */
    @PostMapping("/handle")
    @OperationLog(module = "入库管理", action = "HANDLE_REJECT", description = "处理不合格品")
    public Result<Void> handleReject(@RequestBody RejectHandleDTO dto) {
        Long userId = 1L; // TODO: 从SecurityContext获取
        returnService.handleReject(dto, userId);
        return Result.success("处理成功", null);
    }

    /**
     * 按入库单号查询退货记录
     */
    @GetMapping("/by-order/{orderNo}")
    public Result<List<Map<String, Object>>> listByInboundOrderNo(@PathVariable String orderNo) {
        List<Map<String, Object>> data = returnService.listByInboundOrderNo(orderNo);
        return Result.success(data);
    }
}
