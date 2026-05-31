package com.wms.stocktake.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 盘点明细表
 */
@Data
@TableName("wms_stocktake_item")
public class StocktakeItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 盘点单ID */
    private Long orderId;

    /** 盘点单号 */
    private String orderNo;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 条码 */
    private String barcode;

    /** 库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 批次号 */
    private String batchNo;

    /** 系统数量 */
    private Integer systemQty;

    /** 盘点数量 */
    private Integer countedQty;

    /** 差异数量 */
    private Integer diffQty;

    /** 差异原因: profit/loss/wrong/missed/other */
    private String diffReason;

    /** 差异说明 */
    private String diffRemark;

    /** 状态: 0待盘点 1已盘点 2已确认 */
    private Integer status;

    /** 盘点轮次: 1初盘 2复盘 */
    private Integer roundNo;

    /** 盘点人ID */
    private Long countUserId;

    /** 盘点人姓名 */
    private String countUserName;

    /** 盘点时间 */
    private LocalDateTime countTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;     // 待盘点
    public static final int STATUS_COUNTED = 1;     // 已盘点
    public static final int STATUS_CONFIRMED = 2;   // 已确认

    // ========== 差异原因常量 ==========
    public static final String DIFF_PROFIT = "profit";     // 盘盈
    public static final String DIFF_LOSS = "loss";         // 盘亏
    public static final String DIFF_WRONG = "wrong";       // 错放
    public static final String DIFF_MISSED = "missed";     // 漏扫
    public static final String DIFF_OTHER = "other";       // 其他
}
