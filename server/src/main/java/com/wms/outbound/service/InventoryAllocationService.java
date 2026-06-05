package com.wms.outbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wms.inbound.entity.Inventory;
import com.wms.outbound.dto.AllocationDTO;
import com.wms.outbound.dto.AllocationResultDTO;
import com.wms.inbound.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 库存分配服务
 *
 * 实现功能：
 * 1. 库存锁定（预占）
 * 2. 库存扣减（发货）
 * 3. 库存释放（取消）
 * 4. 批量分配
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAllocationService {

    private final InventoryRepository inventoryRepository;

    /**
     * 锁定库存
     * 库存状态变化：available_qty减少，locked_qty增加
     */
    @Transactional
    public AllocationResultDTO lockInventory(AllocationDTO dto) {
        AllocationResultDTO result = new AllocationResultDTO();
        result.setProductId(dto.getProductId());
        result.setRequestedQty(dto.getQty());

        // 查询商品库存
        Inventory inventory = getAvailableInventory(dto.getProductId(), dto.getLocationId());
        if (inventory == null) {
            result.setSuccess(false);
            result.setFailReason("库存记录不存在");
            return result;
        }

        Integer availableBefore = inventory.getAvailableQty();
        Integer lockedBefore = inventory.getLockedQty();

        // 检查可用库存是否充足
        if (availableBefore < dto.getQty()) {
            result.setSuccess(false);
            result.setFailReason("库存不足，当前可用库存：" + availableBefore + "件");
            result.setAvailableBefore(availableBefore);
            return result;
        }

        // 锁定库存：available_qty减少，locked_qty增加
        inventory.setAvailableQty(availableBefore - dto.getQty());
        inventory.setLockedQty(lockedBefore + dto.getQty());
        inventoryRepository.updateById(inventory);

        // 设置返回结果
        result.setSuccess(true);
        result.setAllocatedQty(dto.getQty());
        result.setLocationId(inventory.getLocationId());
        result.setLocationCode(inventory.getLocationCode());
        result.setBatchNo(inventory.getBatchNo());
        result.setAvailableBefore(availableBefore);
        result.setAvailableAfter(inventory.getAvailableQty());
        result.setLockedBefore(lockedBefore);
        result.setLockedAfter(inventory.getLockedQty());

        log.info("库存锁定成功: productId={}, qty={}, available={}, locked={}",
            dto.getProductId(), dto.getQty(), inventory.getAvailableQty(), inventory.getLockedQty());

        return result;
    }

    /**
     * 扣减库存
     * 库存状态变化：qty_total减少，locked_qty减少
     */
    @Transactional
    public boolean deductInventory(Long outboundOrderId, Long productId, Integer qty) {
        // 查询锁定库存
        Inventory inventory = getLockedInventory(productId, outboundOrderId);
        if (inventory == null) {
            log.warn("未找到锁定库存: productId={}, outboundOrderId={}", productId, outboundOrderId);
            return false;
        }

        // 扣减库存：qty_total减少，locked_qty减少
        inventory.setQty(inventory.getQty() - qty);
        inventory.setLockedQty(inventory.getLockedQty() - qty);
        inventoryRepository.updateById(inventory);

        log.info("库存扣减成功: productId={}, qty={}, total={}, locked={}",
            productId, qty, inventory.getQty(), inventory.getLockedQty());

        return true;
    }

    /**
     * 释放库存
     * 库存状态变化：locked_qty减少，available_qty增加
     */
    @Transactional
    public boolean releaseInventory(Long outboundOrderId, Long productId, Integer qty) {
        // 查询锁定库存
        Inventory inventory = getLockedInventory(productId, outboundOrderId);
        if (inventory == null) {
            log.warn("未找到锁定库存: productId={}, outboundOrderId={}", productId, outboundOrderId);
            return false;
        }

        // 释放库存：locked_qty减少，available_qty增加
        inventory.setLockedQty(inventory.getLockedQty() - qty);
        inventory.setAvailableQty(inventory.getAvailableQty() + qty);
        inventoryRepository.updateById(inventory);

        log.info("库存释放成功: productId={}, qty={}, available={}, locked={}",
            productId, qty, inventory.getAvailableQty(), inventory.getLockedQty());

        return true;
    }

    /**
     * 批量锁定库存
     */
    @Transactional
    public List<AllocationResultDTO> batchLockInventory(List<AllocationDTO> allocations) {
        List<AllocationResultDTO> results = new ArrayList<>();

        for (AllocationDTO dto : allocations) {
            AllocationResultDTO result = lockInventory(dto);
            results.add(result);

            // 如果某个商品锁定失败，记录日志但继续处理其他商品
            if (!result.getSuccess()) {
                log.warn("批量锁定库存部分失败: productId={}, reason={}",
                    dto.getProductId(), result.getFailReason());
            }
        }

        // 统计结果
        long successCount = results.stream().filter(AllocationResultDTO::getSuccess).count();
        log.info("批量锁定库存完成: 总数={}, 成功={}, 失败={}",
            allocations.size(), successCount, allocations.size() - successCount);

        return results;
    }

    /**
     * 获取可用库存记录
     */
    private Inventory getAvailableInventory(Long productId, Long locationId) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getProductId, productId);
        if (locationId != null) {
            wrapper.eq(Inventory::getLocationId, locationId);
        }
        wrapper.gt(Inventory::getAvailableQty, 0);
        wrapper.orderByAsc(Inventory::getInboundTime); // 先进先出

        List<Inventory> inventories = inventoryRepository.selectList(wrapper);
        return inventories.isEmpty() ? null : inventories.get(0);
    }

    /**
     * 获取锁定库存记录
     */
    private Inventory getLockedInventory(Long productId, Long outboundOrderId) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getProductId, productId);
        wrapper.gt(Inventory::getLockedQty, 0);

        List<Inventory> inventories = inventoryRepository.selectList(wrapper);
        return inventories.isEmpty() ? null : inventories.get(0);
    }
}