package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.system.dto.LocationBatchDTO;
import com.wms.system.dto.PageDTO;
import com.wms.system.entity.BaseLocation;
import com.wms.system.entity.BaseZone;
import com.wms.system.repository.BaseLocationRepository;
import com.wms.system.repository.BaseZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final BaseLocationRepository locationRepository;
    private final BaseZoneRepository zoneRepository;

    public Map<String, Object> listLocations(PageDTO pageDTO, String keyword, Long zoneId, Long warehouseId, Integer type, Integer status) {
        LambdaQueryWrapper<BaseLocation> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(BaseLocation::getCode, keyword);
        }
        if (zoneId != null) {
            wrapper.eq(BaseLocation::getZoneId, zoneId);
        }
        if (warehouseId != null) {
            wrapper.eq(BaseLocation::getWarehouseId, warehouseId);
        }
        if (type != null) {
            wrapper.eq(BaseLocation::getType, type);
        }
        if (status != null) {
            wrapper.eq(BaseLocation::getStatus, status);
        }
        wrapper.orderByAsc(BaseLocation::getCode);

        IPage<BaseLocation> page = locationRepository.selectPage(
            new Page<>(pageDTO.getPage(), pageDTO.getLimit()), wrapper);

        List<Map<String, Object>> list = page.getRecords().stream().map(this::toMap).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        return result;
    }

    public Map<String, Object> getLocationById(Long id) {
        BaseLocation location = locationRepository.selectById(id);
        if (location == null) {
            throw new RuntimeException("库位不存在");
        }
        return toMap(location);
    }

    public int batchGenerate(LocationBatchDTO dto) {
        BaseZone zone = zoneRepository.selectById(dto.getZoneId());
        if (zone == null) {
            throw new RuntimeException("库区不存在");
        }

        int count = 0;
        for (int row = dto.getStartRow(); row <= dto.getEndRow(); row++) {
            for (int col = dto.getStartCol(); col <= dto.getEndCol(); col++) {
                for (int layer = dto.getStartLayer(); layer <= dto.getEndLayer(); layer++) {
                    String code = String.format("%s-%02d-%02d-%02d",
                        zone.getCode(), row, col, layer);

                    // 检查是否已存在
                    LambdaQueryWrapper<BaseLocation> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(BaseLocation::getCode, code);
                    if (locationRepository.selectCount(wrapper) > 0) {
                        continue;
                    }

                    BaseLocation location = new BaseLocation();
                    location.setCode(code);
                    location.setZoneId(dto.getZoneId());
                    location.setZoneCode(zone.getCode());
                    location.setWarehouseId(zone.getWarehouseId());
                    location.setWarehouseCode(zone.getWarehouseCode());
                    location.setRowNum(row);
                    location.setColNum(col);
                    location.setLayerNum(layer);
                    location.setType(dto.getType());
                    location.setStorageType(dto.getStorageType() != null ? dto.getStorageType() : 1);
                    location.setMaxLength(dto.getMaxLength());
                    location.setMaxWidth(dto.getMaxWidth());
                    location.setMaxHeight(dto.getMaxHeight());
                    location.setMaxWeight(dto.getMaxWeight());
                    location.setStatus(1); // 空闲
                    location.setCurrentQty(0);

                    locationRepository.insert(location);
                    count++;
                }
            }
        }

        // 更新库区的库位数量
        LambdaQueryWrapper<BaseLocation> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(BaseLocation::getZoneId, dto.getZoneId());
        Long locationCount = locationRepository.selectCount(countWrapper);
        zone.setLocationCount(locationCount.intValue());
        zoneRepository.updateById(zone);

        return count;
    }

    public void updateLocation(Long id, Map<String, Object> updates) {
        BaseLocation location = locationRepository.selectById(id);
        if (location == null) {
            throw new RuntimeException("库位不存在");
        }

        if (updates.containsKey("maxLength")) {
            location.setMaxLength(new java.math.BigDecimal(updates.get("maxLength").toString()));
        }
        if (updates.containsKey("maxWidth")) {
            location.setMaxWidth(new java.math.BigDecimal(updates.get("maxWidth").toString()));
        }
        if (updates.containsKey("maxHeight")) {
            location.setMaxHeight(new java.math.BigDecimal(updates.get("maxHeight").toString()));
        }
        if (updates.containsKey("maxWeight")) {
            location.setMaxWeight(new java.math.BigDecimal(updates.get("maxWeight").toString()));
        }

        locationRepository.updateById(location);
    }

    public void deleteLocation(Long id) {
        locationRepository.deleteById(id);
    }

    public void enableLocation(Long id) {
        BaseLocation location = locationRepository.selectById(id);
        if (location == null) {
            throw new RuntimeException("库位不存在");
        }
        location.setStatus(1);
        locationRepository.updateById(location);
    }

    public void disableLocation(Long id) {
        BaseLocation location = locationRepository.selectById(id);
        if (location == null) {
            throw new RuntimeException("库位不存在");
        }
        location.setStatus(4); // 禁用
        locationRepository.updateById(location);
    }

    private Map<String, Object> toMap(BaseLocation location) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", location.getId());
        map.put("code", location.getCode());
        map.put("zoneId", location.getZoneId());
        map.put("zoneCode", location.getZoneCode());
        map.put("warehouseId", location.getWarehouseId());
        map.put("warehouseCode", location.getWarehouseCode());
        map.put("rowNum", location.getRowNum());
        map.put("colNum", location.getColNum());
        map.put("layerNum", location.getLayerNum());
        map.put("type", location.getType());
        map.put("storageType", location.getStorageType());
        map.put("warehouseId", location.getWarehouseId());
        map.put("maxLength", location.getMaxLength());
        map.put("maxWidth", location.getMaxWidth());
        map.put("maxHeight", location.getMaxHeight());
        map.put("maxWeight", location.getMaxWeight());
        map.put("status", location.getStatus());
        map.put("currentQty", location.getCurrentQty());
        map.put("createTime", location.getCreateTime());
        return map;
    }
}
