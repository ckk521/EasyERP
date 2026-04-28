package com.wms.system.service;

import com.wms.system.entity.BaseLocation;
import com.wms.system.entity.BaseShelfConfig;
import com.wms.system.entity.BaseZone;
import com.wms.system.repository.BaseLocationRepository;
import com.wms.system.repository.BaseShelfConfigRepository;
import com.wms.system.repository.BaseZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 货架配置服务
 * 支持配置货架参数并批量生成库位
 */
@Service
@RequiredArgsConstructor
public class ShelfConfigService {

    private final BaseShelfConfigRepository shelfConfigRepository;
    private final BaseLocationRepository locationRepository;
    private final BaseZoneRepository zoneRepository;

    /**
     * 创建货架配置
     * 货架类型继承库区的默认货架类型
     */
    @Transactional
    public BaseShelfConfig create(BaseShelfConfig config) {
        // 查询库区信息，获取默认货架类型
        BaseZone zone = zoneRepository.selectById(config.getZoneId());
        if (zone == null) {
            throw new RuntimeException("库区不存在");
        }

        // 货架类型继承库区设置
        if (config.getShelfType() == null) {
            config.setShelfType(zone.getDefaultShelfType() != null ? zone.getDefaultShelfType() : BaseShelfConfig.SHELF_TYPE_STANDARD);
        }

        // 校验货架类型与库区类型一致
        if (zone.getDefaultShelfType() != null && config.getShelfType() != zone.getDefaultShelfType()) {
            throw new RuntimeException("货架类型必须与库区类型一致: " + getShelfTypeName(zone.getDefaultShelfType()));
        }

        // 校验
        validateConfig(config);

        // 生成编码
        if (config.getCode() == null || config.getCode().isEmpty()) {
            config.setCode(generateCode(config.getZoneCode(), config.getRowNum()));
        }

        // 检查排号是否已存在
        if (shelfConfigRepository.existsByZoneIdAndRowNum(config.getZoneId(), config.getRowNum())) {
            throw new RuntimeException("该库区排号已存在: " + config.getRowNum());
        }

        config.setIsGenerated(BaseShelfConfig.GENERATED_NO);
        config.setLocationCount(0);
        shelfConfigRepository.insert(config);
        return config;
    }

    /**
     * 批量生成库位
     * 根据货架类型生成不同结构的库位
     */
    @Transactional
    public Map<String, Object> generateLocations(Long configId) {
        BaseShelfConfig config = shelfConfigRepository.selectById(configId);
        if (config == null) {
            throw new RuntimeException("货架配置不存在");
        }

        if (config.getIsGenerated() == BaseShelfConfig.GENERATED_YES) {
            throw new RuntimeException("该货架已生成库位，请勿重复生成");
        }

        // 查询库区信息
        BaseZone zone = zoneRepository.selectById(config.getZoneId());
        if (zone == null) {
            throw new RuntimeException("库区不存在");
        }

        List<BaseLocation> locations = new ArrayList<>();
        int shelfType = config.getShelfType() != null ? config.getShelfType() : BaseShelfConfig.SHELF_TYPE_STANDARD;

        // 根据货架类型生成不同结构的库位
        switch (shelfType) {
            case BaseShelfConfig.SHELF_TYPE_STANDARD:
                locations = generateStandardLocations(config);
                break;
            case BaseShelfConfig.SHELF_TYPE_DRIVE_IN:
                locations = generateDriveInLocations(config);
                break;
            case BaseShelfConfig.SHELF_TYPE_ASRS:
                locations = generateASRSLocations(config);
                break;
            default:
                locations = generateStandardLocations(config);
        }

        // 批量插入
        for (BaseLocation location : locations) {
            locationRepository.insert(location);
        }

        // 更新货架配置状态
        config.setIsGenerated(BaseShelfConfig.GENERATED_YES);
        config.setLocationCount(locations.size());
        shelfConfigRepository.updateById(config);

        // 更新库区库位数量
        zone.setLocationCount(zone.getLocationCount() + locations.size());
        zoneRepository.updateById(zone);

        Map<String, Object> result = new HashMap<>();
        result.put("configId", configId);
        result.put("generatedCount", locations.size());
        result.put("shelfType", shelfType);
        result.put("shelfTypeName", getShelfTypeName(shelfType));
        return result;
    }

