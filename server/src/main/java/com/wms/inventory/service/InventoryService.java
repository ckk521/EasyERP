package com.wms.inventory.service;

import com.wms.inventory.dto.*;
import java.util.List;
import java.util.Map;

/**
 * 库存服务接口
 */
public interface InventoryService {

    /**
     * 实时库存查询（分页）
     * Story 4.1
     */
    Map<String, Object> queryInventory(InventoryQueryDTO query);

    /**
     * 库存明细查询（单个商品分布）
     * Story 4.2
     */
    List<InventoryVO> getProductInventoryDetail(Long productId, Long warehouseId);

    /**
     * 批次库存查询
     * Story 4.3
     */
    List<BatchInventoryVO> queryBatchInventory(InventoryQueryDTO query);

    /**
     * 效期预警监控
     * Story 4.17
     */
    List<ExpiryWarningVO> getExpiryWarnings(Long warehouseId, Integer expiryStatus);

    /**
     * 更新效期状态（定时任务调用）
     */
    void updateExpiryStatus();

    /**
     * 获取库存汇总统计
     */
    Map<String, Object> getInventorySummary(Long warehouseId);

    /**
     * 修复库存批次号（从验收记录同步）
     */
    int fixInventoryBatchNo();
}