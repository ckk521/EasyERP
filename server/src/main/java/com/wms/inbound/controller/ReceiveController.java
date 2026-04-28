package com.wms.inbound.controller;

import com.wms.inbound.dto.ReceiveDTO;
import com.wms.inbound.entity.InboundOrder;
import com.wms.inbound.entity.InboundOrderItem;
import com.wms.inbound.entity.ReceiveRecord;
import com.wms.inbound.service.ReceiveService;
import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import com.wms.system.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收货作业控制器
 * 阶段三: 收货作业 + 差异处理
 */
@RestController
@RequestMapping("/api/v1/inbound/receive")
@RequiredArgsConstructor
public class ReceiveController {

    private final ReceiveService receiveService;

    /**
     * 扫描入库单号定位单据
     * TC-2.5.1
     */
    @GetMapping("/order/{orderNo}")
    @OperationLog(module = "入库管理", action = "SCAN", description = "扫描入库单号")
    public Result<Map<String, Object>> findOrderByNo(@PathVariable String orderNo) {
        InboundOrder order = receiveService.findOrderByNo(orderNo);

        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("deliveryBatchNo", order.getDeliveryBatchNo());
        result.put("orderType", order.getOrderType());
        result.put("status", order.getStatus());
        result.put("supplierName", order.getSupplierName());
        result.put("warehouseName", order.getWarehouseName());

        return Result.success(result);
    }

    /**
     * 扫描商品条码识别商品
     * TC-2.5.2
     */
    @GetMapping("/item")
    @OperationLog(module = "入库管理", action = "SCAN", description = "扫描商品条码")
    public Result<Map<String, Object>> findItem(
            @RequestParam Long orderId,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String barcode) {

        InboundOrderItem item = null;
        if (skuCode != null) {
            item = receiveService.findItemBySkuCode(orderId, skuCode);
        } else if (barcode != null) {
            item = receiveService.findItemByBarcode(orderId, barcode);
        }

        if (item == null) {
            return Result.error(404, "商品不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", item.getId());
        result.put("skuCode", item.getSkuCode());
        result.put("productName", item.getProductName());
        result.put("expectedQty", item.getExpectedQty());
        result.put("receivedQty", item.getReceivedQty());
        result.put("status", item.getStatus());

        return Result.success(result);
    }

    /**
     * 执行收货
     * TC-2.5.3, TC-2.6
     */
    @PostMapping("/execute")
    @OperationLog(module = "入库管理", action = "RECEIVE", description = "执行收货")
    public Result<Void> receiveItem(@RequestBody ReceiveDTO dto) {
        Long userId = UserContext.requireCurrentUserId();
        String username = UserContext.getCurrentUsername();
        receiveService.receiveItem(dto, userId, username);
        return Result.success("收货成功", null);
    }

    /**
     * 查询待收货的入库单列表
     */
    @GetMapping("/pending")
    public Result<Map<String, Object>> listPendingOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        // 返回待收货和收货中状态的入库单
        Map<String, Object> data = receiveService.listPendingOrders(page, limit);
        return Result.success(data);
    }

    /**
     * 查询入库明细的收货记录
     */
    @GetMapping("/records/item/{itemId}")
    public Result<List<ReceiveRecord>> getItemReceiveRecords(@PathVariable Long itemId) {
        List<ReceiveRecord> records = receiveService.getReceiveRecords(itemId);
        return Result.success(records);
    }

    /**
     * 查询入库单的所有收货记录
     */
    @GetMapping("/records/order/{orderId}")
    public Result<List<ReceiveRecord>> getOrderReceiveRecords(@PathVariable Long orderId) {
        List<ReceiveRecord> records = receiveService.getOrderReceiveRecords(orderId);
        return Result.success(records);
    }

    /**
     * 查询最近的收货记录（全局）
     */
    @GetMapping("/records/recent")
    public Result<Map<String, Object>> getRecentReceiveRecords(
            @RequestParam(defaultValue = "20") int limit) {
        List<ReceiveRecord> records = receiveService.getRecentReceiveRecords(limit);
        Map<String, Object> result = new HashMap<>();
        result.put("list", records);
        return Result.success(result);
    }
}
