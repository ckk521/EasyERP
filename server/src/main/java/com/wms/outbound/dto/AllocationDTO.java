package com.wms.outbound.dto;

import lombok.Data;

/**
 * 库存分配DTO
 */
@Data
public class AllocationDTO {

    /** 商品ID */
    private Long productId;

    /** 分配数量 */
    private Integer qty;

    /** 出库单ID */
    private Long outboundOrderId;

    /** 出库单明细ID */
    private Long orderItemId;

    /** 库位ID（指定分配） */
    private Long locationId;

    /** 批次号（指定分配） */
    private String batchNo;

    /** 分配策略: 1先进先出 2按库位优先级 3按批次效期 */
    private Integer strategy;

    // ========== 分配策略常量 ==========
    public static final int STRATEGY_FIFO = 1;           // 先进先出
    public static final int STRATEGY_LOCATION = 2;       // 按库位优先级
    public static final int STRATEGY_EXPIRY = 3;         // 按批次效期
}