package com.wms.system.controller;

import com.wms.system.common.Result;
import com.wms.system.entity.SysWarehouse;
import com.wms.system.repository.SysWarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/base/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final SysWarehouseRepository warehouseRepository;

    @GetMapping
    public Result<Map<String, Object>> listWarehouses(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer limit) {
        List<SysWarehouse> warehouses = warehouseRepository.selectList(null);
        List<Map<String, Object>> list = warehouses.stream().map(wh -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", wh.getId());
            map.put("code", wh.getCode() != null ? wh.getCode() : "");
            map.put("name", wh.getName() != null ? wh.getName() : "");
            map.put("type", wh.getType() != null ? wh.getType() : 0);
            map.put("status", wh.getStatus() != null ? wh.getStatus() : 0);
            return map;
        }).collect(Collectors.toList());
        return Result.success(Map.of("list", list));
    }
}
