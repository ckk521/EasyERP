package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.system.entity.BaseLocation;
import com.wms.system.entity.BaseProduct;
import com.wms.system.entity.BaseZone;
import com.wms.system.entity.SysWarehouse;
import com.wms.system.repository.BaseLocationRepository;
import com.wms.system.repository.BaseProductRepository;
import com.wms.system.repository.BaseZoneRepository;
import com.wms.system.repository.SysWarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 存储属性校验服务
 * 用于上架时校验商品与库位、库区、仓库的存储属性匹配
 */
@Service
@RequiredArgsConstructor
public class StorageValidationService {

    private final BaseProductRepository productRepository;
    private final BaseLocationRepository locationRepository;
    private final BaseZoneRepository zoneRepository;
    private final SysWarehouseRepository warehouseRepository;

    /**
     * 校验商品是否可以上架到指定库位
     * @param skuCode 商品SKU编码
     * @param locationCode 库位编码
     * @return 校验结果，null表示通过，否则返回错误信息
     */
    public String validatePutaway(String skuCode, String locationCode) {
        // 1. 获取商品信息
        BaseProduct product = productRepository.selectOne(
            new LambdaQueryWrapper<BaseProduct>()
                .eq(BaseProduct::getSkuCode, skuCode)
        );
        if (product == null) {
            return "商品不存在: " + skuCode;
        }

        // 2. 获取库位信息
        BaseLocation location = locationRepository.selectOne(
            new LambdaQueryWrapper<BaseLocation>()
                .eq(BaseLocation::getCode, locationCode)
        );
        if (location == null) {
            return "库位不存在: " + locationCode;
        }

        // 3. 获取库区信息
        BaseZone zone = zoneRepository.selectById(location.getZoneId());
        if (zone == null) {
            return "库区不存在";
        }

        // 4. 获取仓库信息
        SysWarehouse warehouse = warehouseRepository.selectById(location.getWarehouseId());
        if (warehouse == null) {
            return "仓库不存在";
        }

        // 5. 校验商品存储类型是否在仓库支持的范围内
        Integer productStorageType = product.getStorageCond();
        String warehouseStorageTypes = warehouse.getStorageTypes();

        if (warehouseStorageTypes != null && !warehouseStorageTypes.isEmpty()) {
            List<String> allowedTypes = Arrays.asList(warehouseStorageTypes.split(","));
            if (!allowedTypes.contains(String.valueOf(productStorageType))) {
                return String.format("商品存储类型(%s)不在仓库支持的存储类型范围内(%s)",
                    getStorageTypeName(productStorageType), warehouseStorageTypes);
            }
        }

        // 6. 校验商品存储类型是否在库区支持的范围内
        String zoneStorageTypes = zone.getStorageTypes();
        if (zoneStorageTypes != null && !zoneStorageTypes.isEmpty()) {
            List<String> allowedTypes = Arrays.asList(zoneStorageTypes.split(","));
            if (!allowedTypes.contains(String.valueOf(productStorageType))) {
                return String.format("商品存储类型(%s)不在库区支持的存储类型范围内(%s)",
                    getStorageTypeName(productStorageType), zoneStorageTypes);
            }
        }

        // 7. 校验库位存储类型是否与商品匹配
        Integer locationStorageType = location.getStorageType();
        if (locationStorageType != null && !locationStorageType.equals(productStorageType)) {
            return String.format("库位存储类型(%s)与商品存储类型(%s)不匹配",
                getStorageTypeName(locationStorageType), getStorageTypeName(productStorageType));
        }

        // 8. 校验库位状态
        if (location.getStatus() == 4) {
            return "库位已禁用，无法上架";
        }
        if (location.getStatus() == 3) {
            return "库位已锁定，无法上架";
        }

        return null; // 校验通过
    }

    /**
     * 获取存储类型名称
     */
    public static String getStorageTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "常温";
            case 2: return "冷藏";
            case 3: return "冷冻";
            case 4: return "恒温";
            default: return "未知(" + type + ")";
        }
    }

    /**
     * 校验库位存储类型是否在库区和仓库支持的范围内
     */
    public String validateLocationStorageType(Long locationId) {
        BaseLocation location = locationRepository.selectById(locationId);
        if (location == null) {
            return "库位不存在";
        }

        BaseZone zone = zoneRepository.selectById(location.getZoneId());
        if (zone == null) {
            return "库区不存在";
        }

        SysWarehouse warehouse = warehouseRepository.selectById(location.getWarehouseId());
        if (warehouse == null) {
            return "仓库不存在";
        }

        Integer locationStorageType = location.getStorageType();
        if (locationStorageType == null) {
            return null; // 未设置存储类型，跳过校验
        }

        // 校验是否在库区范围内
        String zoneStorageTypes = zone.getStorageTypes();
        if (zoneStorageTypes != null && !zoneStorageTypes.isEmpty()) {
            List<String> allowedTypes = Arrays.asList(zoneStorageTypes.split(","));
            if (!allowedTypes.contains(String.valueOf(locationStorageType))) {
                return String.format("库位存储类型(%s)不在库区支持的存储类型范围内(%s)",
                    getStorageTypeName(locationStorageType), zoneStorageTypes);
            }
        }

        // 校验是否在仓库范围内
        String warehouseStorageTypes = warehouse.getStorageTypes();
        if (warehouseStorageTypes != null && !warehouseStorageTypes.isEmpty()) {
            List<String> allowedTypes = Arrays.asList(warehouseStorageTypes.split(","));
            if (!allowedTypes.contains(String.valueOf(locationStorageType))) {
                return String.format("库位存储类型(%s)不在仓库支持的存储类型范围内(%s)",
                    getStorageTypeName(locationStorageType), warehouseStorageTypes);
            }
        }

        return null;
    }

    /**
     * 根据商品存储类型推荐可用库位
     * @param skuCode 商品SKU编码
     * @param warehouseId 仓库ID（可选）
     * @return 推荐的库位列表
     */
    public List<BaseLocation> recommendLocations(String skuCode, Long warehouseId) {
        // 获取商品信息
        BaseProduct product = productRepository.selectOne(
            new LambdaQueryWrapper<BaseProduct>()
                .eq(BaseProduct::getSkuCode, skuCode)
        );
        if (product == null) {
            throw new RuntimeException("商品不存在: " + skuCode);
        }

        Integer productStorageType = product.getStorageCond();

        // 查询匹配的库位
        LambdaQueryWrapper<BaseLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseLocation::getStatus, 1) // 空闲状态
               .eq(BaseLocation::getStorageType, productStorageType); // 存储类型匹配

        if (warehouseId != null) {
            wrapper.eq(BaseLocation::getWarehouseId, warehouseId);
        }

        wrapper.orderByAsc(BaseLocation::getCode).last("LIMIT 10");

        return locationRepository.selectList(wrapper);
    }
}
