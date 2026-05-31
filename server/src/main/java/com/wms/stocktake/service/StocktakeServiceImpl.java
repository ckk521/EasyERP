package com.wms.stocktake.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.inbound.entity.Inventory;
import com.wms.inventory.repository.InventoryRepositoryExt;
import com.wms.stocktake.dto.*;
import com.wms.stocktake.entity.StocktakeApproveRecord;
import com.wms.stocktake.entity.StocktakeItem;
import com.wms.stocktake.entity.StocktakeOrder;
import com.wms.stocktake.repository.StocktakeApproveRecordRepository;
import com.wms.stocktake.repository.StocktakeItemRepository;
import com.wms.stocktake.repository.StocktakeOrderRepository;
import com.wms.system.entity.BaseLocation;
import com.wms.system.entity.BaseProduct;
import com.wms.system.entity.BaseZone;
import com.wms.system.entity.SysRole;
import com.wms.system.entity.SysUser;
import com.wms.system.entity.SysWarehouse;
import com.wms.system.repository.BaseLocationRepository;
import com.wms.system.repository.BaseProductRepository;
import com.wms.system.repository.BaseZoneRepository;
import com.wms.system.repository.SysRoleRepository;
import com.wms.system.repository.SysUserRepository;
import com.wms.system.repository.SysWarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 盘点服务实现
 */
@Service
@RequiredArgsConstructor
public class StocktakeServiceImpl implements StocktakeService {

    private final StocktakeOrderRepository orderRepository;
    private final StocktakeItemRepository itemRepository;
    private final InventoryRepositoryExt inventoryRepository;
    private final SysWarehouseRepository warehouseRepository;
    private final BaseLocationRepository locationRepository;
    private final BaseProductRepository productRepository;
    private final BaseZoneRepository zoneRepository;
    private final StocktakeApproveRecordRepository approveRecordRepository;
    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional
    public Long createOrder(StocktakeCreateDTO dto, Long userId, String username) {
        // 参数校验
        validateCreateDTO(dto);

        // 查询仓库
        SysWarehouse warehouse = warehouseRepository.selectById(dto.getWarehouseId());
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }

        // 生成盘点单号
        String orderNo = generateOrderNo();

        // 创建盘点单
        StocktakeOrder order = new StocktakeOrder();
        order.setOrderNo(orderNo);
        order.setWarehouseId(dto.getWarehouseId());
        order.setWarehouseCode(warehouse.getCode());
        order.setWarehouseName(warehouse.getName());
        order.setStocktakeType(dto.getStocktakeType());
        order.setBlindMode(dto.getBlindMode() != null ? dto.getBlindMode() : StocktakeOrder.BLIND_MODE_OFF);
        order.setScopeType(dto.getScopeType());
        order.setScopeConfig(buildScopeConfig(dto));
        order.setStatus(StocktakeOrder.STATUS_PENDING);
        order.setTotalItems(0); // 循环盘初始为0，其他类型后续更新
        order.setCountedItems(0);
        order.setDiffItems(0);
        order.setPlanDate(dto.getPlanDate());
        order.setRemark(dto.getRemark());
        order.setCreateUserId(userId);
        order.setCreateUserName(username);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 循环盘配置
        if (dto.getStocktakeType() == StocktakeOrder.TYPE_CYCLE) {
            order.setCycleType(dto.getCycleType());
            order.setCycleDay(dto.getCycleDay());
            order.setCycleStrategy(dto.getCycleStrategy());
            order.setCycleConfig(buildCycleConfig(dto));
            order.setCycleIndex(0);
            order.setNextCycleDate(calculateNextCycleDate(dto.getCycleType(), dto.getCycleDay()));
        }

        orderRepository.insert(order);

        // 循环盘不立即创建盘点明细，等到盘点日或手动触发
        if (dto.getStocktakeType() == StocktakeOrder.TYPE_CYCLE) {
            return order.getId();
        }

        // 全盘和抽盘：立即查询库存数据并创建盘点明细
        List<Inventory> inventories = getInventoriesByScope(dto);
        if (inventories.isEmpty()) {
            throw new IllegalStateException("该仓库无库存数据，无法创建盘点单");
        }

        // 创建盘点明细
        for (Inventory inv : inventories) {
            StocktakeItem item = new StocktakeItem();
            item.setOrderId(order.getId());
            item.setOrderNo(orderNo);
            item.setProductId(inv.getProductId());
            item.setSkuCode(inv.getSkuCode());
            item.setProductName(inv.getProductName());
            item.setLocationId(inv.getLocationId());
            item.setLocationCode(inv.getLocationCode());
            item.setBatchNo(inv.getBatchNo());
            item.setSystemQty(inv.getQty());
            item.setStatus(StocktakeItem.STATUS_PENDING);
            item.setRoundNo(1);
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            itemRepository.insert(item);
        }

