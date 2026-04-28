package com.wms.system.controller;

import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import com.wms.system.dto.LocationBatchDTO;
import com.wms.system.dto.PageDTO;
import com.wms.system.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/base/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public Result<Map<String, Object>> listLocations(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(page);
        pageDTO.setLimit(limit);

        Map<String, Object> data = locationService.listLocations(pageDTO, keyword, zoneId, warehouseId, type, status);
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getLocation(@PathVariable Long id) {
        return Result.success(locationService.getLocationById(id));
    }

    @PostMapping("/batch")
    @OperationLog(module = "库位管理", action = "BATCH_CREATE", description = "批量生成库位")
    public Result<Map<String, Object>> batchGenerate(@RequestBody LocationBatchDTO dto) {
        int count = locationService.batchGenerate(dto);
        return Result.success("已生成" + count + "个库位", Map.of("count", count));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "库位管理", action = "UPDATE", description = "更新库位")
    public Result<Void> updateLocation(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        locationService.updateLocation(id, updates);
        return Result.success("库位信息已更新", null);
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "库位管理", action = "DELETE", description = "删除库位")
    public Result<Void> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return Result.success("库位已删除", null);
    }

    @PatchMapping("/{id}/enable")
    @OperationLog(module = "库位管理", action = "ENABLE", description = "启用库位")
    public Result<Void> enableLocation(@PathVariable Long id) {
        locationService.enableLocation(id);
        return Result.success("库位已启用", null);
    }

    @PatchMapping("/{id}/disable")
    @OperationLog(module = "库位管理", action = "DISABLE", description = "禁用库位")
    public Result<Void> disableLocation(@PathVariable Long id) {
        locationService.disableLocation(id);
        return Result.success("库位已禁用", null);
    }
}
