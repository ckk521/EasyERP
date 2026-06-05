package com.wms.outbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.outbound.dto.*;
import com.wms.outbound.entity.OutboundOrder;
import com.wms.outbound.entity.OutboundOrderItem;
import com.wms.outbound.entity.Wave;
import com.wms.outbound.repository.OutboundOrderItemRepository;
import com.wms.outbound.repository.OutboundOrderRepository;
import com.wms.outbound.repository.WaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 波次管理服务
 *
 * 实现功能：
 * 1. 波次创建（按时间/物流/区域/商品策略）
 * 2. 波次释放（库存锁定）
 * 3. 波次取消（库存释放）
 * 4. 波次拆分（最大订单数限制）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaveService {

    private final WaveRepository waveRepository;
    private final OutboundOrderRepository orderRepository;
    private final OutboundOrderItemRepository orderItemRepository;
    private final InventoryAllocationService allocationService;

    /**
     * 创建波次
     */
    @Transactional
    public Long createWave(WaveCreateDTO dto) {
        // 1. 根据策略查询符合条件的订单
        List<OutboundOrder> orders = findOrdersByStrategy(dto);

        // 2. 校验订单数量
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("无符合条件的订单");
        }

        // 3. 创建波次
        Wave wave = buildWave(dto, orders);
        waveRepository.insert(wave);

        // 4. 更新订单的波次信息
        updateOrdersWaveInfo(orders, wave);

        log.info("波次创建成功: waveNo={}, orders={}", wave.getWaveNo(), wave.getTotalOrders());
        return wave.getId();
    }

    /**
     * 创建波次并返回统计信息
     */
    @Transactional
    public Map<String, Object> createWaveWithStatistics(WaveCreateDTO dto) {
        Long waveId = createWave(dto);
        Map<String, Object> result = new HashMap<>();
        result.put("waveId", waveId);

        // 如果是按商品策略，返回高频SKU统计
        if (dto.getStrategyType() == Wave.STRATEGY_PRODUCT) {
            List<Map<String, Object>> highFrequencySku = calculateHighFrequencySku(dto);
            result.put("highFrequencySku", highFrequencySku);
        }

        return result;
    }

    /**
     * 创建波次（支持拆分）
     */
    @Transactional
    public List<Long> createWaveWithSplit(WaveCreateDTO dto) {
        // 1. 根据策略查询符合条件的订单
        List<OutboundOrder> orders = findOrdersByStrategy(dto);

        if (orders.isEmpty()) {
            throw new IllegalArgumentException("无符合条件的订单");
        }

        // 2. 检查是否需要拆分
        Integer maxOrders = dto.getMaxOrders();
        if (maxOrders == null || orders.size() <= maxOrders) {
            // 不需要拆分，创建单个波次
            Long waveId = createWave(dto);
            return Collections.singletonList(waveId);
        }

        // 3. 需要拆分为多个波次
        List<Long> waveIds = new ArrayList<>();
        int totalOrders = orders.size();
        int startIndex = 0;

        while (startIndex < totalOrders) {
            int endIndex = Math.min(startIndex + maxOrders, totalOrders);
            List<OutboundOrder> batchOrders = orders.subList(startIndex, endIndex);

            // 创建波次
            Wave wave = buildWave(dto, batchOrders);
            waveRepository.insert(wave);
            waveIds.add(wave.getId());

            // 更新订单的波次信息
            updateOrdersWaveInfo(batchOrders, wave);

            startIndex = endIndex;
        }

        log.info("波次拆分创建完成: 总订单数={}, 波次数={}", totalOrders, waveIds.size());
        return waveIds;
    }

    /**
     * 释放波次
     */
    @Transactional
    public WaveReleaseResultDTO releaseWave(WaveReleaseDTO dto) {
        WaveReleaseResultDTO result = new WaveReleaseResultDTO();

        // 1. 查询波次
        Wave wave = waveRepository.selectById(dto.getWaveId());
        if (wave == null) {
            result.setSuccess(false);
            result.setFailReason("波次不存在");
            return result;
        }

        // 2. 检查波次状态
        if (wave.getStatus() != Wave.STATUS_PENDING) {
            result.setSuccess(false);
            result.setFailReason("波次状态不允许释放");
            return result;
        }

        // 3. 查询波次中的订单
        LambdaQueryWrapper<OutboundOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(OutboundOrder::getWaveId, wave.getId());
        List<OutboundOrder> orders = orderRepository.selectList(orderWrapper);

        // 4. 准备库存分配数据
        List<AllocationDTO> allocations = prepareAllocations(orders);

        // 5. 批量锁定库存
        List<AllocationResultDTO> allocationResults = allocationService.batchLockInventory(allocations);

        // 6. 检查锁定结果
        List<AllocationResultDTO> failures = allocationResults.stream()
            .filter(r -> !r.getSuccess())
            .collect(Collectors.toList());

        if (!failures.isEmpty()) {
            // 有库存不足的情况
            if (Boolean.TRUE.equals(dto.getAllowPartialRelease())) {
                // 允许部分释放
                return handlePartialRelease(wave, orders, allocationResults, failures);
            } else {
                // 不允许部分释放，返回失败
                result.setSuccess(false);
                result.setShortageList(buildShortageList(failures));
                result.setFailReason("部分商品库存不足");
                return result;
            }
        }

        // 7. 全部锁定成功，更新波次和订单状态
        wave.setStatus(Wave.STATUS_PICKING);
        wave.setStartTime(LocalDateTime.now());
        if (!Boolean.TRUE.equals(dto.getAutoAssign()) && dto.getAssignedUserId() != null) {
            wave.setAssignedUserId(dto.getAssignedUserId());
            wave.setAssignedUserName(dto.getAssignedUserName());
        }
        waveRepository.updateById(wave);

        // 更新订单状态为已分配
        for (OutboundOrder order : orders) {
            order.setStatus(OutboundOrder.STATUS_ALLOCATED);
            orderRepository.updateById(order);
        }

        // 8. 生成拣货任务
        int pickTaskCount = generatePickTasks(wave, orders);

        result.setSuccess(true);
        result.setNewStatus(Wave.STATUS_PICKING);
        result.setPickTaskCount(pickTaskCount);
        result.setReleasedOrderCount(orders.size());

        log.info("波次释放成功: waveId={}, orders={}, pickTasks={}",
            wave.getId(), orders.size(), pickTaskCount);

        return result;
    }

    /**
     * 取消波次
     */
    @Transactional
    public boolean cancelWave(Long waveId) {
        // 1. 查询波次
        Wave wave = waveRepository.selectById(waveId);
        if (wave == null) {
            throw new IllegalArgumentException("波次不存在");
        }

        // 2. 检查状态
        if (wave.getStatus() == Wave.STATUS_COMPLETED) {
            throw new IllegalStateException("已完成的波次不可取消");
        }

        // 3. 如果是拣货中状态，需要释放库存
        if (wave.getStatus() == Wave.STATUS_PICKING) {
            // 查询波次中的订单
            LambdaQueryWrapper<OutboundOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(OutboundOrder::getWaveId, waveId);
            List<OutboundOrder> orders = orderRepository.selectList(orderWrapper);

            // 释放库存
            releaseInventoryForOrders(orders);

            // 订单状态回退为待分配
            for (OutboundOrder order : orders) {
                order.setStatus(OutboundOrder.STATUS_PENDING);
                order.setWaveId(null);
                order.setWaveNo(null);
                orderRepository.updateById(order);
            }
        }

        // 4. 更新波次状态为已取消
        wave.setStatus(Wave.STATUS_CANCELLED);
        waveRepository.updateById(wave);

        log.info("波次取消成功: waveId={}", waveId);
        return true;
    }

    // ========== 私有方法 ==========

    /**
     * 根据策略查询订单
     */
    private List<OutboundOrder> findOrdersByStrategy(WaveCreateDTO dto) {
        LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<>();

        // 基础条件：待分配状态，指定仓库
        wrapper.eq(OutboundOrder::getStatus, OutboundOrder.STATUS_PENDING);
        if (dto.getWarehouseId() != null) {
            wrapper.eq(OutboundOrder::getWarehouseId, dto.getWarehouseId());
        }

        // 根据策略类型添加条件
        switch (dto.getStrategyType()) {
            case Wave.STRATEGY_TIME:
                // 按时间策略：创建时间在指定范围内
                if (dto.getStartTime() != null && dto.getEndTime() != null) {
                    wrapper.between(OutboundOrder::getCreateTime, dto.getStartTime(), dto.getEndTime());
                }
                break;

            case Wave.STRATEGY_LOGISTICS:
                // 按物流策略：指定物流公司
                if (dto.getLogisticsCompany() != null) {
                    wrapper.eq(OutboundOrder::getLogisticsCompany, dto.getLogisticsCompany());
                }
                break;

            case Wave.STRATEGY_REGION:
                // 按区域策略：收货地址包含指定区域
                if (dto.getRegion() != null) {
                    wrapper.like(OutboundOrder::getReceiverAddress, dto.getRegion());
                }
                break;

            case Wave.STRATEGY_PRODUCT:
                // 按商品策略：包含高频SKU的订单
                // TODO: 实现高频SKU筛选逻辑
                break;

            case Wave.STRATEGY_CUSTOMER:
                // 按客户策略：指定客户
                if (dto.getCustomerId() != null) {
                    wrapper.eq(OutboundOrder::getCustomerId, dto.getCustomerId());
                }
                break;

            default:
                throw new IllegalArgumentException("不支持的策略类型: " + dto.getStrategyType());
        }

        wrapper.orderByAsc(OutboundOrder::getCreateTime);

        return orderRepository.selectList(wrapper);
    }

    /**
     * 构建波次对象
     */
    private Wave buildWave(WaveCreateDTO dto, List<OutboundOrder> orders) {
        Wave wave = new Wave();
        wave.setWaveNo(generateWaveNo());
        wave.setStrategyType(dto.getStrategyType());
        wave.setStrategyName(dto.getStrategyName());
        wave.setWarehouseId(dto.getWarehouseId());
        wave.setWarehouseCode(dto.getWarehouseCode());
        wave.setWarehouseName(dto.getWarehouseName());
        wave.setStatus(Wave.STATUS_PENDING);

        // 计算统计信息
        wave.setTotalOrders(orders.size());
        int totalSku = calculateTotalSku(orders);
        int totalQty = orders.stream().mapToInt(OutboundOrder::getTotalQty).sum();
        wave.setTotalSku(totalSku);
        wave.setTotalQty(totalQty);
        wave.setPickedQty(0);
        wave.setPackedQty(0);
        wave.setShippedQty(0);

        wave.setRemark(dto.getRemark());
        wave.setCreateTime(LocalDateTime.now());

        return wave;
    }

    /**
     * 生成波次号
     * 格式：W+年月日+3位序号，如 W20260531001
     */
    private String generateWaveNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "W" + dateStr;

        Integer maxSeq = waveRepository.getMaxSeqByDate(prefix);
        int nextSeq = (maxSeq == null) ? 1 : maxSeq + 1;

        return prefix + String.format("%03d", nextSeq);
    }

    /**
     * 更新订单的波次信息
     */
    private void updateOrdersWaveInfo(List<OutboundOrder> orders, Wave wave) {
        for (OutboundOrder order : orders) {
            order.setWaveId(wave.getId());
            order.setWaveNo(wave.getWaveNo());
            orderRepository.updateById(order);
        }
    }

    /**
     * 计算总SKU数
     */
    private int calculateTotalSku(List<OutboundOrder> orders) {
        Set<String> skuSet = new HashSet<>();
        for (OutboundOrder order : orders) {
            LambdaQueryWrapper<OutboundOrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OutboundOrderItem::getOrderId, order.getId());
            List<OutboundOrderItem> items = orderItemRepository.selectList(wrapper);
            items.forEach(item -> skuSet.add(item.getSkuCode()));
        }
        return skuSet.size();
    }

    /**
     * 准备库存分配数据
     */
    private List<AllocationDTO> prepareAllocations(List<OutboundOrder> orders) {
        List<AllocationDTO> allocations = new ArrayList<>();

        for (OutboundOrder order : orders) {
            LambdaQueryWrapper<OutboundOrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OutboundOrderItem::getOrderId, order.getId());
            List<OutboundOrderItem> items = orderItemRepository.selectList(wrapper);

            for (OutboundOrderItem item : items) {
                AllocationDTO allocation = new AllocationDTO();
                allocation.setProductId(item.getProductId());
                allocation.setQty(item.getQty());
                allocation.setOutboundOrderId(order.getId());
                allocations.add(allocation);
            }
        }

        return allocations;
    }

    /**
     * 处理部分释放
     */
    private WaveReleaseResultDTO handlePartialRelease(Wave wave, List<OutboundOrder> orders,
            List<AllocationResultDTO> allocationResults, List<AllocationResultDTO> failures) {
        WaveReleaseResultDTO result = new WaveReleaseResultDTO();

        // 找出成功锁定的订单
        Set<Long> failedProductIds = failures.stream()
            .map(AllocationResultDTO::getProductId)
            .collect(Collectors.toSet());

        int releasedCount = 0;
        int failedCount = 0;

        // 更新订单状态
        for (OutboundOrder order : orders) {
            LambdaQueryWrapper<OutboundOrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OutboundOrderItem::getOrderId, order.getId());
            List<OutboundOrderItem> items = orderItemRepository.selectList(wrapper);

            boolean hasFailure = items.stream()
                .anyMatch(item -> failedProductIds.contains(item.getProductId()));

            if (hasFailure) {
                failedCount++;
            } else {
                order.setStatus(OutboundOrder.STATUS_ALLOCATED);
                orderRepository.updateById(order);
                releasedCount++;
            }
        }

        // 更新波次状态
        if (releasedCount > 0) {
            wave.setStatus(Wave.STATUS_PICKING);
            wave.setStartTime(LocalDateTime.now());
            wave.setTotalOrders(releasedCount);
            waveRepository.updateById(wave);
        }

        result.setSuccess(true);
        result.setPartialRelease(true);
        result.setReleasedOrderCount(releasedCount);
        result.setFailedOrderCount(failedCount);
        result.setNewStatus(Wave.STATUS_PICKING);
        result.setShortageList(buildShortageList(failures));

        return result;
    }

    /**
     * 构建缺货清单
     */
    private List<ShortageItemDTO> buildShortageList(List<AllocationResultDTO> failures) {
        return failures.stream().map(failure -> {
            ShortageItemDTO item = new ShortageItemDTO();
            item.setProductId(failure.getProductId());
            item.setRequiredQty(failure.getRequestedQty());
            item.setAvailableQty(failure.getAvailableBefore());
            item.setShortageQty(failure.getRequestedQty() - failure.getAvailableBefore());
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 释放订单的库存
     */
    private void releaseInventoryForOrders(List<OutboundOrder> orders) {
        for (OutboundOrder order : orders) {
            LambdaQueryWrapper<OutboundOrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OutboundOrderItem::getOrderId, order.getId());
            List<OutboundOrderItem> items = orderItemRepository.selectList(wrapper);

            for (OutboundOrderItem item : items) {
                allocationService.releaseInventory(order.getId(), item.getProductId(), item.getQty());
            }
        }
    }

    /**
     * 生成拣货任务
     */
    private int generatePickTasks(Wave wave, List<OutboundOrder> orders) {
        // TODO: 实现拣货任务生成逻辑
        // 暂时返回订单数量作为任务数
        return orders.size();
    }

    /**
     * 计算高频SKU
     */
    private List<Map<String, Object>> calculateHighFrequencySku(WaveCreateDTO dto) {
        // TODO: 实现高频SKU统计逻辑
        return new ArrayList<>();
    }
}
