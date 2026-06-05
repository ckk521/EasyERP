package com.wms.outbound.controller;

import com.wms.outbound.dto.*;
import com.wms.outbound.entity.OutboundOrderItem;
import com.wms.outbound.service.PackService;
import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 打包管理控制器
 *
 * 实现功能：
 * 1. 查看待打包任务列表
 * 2. 领取打包任务
 * 3. 系统推荐包装箱型
 * 4. 确认打包
 */
@RestController
@RequestMapping("/api/v1/outbound/pack")
@RequiredArgsConstructor
public class PackController {

    private final PackService packService;

    /**
     * 查看待打包任务列表
     */
    @GetMapping("/pending")
    public Result<List<PackTaskDetailDTO>> getPendingPackTasks(@RequestParam Long warehouseId) {
        List<PackTaskDetailDTO> tasks = packService.getPendingPackTasks(warehouseId);
        return Result.success(tasks);
    }

    /**
     * 领取打包任务
     */
    @PostMapping("/claim")
    @OperationLog(module = "出库管理", action = "CLAIM", description = "领取打包任务")
    public Result<PackTaskDetailDTO> claimPackTask(@RequestBody Map<String, Object> request) {
        String orderNo = (String) request.get("orderNo");
        Long userId = Long.valueOf(request.get("userId").toString());
        String userName = (String) request.get("userName");

        PackTaskDetailDTO result = packService.claimPackTask(orderNo, userId, userName);
        return Result.success("领取成功", result);
    }

    /**
     * 确认打包
     */
    @PostMapping("/confirm")
    @OperationLog(module = "出库管理", action = "CONFIRM", description = "确认打包")
    public Result<PackResultDTO> confirmPack(@Validated @RequestBody PackConfirmDTO dto) {
        PackResultDTO result = packService.confirmPack(dto);
        return Result.success("打包成功", result);
    }

    /**
     * 完成订单打包（整个订单打包完成）
     */
    @PostMapping("/complete")
    @OperationLog(module = "出库管理", action = "COMPLETE", description = "完成订单打包")
    public Result<Map<String, Object>> completeOrderPack(@RequestBody Map<String, Long> request) {
        Long orderId = request.get("orderId");
        Map<String, Object> result = packService.completeOrderPack(orderId);
        return Result.success("订单打包完成", result);
    }

    /**
     * 获取推荐包装箱型
     */
    @PostMapping("/recommend-box")
    public Result<List<PackTaskDetailDTO.RecommendedBoxDTO>> recommendBoxType(
            @RequestBody Map<String, Object> request) {
        // 从请求中获取参数
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) request.get("items");
        Double totalWeight = (Double) request.get("totalWeight");
        Double totalVolume = (Double) request.get("totalVolume");

        // 转换为商品列表（简化处理）
        List<OutboundOrderItem> items = new java.util.ArrayList<>();
        if (itemsData != null) {
            for (Map<String, Object> itemData : itemsData) {
                OutboundOrderItem item = new OutboundOrderItem();
                item.setQty(itemData.get("qty") != null ? Integer.valueOf(itemData.get("qty").toString()) : 0);
                items.add(item);
            }
        }

        java.math.BigDecimal weight = totalWeight != null ?
            new BigDecimal(totalWeight) : BigDecimal.ZERO;
        java.math.BigDecimal volume = totalVolume != null ?
            new BigDecimal(totalVolume) : BigDecimal.ZERO;

        List<PackTaskDetailDTO.RecommendedBoxDTO> recommendations =
            packService.recommendBoxType(items, weight, volume);

        return Result.success(recommendations);
    }
}
