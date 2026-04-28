package com.wms.inbound.controller;

import com.wms.inbound.dto.InboundOrderDTO;
import com.wms.inbound.dto.InboundOrderQueryDTO;
import com.wms.inbound.service.InboundOrderService;
import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 入库单管理控制器
 * 实现阶段二: 入库单CRUD + 送货批次号
 */
@RestController
@RequestMapping("/api/v1/inbound/orders")
@RequiredArgsConstructor
public class InboundOrderController {

    private final InboundOrderService orderService;

    /**
     * 分页查询入库单列表
     */
    @GetMapping
    public Result<Map<String, Object>> listOrders(InboundOrderQueryDTO queryDTO) {
        Map<String, Object> data = orderService.listOrders(queryDTO);
        return Result.success(data);
    }

    /**
     * 获取入库单详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable Long id) {
        Map<String, Object> data = orderService.getOrderDetail(id);
        return Result.success(data);
    }

    /**
     * 创建入库单
     */
    @PostMapping
    @OperationLog(module = "入库管理", action = "CREATE", description = "创建入库单")
    public Result<Map<String, Object>> createOrder(@Validated @RequestBody InboundOrderDTO dto) {
        Long id = orderService.createOrder(dto);
        return Result.success("入库单创建成功", Map.of("id", id));
    }

    /**
     * 更新入库单
     */
    @PutMapping("/{id}")
    @OperationLog(module = "入库管理", action = "UPDATE", description = "更新入库单")
    public Result<Void> updateOrder(@PathVariable Long id, @Validated @RequestBody InboundOrderDTO dto) {
        orderService.updateOrder(id, dto);
        return Result.success("入库单更新成功", null);
    }

    /**
     * 取消入库单
     */
    @PostMapping("/{id}/cancel")
    @OperationLog(module = "入库管理", action = "CANCEL", description = "取消入库单")
    public Result<Void> cancelOrder(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        orderService.cancelOrder(id, reason);
        return Result.success("入库单已取消", null);
    }

    /**
     * 重新激活入库单
     */
    @PostMapping("/{id}/reactivate")
    @OperationLog(module = "入库管理", action = "REACTIVATE", description = "重新激活入库单")
    public Result<Void> reactivateOrder(@PathVariable Long id) {
        orderService.reactivateOrder(id);
        return Result.success("入库单已重新激活", null);
    }

    /**
     * 删除入库单
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "入库管理", action = "DELETE", description = "删除入库单")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return Result.success("入库单已删除", null);
    }

    /**
     * 批量删除入库单
     */
    @DeleteMapping("/batch")
    @OperationLog(module = "入库管理", action = "BATCH_DELETE", description = "批量删除入库单")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        for (Long id : ids) {
            orderService.deleteOrder(id);
        }
        return Result.success("已成功删除" + ids.size() + "个入库单", null);
    }

    /**
     * 按送货批次号查询入库单
     */
    @GetMapping("/by-batch")
    public Result<Map<String, Object>> listByBatchNo(@RequestParam String deliveryBatchNo) {
        InboundOrderQueryDTO queryDTO = new InboundOrderQueryDTO();
        queryDTO.setDeliveryBatchNo(deliveryBatchNo);
        queryDTO.setPage(1);
        queryDTO.setLimit(100);
        Map<String, Object> data = orderService.listOrders(queryDTO);
        return Result.success(data);
    }
}
