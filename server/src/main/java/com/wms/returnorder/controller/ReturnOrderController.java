package com.wms.returnorder.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.returnorder.dto.*;
import com.wms.returnorder.entity.ReturnOrder;
import com.wms.returnorder.service.ReturnOrderService;
import com.wms.system.common.Result;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 退货单 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/return")
@RequiredArgsConstructor
public class ReturnOrderController {

    private final ReturnOrderService returnOrderService;

    /**
     * 退货单列表
     */
    @GetMapping("/orders")
    public Result<Page<ReturnOrderDTO>> getReturnOrderList(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {
        Page<ReturnOrderDTO> pageResult = returnOrderService.getReturnOrderPage(status, keyword, page, limit);
        return Result.success(pageResult);
    }

    /**
     * 退货单详情
     */
    @GetMapping("/orders/{id}")
    public Result<ReturnOrderDTO> getReturnOrderDetail(@PathVariable Long id) {
        ReturnOrderDTO dto = returnOrderService.getReturnOrderDetail(id);
        if (dto == null) {
            return Result.error(404, "退货单不存在");
        }
        return Result.success(dto);
    }

    /**
     * 创建退货单
     */
    @PostMapping("/orders")
    public Result<ReturnOrderDTO> createReturnOrder(@RequestBody ReturnOrderCreateDTO dto, HttpServletRequest request) {
        // 获取当前用户信息
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims != null) {
            dto.setCreateUserId(claims.get("userId", Long.class));
            dto.setCreateUserName(claims.getSubject());
        }

        try {
            ReturnOrder returnOrder = returnOrderService.createReturnOrder(dto);
            ReturnOrderDTO result = returnOrderService.getReturnOrderDetail(returnOrder.getId());
            return Result.success("退货单创建成功", result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 确认收货
     */
    @PostMapping("/orders/{id}/receive")
    public Result<ReturnOrderDTO> confirmReceive(
            @PathVariable Long id,
            @RequestBody ReturnReceiveDTO dto,
            HttpServletRequest request) {
        // 获取当前用户信息
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims != null) {
            dto.setReceiveUserId(claims.get("userId", Long.class));
            dto.setReceiveUserName(claims.getSubject());
        }
        dto.setReturnOrderId(id);

        try {
            ReturnOrder returnOrder = returnOrderService.confirmReceive(dto);
            ReturnOrderDTO result = returnOrderService.getReturnOrderDetail(returnOrder.getId());
            return Result.success("收货成功，已生成入库单", result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 取消退货单
     */
    @PostMapping("/orders/{id}/cancel")
    public Result<ReturnOrderDTO> cancelReturnOrder(
            @PathVariable Long id,
            @RequestBody CancelDTO dto) {
        try {
            ReturnOrder returnOrder = returnOrderService.cancelReturnOrder(id, dto.getCancelReason());
            ReturnOrderDTO result = returnOrderService.getReturnOrderDetail(returnOrder.getId());
            return Result.success("退货单已取消", result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 取消原因DTO
     */
    @lombok.Data
    public static class CancelDTO {
        private String cancelReason;
    }
}