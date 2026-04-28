package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.WarehouseDTO;
import com.wms.system.entity.SysWarehouse;
import com.wms.system.repository.SysWarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final SysWarehouseRepository warehouseRepository;

    public Map<String, Object> listWarehouses(PageDTO pageDTO, String keyword, Integer type, Integer status) {
        LambdaQueryWrapper<SysWarehouse> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysWarehouse::getName, keyword)
                   .or().like(SysWarehouse::getCode, keyword);
        }
        if (type != null) {
            wrapper.eq(SysWarehouse::getType, type);
        }
        if (status != null) {
            wrapper.eq(SysWarehouse::getStatus, status);
        }
        wrapper.orderByDesc(SysWarehouse::getCreateTime);

        IPage<SysWarehouse> page = warehouseRepository.selectPage(
            new Page<>(pageDTO.getPage(), pageDTO.getLimit()), wrapper);

        List<Map<String, Object>> list = page.getRecords().stream().map(this::toMap).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        return result;
    }

    public Map<String, Object> getWarehouseById(Long id) {
        SysWarehouse warehouse = warehouseRepository.selectById(id);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在");
        }
        return toMap(warehouse);
    }

    public Long createWarehouse(WarehouseDTO dto) {
        // 检查编码唯一性
        LambdaQueryWrapper<SysWarehouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysWarehouse::getCode, dto.getCode());
        if (warehouseRepository.selectCount(wrapper) > 0) {
            throw new RuntimeException("仓库编码已存在");
        }

        SysWarehouse warehouse = new SysWarehouse();
        warehouse.setCode(dto.getCode());
        warehouse.setName(dto.getName());
        warehouse.setType(dto.getType());
        warehouse.setStorageTypes(dto.getStorageTypes());
        warehouse.setCountry(dto.getCountry());
        warehouse.setProvince(dto.getProvince());
        warehouse.setAddress(dto.getAddress());
        warehouse.setManager(dto.getManager());
        warehouse.setPhone(dto.getPhone());
        warehouse.setArea(dto.getArea());
        warehouse.setStatus(1);
        warehouse.setCreateTime(LocalDateTime.now());
        warehouse.setUpdateTime(LocalDateTime.now());

        warehouseRepository.insert(warehouse);
        return warehouse.getId();
    }

    public void updateWarehouse(Long id, WarehouseDTO dto) {
        SysWarehouse warehouse = warehouseRepository.selectById(id);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在");
        }

        warehouse.setName(dto.getName());
        warehouse.setType(dto.getType());
        warehouse.setStorageTypes(dto.getStorageTypes());
        warehouse.setCountry(dto.getCountry());
        warehouse.setProvince(dto.getProvince());
        warehouse.setAddress(dto.getAddress());
        warehouse.setManager(dto.getManager());
        warehouse.setPhone(dto.getPhone());
        warehouse.setArea(dto.getArea());
        warehouse.setUpdateTime(LocalDateTime.now());

        warehouseRepository.updateById(warehouse);
    }

    public void deleteWarehouse(Long id) {
        warehouseRepository.deleteById(id);
    }

    public void enableWarehouse(Long id) {
        SysWarehouse warehouse = warehouseRepository.selectById(id);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在");
        }
        warehouse.setStatus(1);
        warehouseRepository.updateById(warehouse);
    }

    public void disableWarehouse(Long id) {
        SysWarehouse warehouse = warehouseRepository.selectById(id);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在");
        }
        warehouse.setStatus(0);
        warehouseRepository.updateById(warehouse);
    }

    public void batchEnable(List<Long> ids) {
        for (Long id : ids) {
            enableWarehouse(id);
        }
    }

    public void batchDisable(List<Long> ids) {
        for (Long id : ids) {
            disableWarehouse(id);
        }
    }

    public void batchDelete(List<Long> ids) {
        warehouseRepository.deleteBatchIds(ids);
    }

    private Map<String, Object> toMap(SysWarehouse warehouse) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", warehouse.getId());
        map.put("code", warehouse.getCode());
        map.put("name", warehouse.getName());
        map.put("type", warehouse.getType());
        map.put("storageTypes", warehouse.getStorageTypes());
        map.put("country", warehouse.getCountry());
        map.put("province", warehouse.getProvince());
        map.put("address", warehouse.getAddress());
        map.put("manager", warehouse.getManager());
        map.put("phone", warehouse.getPhone());
        map.put("area", warehouse.getArea());
        map.put("status", warehouse.getStatus());
        map.put("createTime", warehouse.getCreateTime());
        return map;
    }
}
