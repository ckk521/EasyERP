package com.wms.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.inbound.entity.Inventory;
import com.wms.inbound.entity.InspectRecord;
import com.wms.inventory.dto.*;
import com.wms.inventory.repository.InventoryRepositoryExt;
import com.wms.inventory.service.InventoryService;
import com.wms.system.entity.BaseLocation;
import com.wms.system.entity.BaseProduct;
import com.wms.system.entity.SysWarehouse;
import com.wms.system.entity.BaseZone;
import com.wms.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存服务实现
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepositoryExt inventoryRepository;
    private final BaseProductRepository productRepository;
    private final SysWarehouseRepository warehouseRepository;
    private final BaseZoneRepository zoneRepository;
    private final BaseLocationRepository locationRepository;

    // 默认效期预警天数
    private static final int DEFAULT_EXPIRY_WARNING_DAYS = 15;

    @Override
    public Map<String, Object> queryInventory(InventoryQueryDTO query) {
        // 构建查询条件
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();

        // SKU模糊查询
        if (query.getSkuCode() != null && !query.getSkuCode().isEmpty()) {
            wrapper.like(Inventory::getSkuCode, query.getSkuCode());
        }

        // 商品名称模糊查询
        if (query.getProductName() != null && !query.getProductName().isEmpty()) {
            wrapper.like(Inventory::getProductName, query.getProductName());
        }

        // 仓库筛选
        if (query.getWarehouseId() != null) {
            wrapper.eq(Inventory::getWarehouseId, query.getWarehouseId());
        }

        // 库位筛选
        if (query.getLocationId() != null) {
            wrapper.eq(Inventory::getLocationId, query.getLocationId());
        }

        // 批次号筛选
        if (query.getBatchNo() != null && !query.getBatchNo().isEmpty()) {
            wrapper.eq(Inventory::getBatchNo, query.getBatchNo());
        }

        // 效期状态筛选
        if (query.getExpiryStatus() != null) {
            wrapper.eq(Inventory::getExpiryStatus, query.getExpiryStatus());
        }

        // 条码精确查询
        if (query.getBarcode() != null && !query.getBarcode().isEmpty()) {
            // 先查商品获取ID
            BaseProduct product = productRepository.findByBarcode(query.getBarcode());
            if (product != null) {
                wrapper.eq(Inventory::getProductId, product.getId());
            } else {
                // 没找到商品，返回空结果
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("list", Collections.emptyList());
                emptyResult.put("total", 0);
                emptyResult.put("page", query.getPage());
                emptyResult.put("limit", query.getLimit());
                return emptyResult;
            }
        }

        // 按更新时间倒序
        wrapper.orderByDesc(Inventory::getUpdateTime);

        // 分页查询
        Page<Inventory> page = new Page<>(query.getPage(), query.getLimit());
        IPage<Inventory> result = inventoryRepository.selectPage(page, wrapper);

        // 转换为VO
        List<InventoryVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("list", voList);
        response.put("total", result.getTotal());
        response.put("page", query.getPage());
        response.put("limit", query.getLimit());
        return response;
    }

    @Override
    public List<InventoryVO> getProductInventoryDetail(Long productId, Long warehouseId) {
        List<Inventory> inventories;

        if (warehouseId != null) {
            inventories = inventoryRepository.findByProductIdAndWarehouse(productId, warehouseId);
        } else {
            inventories = inventoryRepository.findByProductId(productId);
        }

        return inventories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BatchInventoryVO> queryBatchInventory(InventoryQueryDTO query) {
        // 构建查询条件
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();

        if (query.getBatchNo() != null && !query.getBatchNo().isEmpty()) {
            wrapper.eq(Inventory::getBatchNo, query.getBatchNo());
        }

        if (query.getWarehouseId() != null) {
            wrapper.eq(Inventory::getWarehouseId, query.getWarehouseId());
        }

        // 按过期日期升序（先过期在前）
        wrapper.orderByAsc(Inventory::getExpiryDate);

        List<Inventory> inventories = inventoryRepository.selectList(wrapper);

        // 按批次分组汇总
        Map<String, List<Inventory>> batchMap = inventories.stream()
                .collect(Collectors.groupingBy(Inventory::getBatchNo));

        return batchMap.values().stream()
                .map(this::convertToBatchVO)
                .sorted(Comparator.comparing(BatchInventoryVO::getExpiryDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpiryWarningVO> getExpiryWarnings(Long warehouseId, Integer expiryStatus) {
        List<Inventory> inventories;

        if (expiryStatus != null) {
            inventories = inventoryRepository.findByExpiryStatus(warehouseId, expiryStatus);
        } else {
            inventories = inventoryRepository.findExpiryWarnings(warehouseId);
        }

        return inventories.stream()
                .map(this::convertToExpiryWarningVO)
                .sorted(Comparator.comparing(ExpiryWarningVO::getExpiryStatus).reversed()
                        .thenComparing(ExpiryWarningVO::getRemainingDays))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateExpiryStatus() {
        List<Inventory> inventories = inventoryRepository.findAllWithExpiryDate();
        LocalDate today = LocalDate.now();

        for (Inventory inventory : inventories) {
            if (inventory.getExpiryDate() == null) {
                continue;
            }

            // 获取商品的预警天数
            BaseProduct product = productRepository.selectById(inventory.getProductId());
            int warningDays = (product != null && product.getExpiryWarning() != null)
                    ? product.getExpiryWarning()
                    : DEFAULT_EXPIRY_WARNING_DAYS;

            // 计算效期状态
            int expiryStatus = calculateExpiryStatus(inventory.getExpiryDate(), warningDays);

            // 更新状态
            if (!Objects.equals(inventory.getExpiryStatus(), expiryStatus)) {
                inventory.setExpiryStatus(expiryStatus);
                inventoryRepository.updateById(inventory);
            }
        }
    }

    @Override
    public Map<String, Object> getInventorySummary(Long warehouseId) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        if (warehouseId != null) {
            wrapper.eq(Inventory::getWarehouseId, warehouseId);
        }

        List<Inventory> inventories = inventoryRepository.selectList(wrapper);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSku", inventories.stream().map(Inventory::getProductId).distinct().count());
        summary.put("totalQty", inventories.stream().mapToInt(i -> i.getQty() != null ? i.getQty() : 0).sum());
        summary.put("totalAvailableQty", inventories.stream().mapToInt(i -> i.getAvailableQty() != null ? i.getAvailableQty() : 0).sum());
        summary.put("totalLockedQty", inventories.stream().mapToInt(i -> i.getLockedQty() != null ? i.getLockedQty() : 0).sum());

        // 效期统计
        summary.put("normalCount", inventories.stream().filter(i -> i.getExpiryStatus() == null || i.getExpiryStatus() == 0).count());
        summary.put("warningCount", inventories.stream().filter(i -> i.getExpiryStatus() != null && i.getExpiryStatus() == 1).count());
        summary.put("nearExpiryCount", inventories.stream().filter(i -> i.getExpiryStatus() != null && i.getExpiryStatus() == 2).count());
        summary.put("expiredCount", inventories.stream().filter(i -> i.getExpiryStatus() != null && i.getExpiryStatus() == 3).count());

        return summary;
    }

    // ========== 私有方法 ==========

    private InventoryVO convertToVO(Inventory inventory) {
        InventoryVO vo = new InventoryVO();
        vo.setId(inventory.getId());
        vo.setWarehouseId(inventory.getWarehouseId());
        vo.setWarehouseCode(inventory.getWarehouseCode());
        vo.setLocationId(inventory.getLocationId());
        vo.setLocationCode(inventory.getLocationCode());
        vo.setProductId(inventory.getProductId());
        vo.setSkuCode(inventory.getSkuCode());
        vo.setProductName(inventory.getProductName());
        vo.setBatchNo(inventory.getBatchNo());
        vo.setProductionDate(inventory.getProductionDate());
        vo.setExpiryDate(inventory.getExpiryDate());
        vo.setQty(inventory.getQty());
        vo.setAvailableQty(inventory.getAvailableQty());
        vo.setLockedQty(inventory.getLockedQty());
        vo.setExpiryStatus(inventory.getExpiryStatus());
        vo.setInboundTime(inventory.getInboundTime());
        vo.setInboundOrderNo(inventory.getInboundOrderNo());
        vo.setUpdateTime(inventory.getUpdateTime());

        // 设置效期状态名称
        vo.setExpiryStatusName(getExpiryStatusName(inventory.getExpiryStatus()));

        // 计算剩余天数
        if (inventory.getExpiryDate() != null) {
            vo.setRemainingDays((int) ChronoUnit.DAYS.between(LocalDate.now(), inventory.getExpiryDate()));
        }

        // 获取仓库名称
        if (inventory.getWarehouseId() != null) {
            SysWarehouse warehouse = warehouseRepository.selectById(inventory.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
            }
        }

        // 获取库位信息
        if (inventory.getLocationId() != null) {
            BaseLocation location = locationRepository.selectById(inventory.getLocationId());
            if (location != null) {
                vo.setZoneId(location.getZoneId());
                vo.setZoneCode(location.getZoneCode());
                // 获取库区名称
                if (location.getZoneId() != null) {
                    BaseZone zone = zoneRepository.selectById(location.getZoneId());
                    if (zone != null) {
                        vo.setZoneName(zone.getName());
                    }
                }
            }
        }

        // 获取条码
        if (inventory.getProductId() != null) {
            BaseProduct product = productRepository.selectById(inventory.getProductId());
            if (product != null) {
                vo.setBarcode(product.getBarcode());
            }
        }

        return vo;
    }

    private BatchInventoryVO convertToBatchVO(List<Inventory> inventories) {
        if (inventories.isEmpty()) {
            return null;
        }

        Inventory first = inventories.get(0);
        BatchInventoryVO vo = new BatchInventoryVO();
        vo.setBatchNo(first.getBatchNo());
        vo.setProductId(first.getProductId());
        vo.setSkuCode(first.getSkuCode());
        vo.setProductName(first.getProductName());
        vo.setProductionDate(first.getProductionDate());
        vo.setExpiryDate(first.getExpiryDate());
        vo.setExpiryStatus(first.getExpiryStatus());
        vo.setExpiryStatusName(getExpiryStatusName(first.getExpiryStatus()));
        vo.setInboundOrderNo(first.getInboundOrderNo());

        // 汇总数量
        vo.setTotalQty(inventories.stream().mapToInt(i -> i.getQty() != null ? i.getQty() : 0).sum());
        vo.setTotalAvailableQty(inventories.stream().mapToInt(i -> i.getAvailableQty() != null ? i.getAvailableQty() : 0).sum());
        vo.setTotalLockedQty(inventories.stream().mapToInt(i -> i.getLockedQty() != null ? i.getLockedQty() : 0).sum());
        vo.setLocationCount(inventories.size());

        // 计算剩余天数
        if (first.getExpiryDate() != null) {
            vo.setRemainingDays((int) ChronoUnit.DAYS.between(LocalDate.now(), first.getExpiryDate()));
        }

        // 最早入库时间
        vo.setEarliestInboundTime(inventories.stream()
                .filter(i -> i.getInboundTime() != null)
                .map(Inventory::getInboundTime)
                .min(Comparator.naturalOrder())
                .orElse(null));

        return vo;
    }

    private ExpiryWarningVO convertToExpiryWarningVO(Inventory inventory) {
        ExpiryWarningVO vo = new ExpiryWarningVO();
        vo.setId(inventory.getId());
        vo.setProductId(inventory.getProductId());
        vo.setSkuCode(inventory.getSkuCode());
        vo.setProductName(inventory.getProductName());
        vo.setBatchNo(inventory.getBatchNo());
        vo.setLocationCode(inventory.getLocationCode());
        vo.setProductionDate(inventory.getProductionDate());
        vo.setExpiryDate(inventory.getExpiryDate());
        vo.setExpiryStatus(inventory.getExpiryStatus());
        vo.setExpiryStatusName(getExpiryStatusName(inventory.getExpiryStatus()));
        vo.setQty(inventory.getQty());
        vo.setInboundOrderNo(inventory.getInboundOrderNo());
        vo.setInboundTime(inventory.getInboundTime());

        // 计算剩余天数
        if (inventory.getExpiryDate() != null) {
            vo.setRemainingDays((int) ChronoUnit.DAYS.between(LocalDate.now(), inventory.getExpiryDate()));
        }

        // 设置预警级别
        if (inventory.getExpiryStatus() != null) {
            switch (inventory.getExpiryStatus()) {
                case 3: // 已过期
                    vo.setWarningLevel("紧急");
                    break;
                case 2: // 临期
                    vo.setWarningLevel("紧急");
                    break;
                case 1: // 预警
                    vo.setWarningLevel("重要");
                    break;
                default:
                    vo.setWarningLevel("一般");
            }
        }

        return vo;
    }

    private String getExpiryStatusName(Integer status) {
        if (status == null) {
            return "正常";
        }
        switch (status) {
            case 1: return "效期预警";
            case 2: return "临期";
            case 3: return "已过期";
            default: return "正常";
        }
    }

    private int calculateExpiryStatus(LocalDate expiryDate, int warningDays) {
        if (expiryDate == null) {
            return 0;
        }

        long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

        if (remainingDays < 0) {
            return 3; // 已过期
        } else if (remainingDays <= warningDays / 2) {
            return 2; // 临期
        } else if (remainingDays <= warningDays) {
            return 1; // 预警
        } else {
            return 0; // 正常
        }
    }

    @Override
    @Transactional
    public int fixInventoryBatchNo() {
        // 查询所有没有批次号的库存记录
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Inventory::getBatchNo)
               .or()
               .eq(Inventory::getBatchNo, "");
        List<Inventory> inventories = inventoryRepository.selectList(wrapper);

        int fixedCount = 0;
        for (Inventory inv : inventories) {
            // 通过入库单号和商品ID查找验收记录
            if (inv.getInboundOrderNo() != null && inv.getProductId() != null) {
                // 查找对应的验收记录获取批次号
                InspectRecord inspectRecord = inventoryRepository.findInspectRecordByOrderNoAndProductId(
                    inv.getInboundOrderNo(), inv.getProductId());
                if (inspectRecord != null && inspectRecord.getBatchNo() != null) {
                    String batchNo = inspectRecord.getBatchNo();

                    // 检查是否已存在相同库位+商品+批次的库存
                    Inventory existing = inventoryRepository.findByLocationProductBatch(
                        inv.getLocationId(), inv.getProductId(), batchNo);

                    if (existing != null && !existing.getId().equals(inv.getId())) {
                        // 合并到已有记录：增加数量，删除当前记录
                        existing.setQty(existing.getQty() + inv.getQty());
                        existing.setAvailableQty(existing.getAvailableQty() + inv.getAvailableQty());
                        existing.setUpdateTime(LocalDateTime.now());
                        inventoryRepository.updateById(existing);
                        inventoryRepository.deleteById(inv.getId());
                    } else {
                        // 直接更新当前记录
                        inv.setBatchNo(batchNo);
                        inv.setProductionDate(inspectRecord.getProductionDate());
                        inv.setExpiryDate(inspectRecord.getExpiryDate());
                        inv.setUpdateTime(LocalDateTime.now());
                        inventoryRepository.updateById(inv);
                    }
                    fixedCount++;
                }
            }
        }
        return fixedCount;
    }
}
