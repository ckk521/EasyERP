package com.wms.system.controller;

import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.WarehouseDTO;
import com.wms.system.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/base/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public Result<Map<String, Object>> listWarehouses(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(page);
        pageDTO.setLimit(limit);

        Map<String, Object> data = warehouseService.listWarehouses(pageDTO, keyword, type, status);
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getWarehouse(@PathVariable Long id) {
        return Result.success(warehouseService.getWarehouseById(id));
    }

    @PostMapping
    @OperationLog(module = "仓库管理", action = "CREATE", description = "创建仓库")
    public Result<Map<String, Object>> createWarehouse(@RequestBody WarehouseDTO dto) {
        Long id = warehouseService.createWarehouse(dto);
        return Result.success("仓库创建成功", Map.of("id", id));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "仓库管理", action = "UPDATE", description = "更新仓库")
    public Result<Void> updateWarehouse(@PathVariable Long id, @RequestBody WarehouseDTO dto) {
        warehouseService.updateWarehouse(id, dto);
        return Result.success("仓库信息已更新", null);
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "仓库管理", action = "DELETE", description = "删除仓库")
    public Result<Void> deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return Result.success("仓库已删除", null);
    }

    @PatchMapping("/{id}/enable")
    @OperationLog(module = "仓库管理", action = "ENABLE", description = "启用仓库")
    public Result<Void> enableWarehouse(@PathVariable Long id) {
        warehouseService.enableWarehouse(id);
        return Result.success("仓库已启用", null);
    }

    @PatchMapping("/{id}/disable")
    @OperationLog(module = "仓库管理", action = "DISABLE", description = "停用仓库")
    public Result<Void> disableWarehouse(@PathVariable Long id) {
        warehouseService.disableWarehouse(id);
        return Result.success("仓库已停用", null);
    }

    @PatchMapping("/batch-enable")
    @OperationLog(module = "仓库管理", action = "BATCH_ENABLE", description = "批量启用仓库")
    public Result<Void> batchEnable(@RequestBody Map<String, List<Long>> request) {
        warehouseService.batchEnable(request.get("ids"));
        return Result.success("已成功启用" + request.get("ids").size() + "个仓库", null);
    }

    @PatchMapping("/batch-disable")
    @OperationLog(module = "仓库管理", action = "BATCH_DISABLE", description = "批量停用仓库")
    public Result<Void> batchDisable(@RequestBody Map<String, List<Long>> request) {
        warehouseService.batchDisable(request.get("ids"));
        return Result.success("已成功停用" + request.get("ids").size() + "个仓库", null);
    }

    @DeleteMapping("/batch-delete")
    @OperationLog(module = "仓库管理", action = "BATCH_DELETE", description = "批量删除仓库")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> request) {
        warehouseService.batchDelete(request.get("ids"));
        return Result.success("已成功删除" + request.get("ids").size() + "个仓库", null);
    }
}
