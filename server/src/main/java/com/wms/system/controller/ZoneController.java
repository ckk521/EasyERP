package com.wms.system.controller;

import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.ZoneDTO;
import com.wms.system.service.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/base/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    @GetMapping
    public Result<Map<String, Object>> listZones(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(page);
        pageDTO.setLimit(limit);

        Map<String, Object> data = zoneService.listZones(pageDTO, keyword, warehouseId, type, status);
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getZone(@PathVariable Long id) {
        return Result.success(zoneService.getZoneById(id));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public Result<java.util.List<com.wms.system.entity.BaseZone>> listByWarehouse(@PathVariable Long warehouseId) {
        return Result.success(zoneService.listByWarehouseId(warehouseId));
    }

    @PostMapping
    @OperationLog(module = "库区管理", action = "CREATE", description = "创建库区")
    public Result<Map<String, Object>> createZone(@RequestBody ZoneDTO dto) {
        Long id = zoneService.createZone(dto);
        return Result.success("库区创建成功", Map.of("id", id));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "库区管理", action = "UPDATE", description = "更新库区")
    public Result<Void> updateZone(@PathVariable Long id, @RequestBody ZoneDTO dto) {
        zoneService.updateZone(id, dto);
        return Result.success("库区信息已更新", null);
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "库区管理", action = "DELETE", description = "删除库区")
    public Result<Void> deleteZone(@PathVariable Long id) {
        zoneService.deleteZone(id);
        return Result.success("库区已删除", null);
    }

    @PatchMapping("/{id}/enable")
    @OperationLog(module = "库区管理", action = "ENABLE", description = "启用库区")
    public Result<Void> enableZone(@PathVariable Long id) {
        zoneService.enableZone(id);
        return Result.success("库区已启用", null);
    }

    @PatchMapping("/{id}/disable")
    @OperationLog(module = "库区管理", action = "DISABLE", description = "停用库区")
    public Result<Void> disableZone(@PathVariable Long id) {
        zoneService.disableZone(id);
        return Result.success("库区已停用", null);
    }
}