        // 更新总SKU数
        order.setTotalItems(inventories.size());
        orderRepository.updateById(order);

        return order.getId();
    }

    @Override
    @Transactional
    public void countItem(StocktakeCountDTO dto, Long userId, String username) {
        // 查询盘点明细
        StocktakeItem item = itemRepository.selectById(dto.getItemId());
        if (item == null) {
            throw new IllegalArgumentException("盘点明细不存在");
        }

        // 查询盘点单
        StocktakeOrder order = orderRepository.selectById(item.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("盘点单不存在");
        }

        // 状态校验
        if (order.getStatus() != StocktakeOrder.STATUS_PENDING && order.getStatus() != StocktakeOrder.STATUS_COUNTING) {
            throw new IllegalStateException("盘点单状态不允许盘点");
        }

        // 更新盘点明细
        item.setCountedQty(dto.getCountedQty());
        item.setDiffQty(dto.getCountedQty() - item.getSystemQty());
        item.setDiffReason(dto.getDiffReason());
        item.setDiffRemark(dto.getDiffRemark());
        item.setStatus(StocktakeItem.STATUS_COUNTED);
        item.setRoundNo(dto.getRoundNo() != null ? dto.getRoundNo() : 1);
        item.setCountUserId(userId);
        item.setCountUserName(username);
        item.setCountTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        itemRepository.updateById(item);

        // 更新盘点单状态
        if (order.getStatus() == StocktakeOrder.STATUS_PENDING) {
            order.setStatus(StocktakeOrder.STATUS_COUNTING);
            order.setStartTime(LocalDateTime.now());
        }

        // 更新已盘点数量
        Integer counted = itemRepository.countByOrderIdAndCounted(order.getId());
        order.setCountedItems(counted != null ? counted : 0);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);
    }

    @Override
    @Transactional
    public void finishOrder(Long orderId) {
        StocktakeOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("盘点单不存在");
        }

        // 检查是否全部盘点完成
        if (order.getCountedItems() < order.getTotalItems()) {
            throw new IllegalStateException("还有未盘点的商品");
        }

        // 计算差异统计
        Integer diffCount = itemRepository.countByOrderIdAndDiff(orderId);
        order.setDiffItems(diffCount != null ? diffCount : 0);

        // 计算准确率
        if (order.getTotalItems() > 0) {
            BigDecimal accuracy = BigDecimal.valueOf(order.getTotalItems() - order.getDiffItems())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(order.getTotalItems()), 2, RoundingMode.HALF_UP);
            order.setAccuracyRate(accuracy);
        }

        order.setStatus(StocktakeOrder.STATUS_REVIEWING);
        order.setFinishTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);
    }

    @Override
    public Map<String, Object> queryOrders(StocktakeQueryDTO query) {
        LambdaQueryWrapper<StocktakeOrder> wrapper = new LambdaQueryWrapper<>();

        // 盘点单号查询
        if (query.getOrderNo() != null && !query.getOrderNo().isEmpty()) {
            wrapper.eq(StocktakeOrder::getOrderNo, query.getOrderNo());
        }

        // 仓库筛选
        if (query.getWarehouseId() != null) {
            wrapper.eq(StocktakeOrder::getWarehouseId, query.getWarehouseId());
        }

        // 盘点类型筛选
        if (query.getStocktakeType() != null) {
            wrapper.eq(StocktakeOrder::getStocktakeType, query.getStocktakeType());
        }

        // 状态筛选
        if (query.getStatus() != null) {
            wrapper.eq(StocktakeOrder::getStatus, query.getStatus());
        }

        // 计划日期范围
        if (query.getPlanDateStart() != null) {
            wrapper.ge(StocktakeOrder::getPlanDate, query.getPlanDateStart());
        }
        if (query.getPlanDateEnd() != null) {
            wrapper.le(StocktakeOrder::getPlanDate, query.getPlanDateEnd());
        }

        // 关键字搜索
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.and(w -> w
                    .like(StocktakeOrder::getOrderNo, query.getKeyword())
                    .or().like(StocktakeOrder::getWarehouseName, query.getKeyword())
            );
        }

        // 按创建时间倒序
        wrapper.orderByDesc(StocktakeOrder::getCreateTime);

        // 分页查询
        Page<StocktakeOrder> page = new Page<>(query.getPage(), query.getLimit());
        IPage<StocktakeOrder> result = orderRepository.selectPage(page, wrapper);

        // 转换为Map列表
        List<Map<String, Object>> list = result.getRecords().stream()
                .map(this::orderToMap)
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("list", list);
        response.put("total", result.getTotal());
        return response;
    }

    @Override
    public Map<String, Object> getOrderDetail(Long orderId) {
        StocktakeOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("盘点单不存在");
        }

        Map<String, Object> result = orderToMap(order);

        // 查询盘点明细
        List<StocktakeItem> items = itemRepository.findByOrderId(orderId);
        List<Map<String, Object>> itemList = items.stream()
                .map(this::itemToMap)
                .collect(java.util.stream.Collectors.toList());

        result.put("items", itemList);
        return result;
    }

    @Override
    public List<StocktakeItem> getOrderItems(Long orderId) {
        return itemRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        StocktakeOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("盘点单不存在");
        }

        // 只有待盘点状态可以取消
        if (order.getStatus() != StocktakeOrder.STATUS_PENDING) {
            throw new IllegalStateException("只有待盘点状态的盘点单可以取消");
        }

        order.setStatus(StocktakeOrder.STATUS_CANCELLED);
        order.setRemark(reason);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);
    }

    @Override
    @Transactional
    public void forceCancelOrder(Long orderId, String reason) {
        StocktakeOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("盘点单不存在");
        }

        // 已取消的不再处理
        if (order.getStatus() == StocktakeOrder.STATUS_CANCELLED) {
            throw new IllegalStateException("盘点单已取消");
        }

        // 已完成的不能取消
        if (order.getStatus() == StocktakeOrder.STATUS_COMPLETED) {
            throw new IllegalStateException("已完成的盘点单不能取消");
        }

        order.setStatus(StocktakeOrder.STATUS_CANCELLED);
        order.setRemark(reason);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);
    }

    // ========== 私有方法 ==========

    private void validateCreateDTO(StocktakeCreateDTO dto) {
        if (dto.getWarehouseId() == null) {
            throw new IllegalArgumentException("仓库不能为空");
        }
        if (dto.getStocktakeType() == null) {
            throw new IllegalArgumentException("盘点类型不能为空");
        }
        if (dto.getPlanDate() == null) {
            throw new IllegalArgumentException("计划日期不能为空");
        }

        // 抽盘时验证筛选条件
        if (dto.getStocktakeType() == StocktakeOrder.TYPE_SAMPLE) {
            if (dto.getScopeType() == null || dto.getScopeType().isEmpty()) {
                throw new IllegalArgumentException("抽盘必须指定筛选方式");
            }
        }
    }

    private List<Inventory> getInventoriesByScope(StocktakeCreateDTO dto) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getWarehouseId, dto.getWarehouseId());

        // 全盘：按库区筛选
        if ("zone".equals(dto.getScopeType()) && dto.getZoneId() != null) {
            // 查询该库区下的所有库位ID
            LambdaQueryWrapper<BaseLocation> locWrapper = new LambdaQueryWrapper<>();
            locWrapper.eq(BaseLocation::getZoneId, dto.getZoneId());
            List<BaseLocation> locations = locationRepository.selectList(locWrapper);
            if (locations.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> locationIds = locations.stream().map(BaseLocation::getId).collect(Collectors.toList());
            wrapper.in(Inventory::getLocationId, locationIds);
        }

        // 抽盘：按筛选方式处理
        if (dto.getStocktakeType() == StocktakeOrder.TYPE_SAMPLE) {
            String scopeType = dto.getScopeType();

            if ("zone".equals(scopeType) && dto.getZoneIds() != null && !dto.getZoneIds().isEmpty()) {
                // 按库区筛选
                LambdaQueryWrapper<BaseLocation> locWrapper = new LambdaQueryWrapper<>();
                locWrapper.in(BaseLocation::getZoneId, dto.getZoneIds());
                List<BaseLocation> locations = locationRepository.selectList(locWrapper);
                if (locations.isEmpty()) {
                    return Collections.emptyList();
                }
                List<Long> locationIds = locations.stream().map(BaseLocation::getId).collect(Collectors.toList());
                wrapper.in(Inventory::getLocationId, locationIds);

            } else if ("category".equals(scopeType) && dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
                // 按商品分类筛选：查询该分类下的商品ID
                LambdaQueryWrapper<BaseProduct> prodWrapper = new LambdaQueryWrapper<>();
                prodWrapper.in(BaseProduct::getCategoryId, dto.getCategoryIds());
                List<BaseProduct> products = productRepository.selectList(prodWrapper);
                if (products.isEmpty()) {
                    return Collections.emptyList();
                }
                List<Long> productIds = products.stream().map(BaseProduct::getId).collect(Collectors.toList());
                wrapper.in(Inventory::getProductId, productIds);

            } else if ("abc".equals(scopeType) && dto.getAbcClass() != null) {
                // 按ABC分类筛选：需要商品表有abcClass字段
                // 目前简化处理，按SKU编码前缀筛选
                LambdaQueryWrapper<BaseProduct> prodWrapper = new LambdaQueryWrapper<>();
                prodWrapper.isNotNull(BaseProduct::getSkuCode);
                List<BaseProduct> products = productRepository.selectList(prodWrapper);
                // TODO: 实际需要根据ABC分类字段筛选
                // 这里简化为取前30%的商品
                int limit = (int) (products.size() * 0.3);
                if ("A".equals(dto.getAbcClass())) {
                    limit = (int) (products.size() * 0.2);
                } else if ("C".equals(dto.getAbcClass())) {
                    limit = (int) (products.size() * 0.5);
                }
                List<Long> productIds = products.stream()
                        .limit(Math.max(limit, 1))
                        .map(BaseProduct::getId)
                        .collect(Collectors.toList());
                if (productIds.isEmpty()) {
                    return Collections.emptyList();
                }
                wrapper.in(Inventory::getProductId, productIds);

            } else if ("sku".equals(scopeType) && dto.getSkuCodes() != null && !dto.getSkuCodes().isEmpty()) {
                // 指定SKU筛选
                wrapper.in(Inventory::getSkuCode, dto.getSkuCodes());

            } else if ("random".equals(scopeType) && dto.getRandomPercent() != null) {
                // 随机抽取
                List<Inventory> allInventories = inventoryRepository.selectList(wrapper);
                int totalSize = allInventories.size();
                int sampleSize = (int) Math.ceil(totalSize * dto.getRandomPercent() / 100.0);
                if (sampleSize >= totalSize) {
                    return allInventories;
                }
                // 随机打乱并取前N个
                Collections.shuffle(allInventories, new Random());
                return allInventories.subList(0, sampleSize);
            }
        }

        return inventoryRepository.selectList(wrapper);
    }

    private String buildScopeConfig(StocktakeCreateDTO dto) {
        try {
            Map<String, Object> config = new HashMap<>();
            if (dto.getZoneId() != null) {
                config.put("zoneId", dto.getZoneId());
            }
            if (dto.getZoneIds() != null && !dto.getZoneIds().isEmpty()) {
                config.put("zoneIds", dto.getZoneIds());
            }
            if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
                config.put("categoryIds", dto.getCategoryIds());
            }
            if (dto.getAbcClass() != null) {
                config.put("abcClass", dto.getAbcClass());
            }
            if (dto.getSkuCodes() != null && !dto.getSkuCodes().isEmpty()) {
                config.put("skuCodes", dto.getSkuCodes());
            }
            if (dto.getRandomPercent() != null) {
                config.put("randomPercent", dto.getRandomPercent());
            }
            return config.isEmpty() ? null : objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 构建循环盘配置JSON
     */
    private String buildCycleConfig(StocktakeCreateDTO dto) {
        try {
            Map<String, Object> config = new HashMap<>();
            if (dto.getCycleZoneIds() != null && !dto.getCycleZoneIds().isEmpty()) {
                config.put("zoneIds", dto.getCycleZoneIds());
            }
            if (dto.getCycleSkuPercent() != null) {
                config.put("skuPercent", dto.getCycleSkuPercent());
            }
            return config.isEmpty() ? null : objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 计算下次盘点日期
     */
    private LocalDate calculateNextCycleDate(String cycleType, Integer cycleDay) {
        LocalDate today = LocalDate.now();

        if (StocktakeOrder.CYCLE_TYPE_DAILY.equals(cycleType)) {
            return today.plusDays(1);
        }

        if (StocktakeOrder.CYCLE_TYPE_WEEKLY.equals(cycleType)) {
            // cycleDay: 1=周一, 7=周日
            int currentDayOfWeek = today.getDayOfWeek().getValue();
            int daysUntilNext = cycleDay - currentDayOfWeek;
            if (daysUntilNext <= 0) {
                daysUntilNext += 7; // 下周
            }
            return today.plusDays(daysUntilNext);
        }

        if (StocktakeOrder.CYCLE_TYPE_MONTHLY.equals(cycleType)) {
            int currentDayOfMonth = today.getDayOfMonth();
            int daysInMonth = today.lengthOfMonth();

            if (cycleDay > daysInMonth) {
                // 如果指定的日期超过当月天数，则在下个月的同一天
                return today.plusMonths(1).withDayOfMonth(Math.min(cycleDay, today.plusMonths(1).lengthOfMonth()));
            }

            if (cycleDay > currentDayOfMonth) {
                return today.withDayOfMonth(cycleDay);
            } else {
                // 下个月
                LocalDate nextMonth = today.plusMonths(1);
                return nextMonth.withDayOfMonth(Math.min(cycleDay, nextMonth.lengthOfMonth()));
            }
        }

        return today;
    }

    private String generateOrderNo() {
        String datePrefix = "ST" + LocalDate.now().format(DATE_FORMATTER);
        Integer maxSeq = orderRepository.getMaxSeqByDate(datePrefix);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;
        return datePrefix + String.format("%04d", nextSeq);
    }

    private Map<String, Object> orderToMap(StocktakeOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("warehouseId", order.getWarehouseId());
        map.put("warehouseCode", order.getWarehouseCode());
        map.put("warehouseName", order.getWarehouseName());
        map.put("stocktakeType", order.getStocktakeType());
        map.put("stocktakeTypeName", getStocktakeTypeName(order.getStocktakeType()));
        map.put("blindMode", order.getBlindMode());
        map.put("scopeType", order.getScopeType());
        map.put("scopeName", getScopeName(order));
        map.put("status", order.getStatus());
        map.put("statusName", getStatusName(order.getStatus()));
        map.put("totalItems", order.getTotalItems());
        map.put("countedItems", order.getCountedItems());
        map.put("diffItems", order.getDiffItems());
        map.put("accuracyRate", order.getAccuracyRate());
        map.put("planDate", order.getPlanDate());
        map.put("startTime", order.getStartTime());
        map.put("finishTime", order.getFinishTime());
        map.put("remark", order.getRemark());
        map.put("createUserName", order.getCreateUserName());
        map.put("createTime", order.getCreateTime());

        // 循环盘字段
        map.put("cycleType", order.getCycleType());
        map.put("cycleDay", order.getCycleDay());
        map.put("cycleStrategy", order.getCycleStrategy());
        map.put("cycleStrategyName", getCycleStrategyName(order.getCycleStrategy()));
        map.put("cycleIndex", order.getCycleIndex());
        map.put("lastCycleDate", order.getLastCycleDate());
        map.put("nextCycleDate", order.getNextCycleDate());
        map.put("parentStrategyId", order.getParentStrategyId());

        // 是否可以生成盘点数据（循环盘）
        boolean canGenerate = canGenerateCycleData(order);
        map.put("canGenerateCycleData", canGenerate);

        return map;
    }

    /**
     * 判断循环盘是否可以生成盘点数据
     */
    private boolean canGenerateCycleData(StocktakeOrder order) {
        if (order.getStocktakeType() != StocktakeOrder.TYPE_CYCLE) {
            return false;
        }

        // 如果已有盘点明细，不能再生成
        LambdaQueryWrapper<StocktakeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StocktakeItem::getOrderId, order.getId());
        long itemCount = itemRepository.selectCount(wrapper);
        if (itemCount > 0) {
            return false;
        }

        return true;
    }

    /**
     * 获取轮转策略名称
     */
    private String getCycleStrategyName(String strategy) {
        if (strategy == null) return null;
        switch (strategy) {
            case "zone_rotation": return "按库区轮转";
            case "sku_rotation": return "按SKU轮转";
            case "fixed": return "固定范围";
            default: return strategy;
        }
    }

    /**
     * 获取盘点范围名称
     */
    private String getScopeName(StocktakeOrder order) {
        String scopeType = order.getScopeType();
        if (scopeType == null || "all".equals(scopeType)) {
            return "全仓库";
        }

        String scopeConfig = order.getScopeConfig();
        if (scopeConfig == null || scopeConfig.isEmpty()) {
            return getScopeTypeName(scopeType);
        }

        try {
            Map<String, Object> config = objectMapper.readValue(scopeConfig, Map.class);

            if ("zone".equals(scopeType)) {
                // 按库区
                Object zoneIdObj = config.get("zoneId");
                Object zoneIdsObj = config.get("zoneIds");

                if (zoneIdObj != null) {
                    Long zoneId = Long.valueOf(zoneIdObj.toString());
                    BaseZone zone = zoneRepository.selectById(zoneId);
                    return zone != null ? zone.getName() : "指定库区";
                }
                if (zoneIdsObj != null) {
                    List<Integer> zoneIds = (List<Integer>) zoneIdsObj;
                    if (zoneIds.size() == 1) {
                        Long zoneId = zoneIds.get(0).longValue();
                        BaseZone zone = zoneRepository.selectById(zoneId);
                        return zone != null ? zone.getName() : "指定库区";
                    }
                    return zoneIds.size() + "个库区";
                }
                return "指定库区";
            }

            if ("category".equals(scopeType)) {
                // 按分类
                Object categoryIdsObj = config.get("categoryIds");
                if (categoryIdsObj != null) {
                    List<Integer> categoryIds = (List<Integer>) categoryIdsObj;
                    if (categoryIds.size() == 1) {
                        return "1个分类";
                    }
                    return categoryIds.size() + "个分类";
                }
                return "指定分类";
            }

            if ("abc".equals(scopeType)) {
                Object abcClass = config.get("abcClass");
                return abcClass != null ? abcClass + "类商品" : "ABC分类";
            }

            if ("sku".equals(scopeType)) {
                Object skuCodesObj = config.get("skuCodes");
                if (skuCodesObj != null) {
                    List<String> skuCodes = (List<String>) skuCodesObj;
                    return skuCodes.size() + "个SKU";
                }
                return "指定商品";
            }

            if ("random".equals(scopeType)) {
                Object percent = config.get("randomPercent");
                return percent != null ? "随机" + percent + "%" : "随机抽取";
            }

        } catch (Exception e) {
            // 解析失败，返回类型名称
        }

        return getScopeTypeName(scopeType);
    }

    private String getScopeTypeName(String scopeType) {
        if (scopeType == null) return "全仓库";
        switch (scopeType) {
            case "all": return "全仓库";
            case "zone": return "指定库区";
            case "category": return "指定分类";
            case "abc": return "ABC分类";
            case "sku": return "指定商品";
            case "random": return "随机抽取";
            default: return scopeType;
        }
    }

    private Map<String, Object> itemToMap(StocktakeItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("orderId", item.getOrderId());
        map.put("orderNo", item.getOrderNo());
        map.put("productId", item.getProductId());
        map.put("skuCode", item.getSkuCode());
        map.put("productName", item.getProductName());
        map.put("barcode", item.getBarcode());
        map.put("locationId", item.getLocationId());
        map.put("locationCode", item.getLocationCode());
        map.put("batchNo", item.getBatchNo());
        map.put("systemQty", item.getSystemQty());
        map.put("countedQty", item.getCountedQty());
        map.put("diffQty", item.getDiffQty());
        map.put("diffReason", item.getDiffReason());
        map.put("diffReasonName", getDiffReasonName(item.getDiffReason()));
        map.put("diffRemark", item.getDiffRemark());
        map.put("status", item.getStatus());
        map.put("statusName", getItemStatusName(item.getStatus()));
        map.put("roundNo", item.getRoundNo());
        map.put("countUserName", item.getCountUserName());
        map.put("countTime", item.getCountTime());
        return map;
    }

    private String getStocktakeTypeName(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1: return "全盘";
            case 2: return "抽盘";
            case 3: return "循环盘";
            default: return "";
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待盘点";
            case 1: return "盘点中";
            case 2: return "待审核";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "";
        }
    }

    private String getItemStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待盘点";
            case 1: return "已盘点";
            case 2: return "已确认";
            default: return "";
        }
    }

    private String getDiffReasonName(String reason) {
        if (reason == null) return "";
        switch (reason) {
            case "profit": return "盘盈";
            case "loss": return "盘亏";
            case "wrong": return "错放";
            case "missed": return "漏扫";
            case "other": return "其他";
            default: return "";
        }
    }

    /**
     * 手动触发生成循环盘数据
     */
    @Override
    @Transactional
    public void generateCycleData(Long orderId, Long userId, String username) {
        StocktakeOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("盘点单不存在");
        }

        if (order.getStocktakeType() != StocktakeOrder.TYPE_CYCLE) {
            throw new IllegalArgumentException("只有循环盘点单可以生成数据");
        }

        // 检查是否已有盘点明细
        LambdaQueryWrapper<StocktakeItem> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(StocktakeItem::getOrderId, orderId);
        long existingCount = itemRepository.selectCount(checkWrapper);
        if (existingCount > 0) {
            throw new IllegalStateException("盘点数据已存在，无法重复生成");
        }

        // 根据轮转策略获取库存数据
        List<Inventory> inventories = getInventoriesForCycle(order);

        if (inventories.isEmpty()) {
            throw new IllegalStateException("没有找到符合条件的库存数据");
        }

        // 创建盘点明细
        for (Inventory inv : inventories) {
            StocktakeItem item = new StocktakeItem();
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setProductId(inv.getProductId());
            item.setSkuCode(inv.getSkuCode());
            item.setProductName(inv.getProductName());
            item.setLocationId(inv.getLocationId());
            item.setLocationCode(inv.getLocationCode());
            item.setBatchNo(inv.getBatchNo());
            item.setSystemQty(inv.getQty());
            item.setStatus(StocktakeItem.STATUS_PENDING);
            item.setRoundNo(1);
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            itemRepository.insert(item);
        }

        // 更新盘点单
        order.setTotalItems(inventories.size());
        order.setLastCycleDate(LocalDate.now());
        order.setNextCycleDate(calculateNextCycleDate(order.getCycleType(), order.getCycleDay()));
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);
    }

    /**
     * 根据轮转策略获取库存数据
     */
    private List<Inventory> getInventoriesForCycle(StocktakeOrder order) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getWarehouseId, order.getWarehouseId());

        String strategy = order.getCycleStrategy();
        String cycleConfig = order.getCycleConfig();

        // 解析配置
        List<Long> zoneIds = new ArrayList<>();
        int skuPercent = 10; // 默认10%

        if (cycleConfig != null && !cycleConfig.isEmpty()) {
            try {
                Map<String, Object> config = objectMapper.readValue(cycleConfig, Map.class);
                if (config.get("zoneIds") != null) {
                    zoneIds = ((List<?>) config.get("zoneIds")).stream()
                        .map(obj -> Long.valueOf(obj.toString()))
                        .collect(Collectors.toList());
                }
                if (config.get("skuPercent") != null) {
                    skuPercent = Integer.valueOf(config.get("skuPercent").toString());
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }

        // 按库区轮转
        if (StocktakeOrder.CYCLE_STRATEGY_ZONE.equals(strategy) && !zoneIds.isEmpty()) {
            int currentIndex = order.getCycleIndex() != null ? order.getCycleIndex() : 0;
            int zoneIndex = currentIndex % zoneIds.size();
            Long zoneId = zoneIds.get(zoneIndex);

            // 更新轮转索引
            order.setCycleIndex(zoneIndex + 1);
            orderRepository.updateById(order);

            // 查询该库区的库位
            LambdaQueryWrapper<BaseLocation> locWrapper = new LambdaQueryWrapper<>();
            locWrapper.eq(BaseLocation::getZoneId, zoneId);
            List<BaseLocation> locations = locationRepository.selectList(locWrapper);

            if (locations.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> locationIds = locations.stream()
                .map(BaseLocation::getId)
                .collect(Collectors.toList());

            wrapper.in(Inventory::getLocationId, locationIds);
            return inventoryRepository.selectList(wrapper);
        }

        // 按SKU轮转
        if (StocktakeOrder.CYCLE_STRATEGY_SKU.equals(strategy)) {
            List<Inventory> allInventories = inventoryRepository.selectList(wrapper);
            int totalSize = allInventories.size();
            int sampleSize = (int) Math.ceil(totalSize * skuPercent / 100.0);

            if (sampleSize >= totalSize) {
                return allInventories;
            }

            // 使用轮转索引实现轮转效果
            int currentIndex = order.getCycleIndex() != null ? order.getCycleIndex() : 0;
            int startIndex = (currentIndex * sampleSize) % totalSize;

            // 更新轮转索引
            order.setCycleIndex(currentIndex + 1);
            orderRepository.updateById(order);

            // 循环取数据
            List<Inventory> result = new ArrayList<>();
            for (int i = 0; i < sampleSize; i++) {
                result.add(allInventories.get((startIndex + i) % totalSize));
            }
            return result;
        }

        // 固定范围（查询所有库存）
        return inventoryRepository.selectList(wrapper);
    }

    /**
     * 审核盘点单（通过）
     */
    @Override
    @Transactional
    public void approveOrder(Long orderId, Long userId, String username) {
        StocktakeOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("盘点单不存在");
        }

        if (order.getStatus() != StocktakeOrder.STATUS_REVIEWING) {
            throw new IllegalStateException("只有待审核状态的盘点单可以审核");
        }

        // 查询盘点明细，处理差异
        LambdaQueryWrapper<StocktakeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StocktakeItem::getOrderId, orderId);
        wrapper.isNotNull(StocktakeItem::getDiffQty);
        wrapper.ne(StocktakeItem::getDiffQty, 0);
        List<StocktakeItem> diffItems = itemRepository.selectList(wrapper);

        // 调整库存
        for (StocktakeItem item : diffItems) {
            if (item.getDiffQty() != null && item.getDiffQty() != 0) {
                adjustInventory(item);
            }
        }

        // 更新盘点单状态
        order.setStatus(StocktakeOrder.STATUS_COMPLETED);
        order.setApproveUserId(userId);
        order.setApproveUserName(username);
        order.setApproveTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);

        // 记录审核操作
        saveApproveRecord(order, StocktakeApproveRecord.ACTION_APPROVE, null, userId, username);
    }

    /**
     * 审核盘点单（驳回）
     */
    @Override
    @Transactional
    public void rejectOrder(Long orderId, String reason, Long userId, String username) {
        StocktakeOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("盘点单不存在");
        }

        if (order.getStatus() != StocktakeOrder.STATUS_REVIEWING) {
            throw new IllegalStateException("只有待审核状态的盘点单可以驳回");
        }

        // 驳回后回到盘点中状态，可以重新盘点
        order.setStatus(StocktakeOrder.STATUS_COUNTING);
        order.setApproveUserId(userId);
        order.setApproveUserName(username);
        order.setApproveTime(LocalDateTime.now());
        order.setRemark((order.getRemark() != null ? order.getRemark() + "\n" : "") + "驳回原因：" + reason);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);

        // 记录审核操作
        saveApproveRecord(order, StocktakeApproveRecord.ACTION_REJECT, reason, userId, username);

        // 重置已盘点的明细状态为待盘点
        LambdaQueryWrapper<StocktakeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StocktakeItem::getOrderId, orderId);
        wrapper.eq(StocktakeItem::getStatus, StocktakeItem.STATUS_COUNTED);
        List<StocktakeItem> items = itemRepository.selectList(wrapper);

        for (StocktakeItem item : items) {
            item.setStatus(StocktakeItem.STATUS_PENDING);
            item.setCountedQty(null);
            item.setDiffQty(null);
            item.setDiffReason(null);
            item.setDiffRemark(null);
            item.setCountUserId(null);
            item.setCountUserName(null);
            item.setCountTime(null);
            item.setUpdateTime(LocalDateTime.now());
            itemRepository.updateById(item);
        }
    }

    /**
     * 调整库存
     */
    private void adjustInventory(StocktakeItem item) {
        // 查询库存记录
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getProductId, item.getProductId());
        wrapper.eq(Inventory::getLocationId, item.getLocationId());
        wrapper.eq(Inventory::getBatchNo, item.getBatchNo());

        Inventory inventory = inventoryRepository.selectOne(wrapper);

        if (inventory == null) {
            // 盘盈：库存不存在，创建新库存
            if (item.getDiffQty() != null && item.getDiffQty() > 0) {
                inventory = new Inventory();
                inventory.setProductId(item.getProductId());
                inventory.setSkuCode(item.getSkuCode());
                inventory.setProductName(item.getProductName());
                inventory.setLocationId(item.getLocationId());
                inventory.setLocationCode(item.getLocationCode());
                inventory.setBatchNo(item.getBatchNo());
                inventory.setQty(item.getCountedQty());
                inventory.setWarehouseId(item.getLocationId() != null ?
                    getLocationWarehouseId(item.getLocationId()) : null);
                inventory.setCreateTime(LocalDateTime.now());
                inventory.setUpdateTime(LocalDateTime.now());
                inventoryRepository.insert(inventory);
            }
        } else {
            // 调整现有库存
            int newQty = inventory.getQty() + (item.getDiffQty() != null ? item.getDiffQty() : 0);
            if (newQty <= 0) {
                // 盘亏导致库存为0，删除库存记录
                inventoryRepository.deleteById(inventory.getId());
            } else {
                inventory.setQty(newQty);
                inventory.setUpdateTime(LocalDateTime.now());
                inventoryRepository.updateById(inventory);
            }
        }
    }

    /**
     * 获取库位所属仓库ID
     */
    private Long getLocationWarehouseId(Long locationId) {
        BaseLocation location = locationRepository.selectById(locationId);
        if (location != null) {
            // 通过库区获取仓库ID
            BaseZone zone = zoneRepository.selectById(location.getZoneId());
            if (zone != null) {
                return zone.getWarehouseId();
            }
        }
        return null;
    }

    /**
     * 保存审核记录
     */
    private void saveApproveRecord(StocktakeOrder order, String action, String reason, Long userId, String username) {
        // 查询用户角色
        String roleName = "";
        try {
            SysUser user = userRepository.selectById(userId);
            if (user != null && user.getRoleId() != null) {
                SysRole role = roleRepository.selectById(user.getRoleId());
                if (role != null) {
                    roleName = role.getName();
                }
            }
        } catch (Exception ignored) {
            // 忽略错误，角色名称为空
        }

        StocktakeApproveRecord record = new StocktakeApproveRecord();
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setAction(action);
        record.setReason(reason);
        record.setOperatorId(userId);
        record.setOperatorName(username);
        record.setOperatorRoleName(roleName);
        record.setCreateTime(LocalDateTime.now());
        approveRecordRepository.insert(record);
    }

    /**
     * 查询审核记录
     */
    @Override
    public List<Map<String, Object>> getApproveRecords(Long orderId) {
        List<Map<String, Object>> records = approveRecordRepository.selectByOrderId(orderId);

        // 转换action为中文
        for (Map<String, Object> record : records) {
            String action = (String) record.get("action");
            if (StocktakeApproveRecord.ACTION_APPROVE.equals(action)) {
                record.put("actionName", "审核通过");
            } else if (StocktakeApproveRecord.ACTION_REJECT.equals(action)) {
                record.put("actionName", "驳回");
            }
        }

        return records;
    }
}
