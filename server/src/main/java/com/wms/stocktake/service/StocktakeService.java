package com.wms.stocktake.service;

import com.wms.stocktake.dto.*;
import com.wms.stocktake.entity.StocktakeItem;
import java.util.List;
import java.util.Map;

/**
 * 盘点服务接口
 */
public interface StocktakeService {

    /**
     * 创建盘点单
     * Story 4.4
     */
    Long createOrder(StocktakeCreateDTO dto, Long userId, String username);

    /**
     * 盘点作业
     * Story 4.5
     */
    void countItem(StocktakeCountDTO dto, Long userId, String username);

    /**
     * 完成盘点
     * Story 4.6
     */
    void finishOrder(Long orderId);

    /**
     * 查询盘点单列表
     * Story 4.7
     */
    Map<String, Object> queryOrders(StocktakeQueryDTO query);

    /**
     * 获取盘点单详情
     */
    Map<String, Object> getOrderDetail(Long orderId);

    /**
     * 获取盘点明细列表
     */
    List<StocktakeItem> getOrderItems(Long orderId);

    /**
     * 取消盘点单
     * @param orderId 订单ID
     * @param reason 取消原因
     */
    void cancelOrder(Long orderId, String reason);

    /**
     * 强制取消盘点单（可取消任意状态）
     * @param orderId 订单ID
     * @param reason 取消原因
     */
    void forceCancelOrder(Long orderId, String reason);

    /**
     * 手动触发生成循环盘数据
     * @param orderId 订单ID
     * @param userId 操作人ID
     * @param username 操作人姓名
     */
    void generateCycleData(Long orderId, Long userId, String username);

    /**
     * 审核盘点单（通过）
     * @param orderId 订单ID
     * @param userId 审核人ID
     * @param username 审核人姓名
     */
    void approveOrder(Long orderId, Long userId, String username);

    /**
     * 审核盘点单（驳回）
     * @param orderId 订单ID
     * @param reason 驳回原因
     * @param userId 审核人ID
     * @param username 审核人姓名
     */
    void rejectOrder(Long orderId, String reason, Long userId, String username);

    /**
     * 查询审核记录
     * @param orderId 订单ID
     * @return 审核记录列表
     */
    List<Map<String, Object>> getApproveRecords(Long orderId);
}
