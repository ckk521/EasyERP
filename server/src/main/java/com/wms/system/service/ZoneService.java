package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.ZoneDTO;
import com.wms.system.entity.BaseZone;
import com.wms.system.entity.SysWarehouse;
import com.wms.system.repository.BaseZoneRepository;
import com.wms.system.repository.SysWarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ZoneService {

    private final BaseZoneRepository zoneRepository;
    private final SysWarehouseRepository warehouseRepository;
    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> listZones(PageDTO pageDTO, String keyword, Long warehouseId, Integer type, Integer status) {
        LambdaQueryWrapper<BaseZone> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(BaseZone::getName, keyword)
                   .or().like(BaseZone::getCode, keyword);
        }
        if (warehouseId != null) {
            wrapper.eq(BaseZone::getWarehouseId, warehouseId);
        }
        if (type != null) {
            wrapper.eq(BaseZone::getType, type);
        }
        if (status != null) {
            wrapper.eq(BaseZone::getStatus, status);
        }
        wrapper.orderByDesc(BaseZone::getCreateTime);

        IPage<BaseZone> page = zoneRepository.selectPage(
            new Page<>(pageDTO.getPage(), pageDTO.getLimit()), wrapper);

        List<Map<String, Object>> list = page.getRecords().stream().map(this::toMap).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        return result;
    }

    public Map<String, Object> getZoneById(Long id) {
        BaseZone zone = zoneRepository.selectById(id);
        if (zone == null) {
            throw new RuntimeException("库区不存在");
        }
        return toMap(zone);
    }

    public Long createZone(ZoneDTO dto) {
        SysWarehouse warehouse = warehouseRepository.selectById(dto.getWarehouseId());
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在");
        }

        // 自动生成库区编码：仓库编码-库区类型缩写-序号
        String zoneCode = generateZoneCode(warehouse.getId(), warehouse.getCode(), dto.getType());

        BaseZone zone = new BaseZone();
        zone.setCode(zoneCode);
        zone.setName(dto.getName());
        zone.setWarehouseId(dto.getWarehouseId());
        zone.setWarehouseCode(warehouse.getCode());
        zone.setType(dto.getType());
        zone.setTempRequire(dto.getTempRequire() != null ? dto.getTempRequire() : 1);
        zone.setStorageTypes(dto.getStorageTypes());
        zone.setLocationCount(0);
        zone.setStatus(1);

        // 使用 JdbcTemplate 直接插入
        String sql = "INSERT INTO base_zone (code, name, warehouse_id, warehouse_code, type, temp_require, storage_types, location_count, status, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
            zone.getCode(),
            zone.getName(),
            zone.getWarehouseId(),
            zone.getWarehouseCode(),
            zone.getType(),
            zone.getTempRequire(),
            zone.getStorageTypes(),
            zone.getLocationCount(),
            zone.getStatus(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        // 获取插入的 ID
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id;
    }

    /**
     * 生成库区编码
     * 格式：仓库编码-库区类型缩写-序号
     * 例如：WH-SZ-001-RC-01 (收货区), WH-SZ-001-CC-01 (存储区)
     */
    private String generateZoneCode(Long warehouseId, String warehouseCode, Integer type) {
        // 库区类型缩写
        String typeAbbr = getZoneTypeAbbr(type);

        // 查询该仓库下同类型库区的最大序号
        LambdaQueryWrapper<BaseZone> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseZone::getWarehouseId, warehouseId)
               .likeRight(BaseZone::getCode, warehouseCode + "-" + typeAbbr);

        List<BaseZone> existingZones = zoneRepository.selectList(wrapper);
        int maxSeq = 0;
        for (BaseZone z : existingZones) {
            String code = z.getCode();
            // 尝试从编码中提取序号
            String[] parts = code.split("-");
            if (parts.length >= 3) {
                try {
                    int seq = Integer.parseInt(parts[parts.length - 1]);
                    if (seq > maxSeq) maxSeq = seq;
                } catch (NumberFormatException ignored) {}
            }
        }

        int nextSeq = maxSeq + 1;
        return warehouseCode + "-" + typeAbbr + "-" + String.format("%02d", nextSeq);
    }

    /**
     * 获取库区类型缩写
     */
    private String getZoneTypeAbbr(Integer type) {
        if (type == null) return "ZN";
        switch (type) {
            case 1: return "RC"; // 收货区 Receive
            case 2: return "QC"; // 质检区 Quality Check
            case 3: return "CC"; // 存储区 Common Storage
            case 4: return "PC"; // 拣货区 Pick
            case 5: return "PK"; // 打包区 Pack
            case 6: return "SC"; // 发货区 Ship
            case 7: return "RT"; // 退货区 Return
            case 8: return "DM"; // 残次品区 Damaged
            default: return "ZN";
        }
    }

    public void updateZone(Long id, ZoneDTO dto) {
        BaseZone zone = zoneRepository.selectById(id);
        if (zone == null) {
            throw new RuntimeException("库区不存在");
        }

        zone.setName(dto.getName());
        zone.setType(dto.getType());
        zone.setTempRequire(dto.getTempRequire());
        zone.setStorageTypes(dto.getStorageTypes());

        zoneRepository.updateById(zone);
    }

    public void deleteZone(Long id) {
        zoneRepository.deleteById(id);
    }

    public void enableZone(Long id) {
        BaseZone zone = zoneRepository.selectById(id);
        if (zone == null) {
            throw new RuntimeException("库区不存在");
        }
        zone.setStatus(1);
        zoneRepository.updateById(zone);
    }

    public void disableZone(Long id) {
        BaseZone zone = zoneRepository.selectById(id);
        if (zone == null) {
            throw new RuntimeException("库区不存在");
        }
        zone.setStatus(0);
        zoneRepository.updateById(zone);
    }

    /**
     * 按仓库ID查询库区列表
     */
    public List<BaseZone> listByWarehouseId(Long warehouseId) {
        LambdaQueryWrapper<BaseZone> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseZone::getWarehouseId, warehouseId);
        wrapper.orderByAsc(BaseZone::getType);
        return zoneRepository.selectList(wrapper);
    }

    private Map<String, Object> toMap(BaseZone zone) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", zone.getId());
        map.put("code", zone.getCode());
        map.put("name", zone.getName());
        map.put("warehouseId", zone.getWarehouseId());
        map.put("warehouseCode", zone.getWarehouseCode());
        map.put("type", zone.getType());
        map.put("tempRequire", zone.getTempRequire());
        map.put("storageTypes", zone.getStorageTypes());
        map.put("locationCount", zone.getLocationCount());
        map.put("status", zone.getStatus());
        map.put("createTime", zone.getCreateTime());
        return map;
    }
}
