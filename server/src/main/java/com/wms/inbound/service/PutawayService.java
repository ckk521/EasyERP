package com.wms.inbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.inbound.dto.PutawayDTO;
import com.wms.inbound.entity.*;
import com.wms.inbound.repository.*;
import com.wms.system.entity.BaseLocation;
import com.wms.system.entity.BaseShelfConfig;
import com.wms.system.repository.BaseLocationRepository;
import com.wms.system.repository.BaseShelfConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 上架服务
 * 阶段五: 上架作业 + 库存更新
 *
 * 支持三种货架类型：
 * 1. 立体货架：任意空闲库位
 * 2. 贯通式货架：推荐最里面的空闲库位（先进后出）
 * 3. 自动化仓库：系统自动分配
 */
@Service
@RequiredArgsConstructor
public class PutawayService {

    private final InboundOrderRepository orderRepository;
    private final InboundOrderItemRepository itemRepository;
    private final PutawayRecordRepository putawayRecordRepository;
    private final InspectRecordRepository inspectRecordRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final BaseLocationRepository locationRepository;
    private final BaseShelfConfigRepository shelfConfigRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 智能库位推荐
     * 根据货架类型推荐不同的库位
     */
    public List<Map<String, Object>> recommendLocations(Long productId, Long warehouseId, Long zoneId) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        // 查询空闲库位
        LambdaQueryWrapper<BaseLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseLocation::getWarehouseId, warehouseId);
        wrapper.eq(BaseLocation::getStatus, 1); // 空闲
        if (zoneId != null) {
            wrapper.eq(BaseLocation::getZoneId, zoneId);
        }
        List<BaseLocation> emptyLocations = locationRepository.selectList(wrapper);

        if (emptyLocations.isEmpty()) {
            return recommendations;
        }

        // 按货架类型分组
        Map<Integer, List<BaseLocation>> byShelfType = emptyLocations.stream()
            .collect(Collectors.groupingBy(l -> l.getShelfType() != null ? l.getShelfType() : 1));

        // 立体货架：按层、列排序，推荐近出库口的库位
        List<BaseLocation> standardLocations = byShelfType.getOrDefault(BaseShelfConfig.SHELF_TYPE_STANDARD, new ArrayList<>());
        for (BaseLocation loc : standardLocations.stream()
            .sorted(Comparator.comparing(BaseLocation::getLayerNum)
                .thenComparing(BaseLocation::getColNum))
            .limit(5)
            .collect(Collectors.toList())) {
            recommendations.add(locationToMap(loc, "立体货架 - 近出库口优先"));
        }

        // 贯通式货架：推荐最里面的空闲库位（深度最大的）
        List<BaseLocation> driveInLocations = byShelfType.getOrDefault(BaseShelfConfig.SHELF_TYPE_DRIVE_IN, new ArrayList<>());
        // 按通道分组，每个通道取深度最大的空闲库位
        Map<Integer, List<BaseLocation>> byAisle = driveInLocations.stream()
            .filter(l -> l.getAisleNum() != null)
            .collect(Collectors.groupingBy(BaseLocation::getAisleNum));

        for (Map.Entry<Integer, List<BaseLocation>> entry : byAisle.entrySet()) {
            BaseLocation deepest = entry.getValue().stream()
                .filter(l -> l.getDepthNum() != null)
                .max(Comparator.comparing(BaseLocation::getDepthNum))
                .orElse(null);
            if (deepest != null) {
                recommendations.add(locationToMap(deepest, "贯通式货架 - 最里侧库位(先进后出)"));
            }
        }

        // 自动化仓库：按层、列排序
        List<BaseLocation> asrsLocations = byShelfType.getOrDefault(BaseShelfConfig.SHELF_TYPE_ASRS, new ArrayList<>());
        for (BaseLocation loc : asrsLocations.stream()
            .sorted(Comparator.comparing(BaseLocation::getLayerNum)
                .thenComparing(BaseLocation::getColNum))
            .limit(5)
            .collect(Collectors.toList())) {
            recommendations.add(locationToMap(loc, "自动化仓库 - 系统推荐"));
        }

        return recommendations;
    }

    /**
     * 库位信息转Map
     */
    private Map<String, Object> locationToMap(BaseLocation loc, String reason) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", loc.getId());
        map.put("code", loc.getCode());
        map.put("shelfType", loc.getShelfType());
        map.put("shelfTypeName", getShelfTypeName(loc.getShelfType()));
        map.put("layerNum", loc.getLayerNum());
        map.put("colNum", loc.getColNum());
        map.put("aisleNum", loc.getAisleNum());
        map.put("depthNum", loc.getDepthNum());
        map.put("reason", reason);
        return map;
    }

    /**
     * 获取货架类型名称
     */
    private String getShelfTypeName(Integer shelfType) {
        if (shelfType == null) return "立体货架";
        switch (shelfType) {
            case BaseShelfConfig.SHELF_TYPE_STANDARD: return "立体货架";
            case BaseShelfConfig.SHELF_TYPE_DRIVE_IN: return "贯通式货架";
            case BaseShelfConfig.SHELF_TYPE_ASRS: return "自动化仓库";
            default: return "未知类型";
        }
    }

    /**
     * 自动分配库位
     * 根据货架类型自动选择最佳库位
     */
    public BaseLocation autoAllocateLocation(Long warehouseId, Long zoneId, Integer shelfType) {
        LambdaQueryWrapper<BaseLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseLocation::getWarehouseId, warehouseId);
        wrapper.eq(BaseLocation::getStatus, 1); // 空闲
        if (zoneId != null) {
            wrapper.eq(BaseLocation::getZoneId, zoneId);
        }
        if (shelfType != null) {
            wrapper.eq(BaseLocation::getShelfType, shelfType);
        }

        List<BaseLocation> emptyLocations = locationRepository.selectList(wrapper);
        if (emptyLocations.isEmpty()) {
            return null;
        }

        int targetType = shelfType != null ? shelfType : BaseShelfConfig.SHELF_TYPE_STANDARD;

        switch (targetType) {
            case BaseShelfConfig.SHELF_TYPE_DRIVE_IN:
                // 贯通式货架：选择最里面的空闲库位
                return emptyLocations.stream()
                    .filter(l -> l.getDepthNum() != null)
                    .max(Comparator.comparing(BaseLocation::getDepthNum))
                    .orElse(emptyLocations.get(0));

            case BaseShelfConfig.SHELF_TYPE_ASRS:
                // 自动化仓库：按层、列排序选择
                return emptyLocations.stream()
                    .min(Comparator.comparing(BaseLocation::getLayerNum)
                        .thenComparing(BaseLocation::getColNum))
                    .orElse(emptyLocations.get(0));

            default:
                // 立体货架：按层、列排序选择
                return emptyLocations.stream()
                    .min(Comparator.comparing(BaseLocation::getLayerNum)
                        .thenComparing(BaseLocation::getColNum))
                    .orElse(emptyLocations.get(0));
        }
    }

    /**
     * 执行上架
     * TC-2.11
     */
    @Transactional
    public void putawayItem(PutawayDTO dto, Long userId, String username) {
        // 参数校验
        validatePutawayDTO(dto);

        // 查询入库单和明细
        InboundOrder order = orderRepository.selectById(dto.getOrderId());
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        InboundOrderItem item = itemRepository.selectById(dto.getItemId());
        if (item == null) {
            throw new RuntimeException("入库明细不存在");
        }

        // 状态校验
        validateItemForPutaway(item, dto.getPutawayQty());

        // 创建上架记录
        createPutawayRecord(order, item, dto, userId, username);

        // 更新库存
        updateInventory(order, item, dto);

        // 创建库存事务
        createInventoryTransaction(order, item, dto, userId);

        // 更新入库明细上架数量
        updateItemPutawayQty(item, dto.getPutawayQty(), userId);

        // 更新入库单状态和进度
        updateOrderAfterPutaway(dto.getOrderId(), order);
    }

    /**
     * 上架参数校验
     */
    private void validatePutawayDTO(PutawayDTO dto) {
        if (dto.getOrderId() == null) {
            throw new IllegalArgumentException("入库单ID不能为空");
        }
        if (dto.getItemId() == null) {
            throw new IllegalArgumentException("入库明细ID不能为空");
        }
        if (dto.getPutawayQty() == null || dto.getPutawayQty() <= 0) {
            throw new IllegalArgumentException("上架数量必须大于0");
        }

        // 如果没有传locationId，通过locationCode查找
        if (dto.getLocationId() == null && dto.getLocationCode() != null) {
            BaseLocation location = locationRepository.selectOne(
                new LambdaQueryWrapper<BaseLocation>()
                    .eq(BaseLocation::getCode, dto.getLocationCode())
            );
            if (location == null) {
                throw new IllegalArgumentException("库位不存在: " + dto.getLocationCode());
            }
            dto.setLocationId(location.getId());
        }

        if (dto.getLocationId() == null) {
            throw new IllegalArgumentException("库位ID不能为空");
        }
    }

    /**
     * 入库明细上架校验
     * 改为从记录表汇总计算
     */
    private void validateItemForPutaway(InboundOrderItem item, Integer putawayQty) {
        // 从记录表汇总计算已上架数量
        Integer alreadyPutaway = putawayRecordRepository.sumPutawayQtyByItemId(item.getId());
        if (alreadyPutaway == null) alreadyPutaway = 0;

        // 从验收记录表汇总计算合格数量
        Integer qualifiedQty = inspectRecordRepository.sumQualifiedQtyByItemId(item.getId());
        if (qualifiedQty == null) qualifiedQty = 0;

        // 必须有合格数量才能上架
        if (qualifiedQty == 0) {
            throw new IllegalStateException("商品未验收合格，不能上架");
        }

        if (alreadyPutaway + putawayQty > qualifiedQty) {
            throw new IllegalArgumentException(
                String.format("上架数量不能超过合格数量(已上架:%d, 合格:%d, 本次:%d)",
                    alreadyPutaway, qualifiedQty, putawayQty));
        }
    }

    /**
     * 创建上架记录
     */
    private void createPutawayRecord(InboundOrder order, InboundOrderItem item,
                                     PutawayDTO dto, Long userId, String username) {
        PutawayRecord record = new PutawayRecord();
        record.setInboundOrderId(order.getId());
        record.setInboundOrderNo(order.getOrderNo());
        record.setInboundItemId(item.getId());
        record.setProductId(item.getProductId());
        record.setSkuCode(item.getSkuCode());
        record.setProductName(item.getProductName());
        record.setBatchNo(item.getBatchNo());
        record.setProductionDate(item.getProductionDate());
        record.setExpiryDate(item.getExpiryDate());
        record.setWarehouseId(order.getWarehouseId());
        record.setLocationId(dto.getLocationId());
        record.setLocationCode(dto.getLocationCode());
        record.setPutawayQty(dto.getPutawayQty());
        record.setIsRecommended(dto.getUseRecommended() != null && dto.getUseRecommended() ? 1 : 0);
        record.setPutawayTime(LocalDateTime.now());
        record.setPutawayUser(userId);
        record.setPutawayUserName(username != null ? username : "操作员");

        putawayRecordRepository.insert(record);

        // 更新库位状态
        updateLocationStatus(dto.getLocationId(), dto.getPutawayQty());
    }

    /**
     * 更新库位状态
     * 贯通式货架需要更新"是否被挡住"状态
     */
    private void updateLocationStatus(Long locationId, Integer qty) {
        BaseLocation location = locationRepository.selectById(locationId);
        if (location == null) return;

        // 更新当前数量
        location.setCurrentQty((location.getCurrentQty() != null ? location.getCurrentQty() : 0) + qty);
        location.setStatus(location.getCurrentQty() > 0 ? 2 : 1); // 2-占用, 1-空闲

        // 贯通式货架：更新同一通道内更外侧库位的"是否被挡住"状态
        if (location.getShelfType() != null && location.getShelfType() == BaseShelfConfig.SHELF_TYPE_DRIVE_IN) {
            updateDriveInBlockedStatus(location);
        }

        locationRepository.updateById(location);
    }

    /**
     * 更新贯通式货架的阻挡状态
     * 当前库位有货后，同一通道内更外侧(depth更小)的库位如果也有货，则被挡住
     */
    private void updateDriveInBlockedStatus(BaseLocation location) {
        if (location.getAisleNum() == null || location.getDepthNum() == null) return;

        // 查询同一通道内更外侧的库位(depth < 当前depth)
        LambdaQueryWrapper<BaseLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseLocation::getZoneId, location.getZoneId());
        wrapper.eq(BaseLocation::getRowNum, location.getRowNum());
        wrapper.eq(BaseLocation::getLayerNum, location.getLayerNum());
        wrapper.eq(BaseLocation::getAisleNum, location.getAisleNum());
        wrapper.lt(BaseLocation::getDepthNum, location.getDepthNum());

        List<BaseLocation> outerLocations = locationRepository.selectList(wrapper);
        for (BaseLocation outer : outerLocations) {
            // 如果外侧库位有货，则标记为被挡住
            if (outer.getCurrentQty() != null && outer.getCurrentQty() > 0) {
                outer.setIsBlocked(1);
                locationRepository.updateById(outer);
            }
        }
    }

    /**
     * 更新库存
     */
    private void updateInventory(InboundOrder order, InboundOrderItem item, PutawayDTO dto) {
        // 查询是否已存在该库位+商品+批次的库存
        Inventory inventory = inventoryRepository.findByLocationProductBatch(
            dto.getLocationId(), item.getProductId(), item.getBatchNo());

        if (inventory == null) {
            // 新增库存记录
            inventory = new Inventory();
            inventory.setWarehouseId(order.getWarehouseId());
            inventory.setWarehouseCode(order.getWarehouseCode());
            inventory.setLocationId(dto.getLocationId());
            inventory.setLocationCode(dto.getLocationCode());
            inventory.setProductId(item.getProductId());
            inventory.setSkuCode(item.getSkuCode());
            inventory.setProductName(item.getProductName());
            inventory.setBatchNo(item.getBatchNo());
            inventory.setProductionDate(item.getProductionDate());
            inventory.setExpiryDate(item.getExpiryDate());
            inventory.setQty(dto.getPutawayQty());
            inventory.setAvailableQty(dto.getPutawayQty());
            inventory.setLockedQty(0);
            inventory.setInboundOrderId(order.getId());
            inventory.setInboundOrderNo(order.getOrderNo());
            inventory.setInboundTime(LocalDateTime.now());
            inventory.setExpiryStatus(calculateExpiryStatus(item.getExpiryDate()));
            inventory.setCreateTime(LocalDateTime.now());
            inventory.setUpdateTime(LocalDateTime.now());
            inventoryRepository.insert(inventory);
        } else {
            // 更新库存数量
            inventory.setQty(inventory.getQty() + dto.getPutawayQty());
            inventory.setAvailableQty(inventory.getAvailableQty() + dto.getPutawayQty());
            inventory.setUpdateTime(LocalDateTime.now());
            inventoryRepository.updateById(inventory);
        }
    }

    /**
     * 计算效期状态
     */
    private int calculateExpiryStatus(LocalDate expiryDate) {
        if (expiryDate == null) {
            return Inventory.EXPIRY_STATUS_NORMAL;
        }

        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today)) {
            return Inventory.EXPIRY_STATUS_EXPIRED;
        }

        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);
        if (daysUntilExpiry <= 30) {
            return Inventory.EXPIRY_STATUS_NEAR;
        } else if (daysUntilExpiry <= 60) {
            return Inventory.EXPIRY_STATUS_WARNING;
        }

        return Inventory.EXPIRY_STATUS_NORMAL;
    }

    /**
     * 创建库存事务
     */
    private void createInventoryTransaction(InboundOrder order, InboundOrderItem item,
                                           PutawayDTO dto, Long userId) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setTransactionNo(generateTransactionNo());
        transaction.setTransactionType(InventoryTransaction.TYPE_INBOUND);
        transaction.setWarehouseId(order.getWarehouseId());
        transaction.setLocationId(dto.getLocationId());
        transaction.setLocationCode(dto.getLocationCode());
        transaction.setProductId(item.getProductId());
        transaction.setSkuCode(item.getSkuCode());
        transaction.setBatchNo(item.getBatchNo());
        transaction.setQtyChange(dto.getPutawayQty());
        transaction.setRefOrderType(InventoryTransaction.REF_TYPE_INBOUND);
        transaction.setRefOrderId(order.getId());
        transaction.setRefOrderNo(order.getOrderNo());
        transaction.setRemark("入库上架");
        transaction.setCreateUser(userId);
        transaction.setCreateTime(LocalDateTime.now());

        transactionRepository.insert(transaction);
    }

    /**
     * 生成事务单号
     * 格式: TX + 年月日 + 4位序号
     */
    private String generateTransactionNo() {
        String datePrefix = LocalDate.now().format(DATE_FORMATTER);
        Integer maxSeq = transactionRepository.getMaxSeqByDate(datePrefix);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;
        return "TX" + datePrefix + String.format("%04d", nextSeq);
    }

    /**
     * 更新入库明细上架数量
     * 改为记录式：不更新 item 的 putawayQty，通过记录表汇总
     */
    private void updateItemPutawayQty(InboundOrderItem item, Integer putawayQty, Long userId) {
        // 从记录表汇总计算已上架数量
        Integer totalPutaway = putawayRecordRepository.sumPutawayQtyByItemId(item.getId());
        if (totalPutaway == null) totalPutaway = 0;

        // 从验收记录表汇总计算合格数量
        Integer qualifiedQty = inspectRecordRepository.sumQualifiedQtyByItemId(item.getId());
        if (qualifiedQty == null) qualifiedQty = 0;

        // 检查是否全部上架
        if (totalPutaway >= qualifiedQty) {
            item.setStatus(InboundOrderItem.STATUS_PUTAWAY);
            itemRepository.updateById(item);
        }
    }

    /**
     * 上架后更新入库单状态和进度
     * 改为从记录表汇总计算
     */
    private void updateOrderAfterPutaway(Long orderId, InboundOrder order) {
        // 从记录表汇总计算总上架数量
        Integer totalPutaway = putawayRecordRepository.sumPutawayQtyByOrderId(orderId);
        if (totalPutaway == null) totalPutaway = 0;

        // 从验收记录表汇总计算总合格数量
        Integer totalQualified = inspectRecordRepository.sumQualifiedQtyByOrderId(orderId);
        if (totalQualified == null) totalQualified = 0;

        // 计算进度
        int progress = totalQualified > 0 ? (totalPutaway * 100 / totalQualified) : 0;

        order.setTotalPutawayQty(totalPutaway);
        order.setProgressPutaway(progress);

        // 检查是否全部上架完成（通过记录表判断）
        LambdaQueryWrapper<InboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrderItem::getOrderId, orderId);
        List<InboundOrderItem> items = itemRepository.selectList(wrapper);

        boolean allPutaway = items.stream()
            .allMatch(item -> {
                Integer putaway = putawayRecordRepository.sumPutawayQtyByItemId(item.getId());
                Integer qualified = inspectRecordRepository.sumQualifiedQtyByItemId(item.getId());
                if (putaway == null) putaway = 0;
                if (qualified == null) qualified = 0;
                return putaway >= qualified;
            });

        if (allPutaway) {
            order.setStatus(InboundOrder.STATUS_COMPLETED);
            order.setCompleteTime(LocalDateTime.now());
        }

        orderRepository.updateById(order);
    }

    /**
     * 查询待上架商品
     * 改为从记录表汇总计算
     */
    public Map<String, Object> listPendingItems(Long orderId) {
        LambdaQueryWrapper<InboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrderItem::getOrderId, orderId);
        List<InboundOrderItem> items = itemRepository.selectList(wrapper);

        // 过滤出有待上架数量的商品
        List<Map<String, Object>> pendingItems = items.stream()
            .map(item -> {
                Integer qualified = inspectRecordRepository.sumQualifiedQtyByItemId(item.getId());
                Integer putaway = putawayRecordRepository.sumPutawayQtyByItemId(item.getId());
                if (qualified == null) qualified = 0;
                if (putaway == null) putaway = 0;
                int pendingQty = qualified - putaway;

                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", item.getId());
                map.put("skuCode", item.getSkuCode());
                map.put("productName", item.getProductName());
                map.put("qualifiedQty", qualified);
                map.put("putawayQty", putaway);
                map.put("pendingQty", pendingQty);
                return map;
            })
            .filter(m -> (int) m.get("pendingQty") > 0)
            .collect(java.util.stream.Collectors.toList());

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("list", pendingItems);
        result.put("total", pendingItems.size());
        return result;
    }

    /**
     * 查询入库明细的上架记录
     */
    public List<PutawayRecord> getPutawayRecords(Long itemId) {
        return putawayRecordRepository.findByItemId(itemId);
    }

    /**
     * 查询入库单的所有上架记录
     */
    public List<PutawayRecord> getOrderPutawayRecords(Long orderId) {
        return putawayRecordRepository.findByOrderId(orderId);
    }

    /**
     * 查询最近的上架记录（全局）
     */
    public List<PutawayRecord> getRecentPutawayRecords(int limit) {
        return putawayRecordRepository.findRecent(limit);
    }
}
