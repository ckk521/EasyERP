package com.wms.exception.controller;

import com.wms.exception.dto.ExceptionOrderCreateDTO;
import com.wms.exception.dto.HandleDTO;
import com.wms.exception.dto.IsolateDTO;
import com.wms.exception.service.ExceptionService;
import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import com.wms.system.dto.PageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exception-orders")
@RequiredArgsConstructor
public class ExceptionController {

    private final ExceptionService exceptionService;

    /**
     * 创建异常处理单
     */
    @PostMapping
    @OperationLog(module = "异常管理", action = "CREATE", description = "创建异常处理单")
    public Result<Map<String, Object>> createExceptionOrder(
            @RequestBody ExceptionOrderCreateDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userName = (String) request.getAttribute("userName");
        Map<String, Object> data = exceptionService.createExceptionOrder(dto, userId, userName);
        return Result.success("异常处理单创建成功", data);
    }

    /**
     * 查询异常处理单列表
     */
    @GetMapping
    public Result<Map<String, Object>> listExceptionOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long inboundOrderId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Integer exceptionType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(page);
        pageDTO.setLimit(limit);

        Map<String, Object> data = exceptionService.listExceptionOrders(
                pageDTO, keyword, inboundOrderId, supplierId, exceptionType, status, startTime, endTime);
        return Result.success(data);
    }

    /**
     * 查询异常处理单详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getExceptionOrder(@PathVariable Long id) {
        Map<String, Object> data = exceptionService.getExceptionOrderById(id);
        return Result.success(data);
    }

    /**
     * 隔离入库
     */
    @PostMapping("/{id}/isolate")
    @OperationLog(module = "异常管理", action = "ISOLATE", description = "隔离入库")
    public Result<Map<String, Object>> isolate(
            @PathVariable Long id,
            @RequestBody IsolateDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userName = (String) request.getAttribute("userName");
        dto.setOrderId(id);
        Map<String, Object> data = exceptionService.isolate(dto, userId, userName);
        return Result.success("隔离入库成功", data);
    }

    /**
     * 异常处理（退货/换货/报废/降价销售）
     */
    @PostMapping("/{id}/handle")
    @OperationLog(module = "异常管理", action = "HANDLE", description = "异常处理")
    public Result<Map<String, Object>> handle(
            @PathVariable Long id,
            @RequestBody HandleDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userName = (String) request.getAttribute("userName");
        Map<String, Object> data = exceptionService.handle(id, dto, userId, userName);
        return Result.success("异常处理成功", data);
    }

    /**
     * 取消异常处理单
     */
    @PostMapping("/{id}/cancel")
    @OperationLog(module = "异常管理", action = "CANCEL", description = "取消异常处理单")
    public Result<Map<String, Object>> cancel(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userName = (String) request.getAttribute("userName");
        String cancelReason = body.get("cancelReason");
        Map<String, Object> data = exceptionService.cancel(id, cancelReason, userId, userName);
        return Result.success("异常处理单已取消", data);
    }

    /**
     * 撤销隔离入库
     */
    @PostMapping("/{id}/undo-isolate")
    @OperationLog(module = "异常管理", action = "UNDO_ISOLATE", description = "撤销隔离入库")
    public Result<Map<String, Object>> undoIsolate(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userName = (String) request.getAttribute("userName");
        Map<String, Object> data = exceptionService.undoIsolate(id, userId, userName);
        return Result.success("撤销隔离入库成功", data);
    }

    /**
     * 查询异常统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Map<String, Object> data = exceptionService.getStatistics(startTime, endTime);
        return Result.success(data);
    }

    /**
     * 创建补货入库单
     */
    @PostMapping("/{id}/create-replacement")
    @OperationLog(module = "异常管理", action = "CREATE_REPLACEMENT", description = "创建补货入库单")
    public Result<Map<String, Object>> createReplacementInbound(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userName = (String) request.getAttribute("userName");
        Map<String, Object> data = exceptionService.createReplacementInbound(id, userId, userName);
        return Result.success("补货入库单创建成功", data);
    }
}