    /**
     * 生成立体货架库位
     * 每个库位独立，可任意存取
     */
    private List<BaseLocation> generateStandardLocations(BaseShelfConfig config) {
        List<BaseLocation> locations = new ArrayList<>();
        int startLayer = config.getStartLayer();
        int endLayer = config.getEndLayer();
        int columnCount = config.getColumnCount();

        for (int layer = startLayer; layer <= endLayer; layer++) {
            for (int col = 1; col <= columnCount; col++) {
                BaseLocation location = createBaseLocation(config, layer, col);
                location.setShelfType(BaseShelfConfig.SHELF_TYPE_STANDARD);
                location.setCode(generateLocationCode(config, layer, col));
                locations.add(location);
            }
        }
        return locations;
    }

    /**
     * 生成贯通式货架库位
     * 有通道和深度概念，同一通道内先进后出
     */
    private List<BaseLocation> generateDriveInLocations(BaseShelfConfig config) {
        List<BaseLocation> locations = new ArrayList<>();
        int startLayer = config.getStartLayer();
        int endLayer = config.getEndLayer();
        int aisleCount = config.getAisleCount() != null ? config.getAisleCount() : 1;
        int depthCount = config.getDepthCount() != null ? config.getDepthCount() : config.getColumnCount();

        for (int layer = startLayer; layer <= endLayer; layer++) {
            for (int aisle = 1; aisle <= aisleCount; aisle++) {
                for (int depth = 1; depth <= depthCount; depth++) {
                    BaseLocation location = createBaseLocation(config, layer, depth);
                    location.setShelfType(BaseShelfConfig.SHELF_TYPE_DRIVE_IN);
                    location.setAisleNum(aisle);
                    location.setDepthNum(depth);
                    location.setIsBlocked(0);
                    location.setCode(generateDriveInLocationCode(config, layer, aisle, depth));
                    locations.add(location);
                }
            }
        }
        return locations;
    }

    /**
     * 生成自动化仓库库位
     * 堆垛机工作范围，系统自动分配
     */
    private List<BaseLocation> generateASRSLocations(BaseShelfConfig config) {
        List<BaseLocation> locations = new ArrayList<>();
        int startLayer = config.getStartLayer();
        int endLayer = config.getEndLayer();
        int columnCount = config.getColumnCount();

        for (int layer = startLayer; layer <= endLayer; layer++) {
            for (int col = 1; col <= columnCount; col++) {
                BaseLocation location = createBaseLocation(config, layer, col);
                location.setShelfType(BaseShelfConfig.SHELF_TYPE_ASRS);
                location.setCode(generateLocationCode(config, layer, col));
                locations.add(location);
            }
        }
        return locations;
    }

    /**
     * 创建基础库位对象
     */
    private BaseLocation createBaseLocation(BaseShelfConfig config, int layer, int col) {
        BaseLocation location = new BaseLocation();
        location.setZoneId(config.getZoneId());
        location.setZoneCode(config.getZoneCode());
        location.setWarehouseId(config.getWarehouseId());
        location.setWarehouseCode(config.getWarehouseCode());
        location.setRowNum(config.getRowNum());
        location.setColNum(col);
        location.setLayerNum(layer);
        location.setType(getLocationType(config.getShelfType()));
        location.setStorageType(config.getStorageType());
        location.setMaxLength(config.getMaxLength());
        location.setMaxWidth(config.getMaxWidth());
        location.setMaxHeight(config.getMaxHeight());
        location.setMaxWeight(config.getMaxWeight());
        location.setStatus(1); // 空闲
        location.setCurrentQty(0);
        return location;
    }

