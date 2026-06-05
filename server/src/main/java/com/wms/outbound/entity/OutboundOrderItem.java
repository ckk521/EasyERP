package com.wms.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 出库单明细表
 */
@Data
@TableName("wms_outbound_order_item")
public class OutboundOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 出库单ID */
    private Long orderId;

    /** 出库单号 */
    private String orderNo;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 商品条码 */
    private String barcode;

    /** 应出数量 */
    private Integer qty;

    /** 已拣数量 */
    private Integer pickedQty;

    /** 已打包数量 */
    private Integer packedQty;

    /** 已发货数量 */
    private Integer shippedQty;

    /** 库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 批次号 */
    private String batchNo;

    /** 状态: 0待拣货 1拣货中 2已拣货 3已打包 4已发货 9已取消 */
    private Integer status;

    /** 差异原因 */
    private String diffReason;

    /** 拣货时间 */
    private LocalDateTime pickTime;

    /** 打包时间 */
    private LocalDateTime packTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 待拣货
    public static final int STATUS_PICKING = 1;      // 拣货中
    public static final int STATUS_PICKED = 2;       // 已拣货
    public static final int STATUS_PACKED = 3;       // 已打包
    public static final int STATUS_SHIPPED = 4;      // 已发货
    public static final int STATUS_CANCELLED = 9;    // 已取消
}