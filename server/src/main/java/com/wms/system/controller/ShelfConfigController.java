package com.wms.system.controller;

import com.wms.system.entity.BaseShelfConfig;
import com.wms.system.service.ShelfConfigService;
import com.wms.system.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 货架配置控制器
 */
@RestController
@RequestMapping("/api/shelf-config")
@RequiredArgsConstructor
public class ShelfConfigController {

    private final ShelfConfigService shelfConfigService;

    /**
     * 创建货架配置
     */
    @PostMapping
    public Result<BaseShelfConfig> create(@RequestBody BaseShelfConfig config) {
        BaseShelfConfig created = shelfConfigService.create(config);
        return Result.success(created);
    }

    /**
     * 批量生成库位
     */
    @PostMapping("/{id}/generate-locations")
    public Result<Map<String, Object>> generateLocations(@PathVariable Long id) {
        Map<String, Object> result = shelfConfigService.generateLocations(id);
        return Result.success(result);
    }

    /**
     * 查询库区的货架配置列表
     */
    @GetMapping("/zone/{zoneId}")
    public Result<List<BaseShelfConfig>> listByZone(@PathVariable Long zoneId) {
        List<BaseShelfConfig> list = shelfConfigService.listByZone(zoneId);
        return Result.success(list);
    }

    /**
     * 查询仓库的货架配置列表
     */
    @GetMapping("/warehouse/{warehouseId}")
    public Result<List<BaseShelfConfig>> listByWarehouse(@PathVariable Long warehouseId) {
        List<BaseShelfConfig> list = shelfConfigService.listByWarehouse(warehouseId);
        return Result.success(list);
    }

    /**
     * 获取货架配置详情
     */
    @GetMapping("/{id}")
    public Result<BaseShelfConfig> getById(@PathVariable Long id) {
        BaseShelfConfig config = shelfConfigService.getById(id);
        return Result.success(config);
    }

    /**
     * 更新货架配置
     */
    @PutMapping("/{id}")
    public Result<BaseShelfConfig> update(@PathVariable Long id, @RequestBody BaseShelfConfig config) {
        config.setId(id);
        BaseShelfConfig updated = shelfConfigService.update(config);
        return Result.success(updated);
    }

    /**
     * 删除货架配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shelfConfigService.delete(id);
        return Result.success(null);
    }
}