    /**
     * 生成贯通式货架库位编码
     * 格式: 库区编码-排号-层号-通道号-深度号
     */
    private String generateDriveInLocationCode(BaseShelfConfig config, int layer, int aisle, int depth) {
        return String.format("%s-R%02d-L%02d-A%02d-D%02d",
            config.getZoneCode(),
            config.getRowNum(),
            layer,
            aisle,
            depth);
    }

    /**
     * 获取货架类型名称
     */
    private String getShelfTypeName(int shelfType) {
        switch (shelfType) {
            case BaseShelfConfig.SHELF_TYPE_STANDARD: return "立体货架";
            case BaseShelfConfig.SHELF_TYPE_DRIVE_IN: return "贯通式货架";
            case BaseShelfConfig.SHELF_TYPE_ASRS: return "自动化仓库";
            default: return "未知类型";
        }
    }

    /**
     * 查询库区的货架配置列表
     */
    public List<BaseShelfConfig> listByZone(Long zoneId) {
        return shelfConfigRepository.findByZoneId(zoneId);
    }

    /**
     * 查询仓库的货架配置列表
     */
    public List<BaseShelfConfig> listByWarehouse(Long warehouseId) {
        return shelfConfigRepository.findByWarehouseId(warehouseId);
    }

    /**
     * 获取货架配置详情
     */
    public BaseShelfConfig getById(Long id) {
        return shelfConfigRepository.selectById(id);
    }

    /**
     * 更新货架配置（仅限未生成库位的）
     */
    @Transactional
    public BaseShelfConfig update(BaseShelfConfig config) {
        BaseShelfConfig existing = shelfConfigRepository.selectById(config.getId());
        if (existing == null) {
            throw new RuntimeException("货架配置不存在");
        }

        if (existing.getIsGenerated() == BaseShelfConfig.GENERATED_YES) {
            throw new RuntimeException("已生成库位的货架配置不能修改");
        }

        validateConfig(config);
        shelfConfigRepository.updateById(config);
        return config;
    }

    /**
     * 删除货架配置（仅限未生成库位的）
     */
    @Transactional
    public void delete(Long id) {
        BaseShelfConfig config = shelfConfigRepository.selectById(id);
        if (config == null) {
            throw new RuntimeException("货架配置不存在");
        }

        if (config.getIsGenerated() == BaseShelfConfig.GENERATED_YES) {
            throw new RuntimeException("已生成库位的货架配置不能删除，请先删除库位");
        }

        shelfConfigRepository.deleteById(id);
    }

    /**
     * 校验配置参数
     */
    private void validateConfig(BaseShelfConfig config) {
        if (config.getZoneId() == null) {
            throw new RuntimeException("库区ID不能为空");
        }
        if (config.getRowNum() == null || config.getRowNum() < 1) {
            throw new RuntimeException("排号必须大于0");
        }
        if (config.getStartLayer() == null || config.getStartLayer() < 1) {
            throw new RuntimeException("起始层必须大于0");
        }
        if (config.getEndLayer() == null || config.getEndLayer() < config.getStartLayer()) {
            throw new RuntimeException("结束层必须大于等于起始层");
        }
        if (config.getColumnCount() == null || config.getColumnCount() < 1) {
            throw new RuntimeException("每层位置数必须大于0");
        }
    }

    /**
     * 生成货架编码
     */
    private String generateCode(String zoneCode, Integer rowNum) {
        return zoneCode + "-R" + String.format("%02d", rowNum);
    }

    /**
     * 生成库位编码
     * 格式: 库区编码-排号-层号-列号
     */
    private String generateLocationCode(BaseShelfConfig config, int layer, int col) {
        return String.format("%s-R%02d-L%02d-C%02d",
            config.getZoneCode(),
            config.getRowNum(),
            layer,
            col);
    }

    /**
     * 根据货架类型获取库位类型
     */
    private Integer getLocationType(Integer shelfType) {
        // 默认返回标准位
        return 1;
    }
}
