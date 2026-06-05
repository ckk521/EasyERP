package com.wms.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存分配表（库存锁定记录）
 *
 * 状态流转：已锁定(0) → 已拣货(1) → 已发货(2) → 已释放(3)
 */
@Data
@TableName("wms_inventory_allocation")
public class InventoryAllocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 出库单ID */
    private Long outboundOrderId;

    /** 出库单号 */
    private String outboundOrderNo;

    /** 出库明细ID */
    private Long outboundItemId;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 仓库ID */
    private Long warehouseId;

    /** 库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 批次号 */
    private String batchNo;

    /** 分配数量（锁定数量） */
    private Integer allocatedQty;

    /** 已拣数量 */
    private Integer pickedQty;

    /** 已发货数量 */
    private Integer shippedQty;

    /** 状态: 0已锁定 1已拣货 2已发货 3已释放 */
    private Integer status;

    /** 波次ID */
    private Long waveId;

    /** 波次号 */
    private String waveNo;

    /** 分配时间 */
    private LocalDateTime allocateTime;

    /** 拣货时间 */
    private LocalDateTime pickTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 释放时间 */
    private LocalDateTime releaseTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 状态常量 ==========
    public static final int STATUS_LOCKED = 0;      // 已锁定
    public static final int STATUS_PICKED = 1;      // 已拣货
    public static final int STATUS_SHIPPED = 2;     // 已发货
    public static final int STATUS_RELEASED = 3;    // 已释放
}