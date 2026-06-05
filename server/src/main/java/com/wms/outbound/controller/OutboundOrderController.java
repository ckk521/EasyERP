package com.wms.outbound.controller;

import com.wms.outbound.dto.OutboundOrderDTO;
import com.wms.outbound.dto.OutboundOrderQueryDTO;
import com.wms.outbound.service.OutboundOrderService;
import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 出库单管理控制器
 *
 * 实现功能：
 * 1. 出库单创建（销售出库、手工创建）
 * 2. 出库单查询（列表、详情）
 * 3. 出库单取消
 * 4. 出库单更新、删除
 */
@RestController
@RequestMapping("/api/v1/outbound/orders")
@RequiredArgsConstructor
public class OutboundOrderController {

    private final OutboundOrderService orderService;

    /**
     * 分页查询出库单列表
     */
    @GetMapping
    public Result<Map<String, Object>> listOrders(OutboundOrderQueryDTO queryDTO) {
        Map<String, Object> data = orderService.listOrders(queryDTO);
        return Result.success(data);
    }

    /**
     * 获取出库单详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable Long id) {
        Map<String, Object> data = orderService.getOrderDetail(id);
        return Result.success(data);
    }

    /**
     * 创建出库单
     */
    @PostMapping
    @OperationLog(module = "出库管理", action = "CREATE", description = "创建出库单")
    public Result<Map<String, Object>> createOrder(@Validated @RequestBody OutboundOrderDTO dto) {
        Long id = orderService.createOrder(dto);
        return Result.success("出库单创建成功", Map.of("id", id));
    }

    /**
     * 更新出库单
     */
    @PutMapping("/{id}")
    @OperationLog(module = "出库管理", action = "UPDATE", description = "更新出库单")
    public Result<Void> updateOrder(@PathVariable Long id, @Validated @RequestBody OutboundOrderDTO dto) {
        orderService.updateOrder(id, dto);
        return Result.success("出库单更新成功", null);
    }

    /**
     * 取消出库单
     */
    @PostMapping("/{id}/cancel")
    @OperationLog(module = "出库管理", action = "CANCEL", description = "取消出库单")
    public Result<Void> cancelOrder(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        orderService.cancelOrder(id, reason);
        return Result.success("出库单已取消", null);
    }

    /**
     * 删除出库单
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "出库管理", action = "DELETE", description = "删除出库单")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return Result.success("出库单已删除", null);
    }

    /**
     * 批量删除出库单
     */
    @DeleteMapping("/batch")
    @OperationLog(module = "出库管理", action = "BATCH_DELETE", description = "批量删除出库单")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        for (Long id : ids) {
            orderService.deleteOrder(id);
        }
        return Result.success("已成功删除" + ids.size() + "个出库单", null);
    }

    /**
     * 设置优先级
     */
    @PatchMapping("/{id}/priority")
    @OperationLog(module = "出库管理", action = "UPDATE", description = "设置出库单优先级")
    public Result<Void> setPriority(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        Integer priority = request.get("priority");
        orderService.setPriority(id, priority);
        return Result.success("优先级设置成功", null);
    }

    /**
     * 按销售订单号查询出库单
     */
    @GetMapping("/by-so")
    public Result<Map<String, Object>> listBySoNo(@RequestParam String soNo) {
        OutboundOrderQueryDTO queryDTO = new OutboundOrderQueryDTO();
        queryDTO.setSoNo(soNo);
        queryDTO.setPage(1);
        queryDTO.setLimit(100);
        Map<String, Object> data = orderService.listOrders(queryDTO);
        return Result.success(data);
    }

    /**
     * 分配出库单（创建波次并释放）
     * 将待分配状态的出库单直接转为拣货中状态
     */
    @PostMapping("/{id}/allocate")
    @OperationLog(module = "出库管理", action = "ALLOCATE", description = "分配出库单")
    public Result<Map<String, Object>> allocateOrder(@PathVariable Long id) {
        Map<String, Object> result = orderService.allocateOrder(id);
        return Result.success("出库单分配成功", result);
    }
}