package com.wms.inbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存事务表
 */
@Data
@TableName("wms_inventory_transaction")
public class InventoryTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事务单号 */
    private String transactionNo;

    /** 事务类型: 1入库 2出库 3调拨入 4调拨出 5盘点调整 6退货 */
    private Integer transactionType;

    /** 仓库ID */
    private Long warehouseId;

    /** 库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 批次号 */
    private String batchNo;

    /** 变动数量(正为增,负为减) */
    private Integer qtyChange;

    /** 变动前数量 */
    private Integer qtyBefore;

    /** 变动后数量 */
    private Integer qtyAfter;

    /** 关联单据类型: INBOUND/OUTBOUND/TRANSFER/STOCKTAKE/RETURN */
    private String refOrderType;

    /** 关联单据ID */
    private Long refOrderId;

    /** 关联单据号 */
    private String refOrderNo;

    /** 备注 */
    private String remark;

    /** 操作人 */
    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ========== 事务类型常量 ==========
    public static final int TYPE_INBOUND = 1;        // 入库
    public static final int TYPE_OUTBOUND = 2;       // 出库
    public static final int TYPE_TRANSFER_IN = 3;    // 调拨入
    public static final int TYPE_TRANSFER_OUT = 4;   // 调拨出
    public static final int TYPE_STOCKTAKE = 5;      // 盘点调整
    public static final int TYPE_RETURN = 6;         // 退货

    // ========== 关联单据类型 ==========
    public static final String REF_TYPE_INBOUND = "INBOUND";
    public static final String REF_TYPE_OUTBOUND = "OUTBOUND";
    public static final String REF_TYPE_TRANSFER = "TRANSFER";
    public static final String REF_TYPE_STOCKTAKE = "STOCKTAKE";
    public static final String REF_TYPE_RETURN = "RETURN";
}
