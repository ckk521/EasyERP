package com.wms.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 拣货记录表
 *
 * 状态流转：待拣货(0) → 拣货中(1) → 已完成(2) / 异常(3) / 已取消(9)
 */
@Data
@TableName("wms_pick_record")
public class PickRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 出库单ID */
    private Long outboundOrderId;

    /** 出库单号 */
    private String outboundOrderNo;

    /** 出库明细ID */
    private Long outboundItemId;

    /** 波次ID */
    private Long waveId;

    /** 波次号 */
    private String waveNo;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 商品条码 */
    private String barcode;

    /** 仓库ID */
    private Long warehouseId;

    /** 库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 批次号 */
    private String batchNo;

    /** 计划拣货数量 */
    private Integer planQty;

    /** 实际拣货数量 */
    private Integer actualQty;

    /** 差异数量 */
    private Integer diffQty;

    /** 库位扫码确认: 0未扫码 1已扫码 */
    private Integer locationScanned;

    /** 商品扫码确认: 0未扫码 1已扫码 */
    private Integer productScanned;

    /** 状态: 0待拣货 1拣货中 2已完成 3异常 9已取消 */
    private Integer status;

    /** 是否异常: 0正常 1异常 */
    private Integer isException;

    /** 异常类型: 1缺货 2破损 3错货 4其他 */
    private Integer exceptionType;

    /** 异常数量 */
    private Integer exceptionQty;

    /** 异常备注 */
    private String exceptionRemark;

    /** 差异原因 */
    private String diffReason;

    /** 拣货人ID */
    private Long pickUserId;

    /** 拣货人姓名 */
    private String pickUserName;

    /** 领取时间 */
    private LocalDateTime claimTime;

    /** 开始拣货时间 */
    private LocalDateTime startTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 拣货顺序序号 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 待拣货
    public static final int STATUS_PICKING = 1;      // 拣货中
    public static final int STATUS_COMPLETED = 2;    // 已完成
    public static final int STATUS_EXCEPTION = 3;    // 异常
    public static final int STATUS_CANCELLED = 9;    // 已取消

    // ========== 异常类型常量 ==========
    public static final int EXCEPTION_SHORTAGE = 1;  // 缺货
    public static final int EXCEPTION_DAMAGED = 2;   // 破损
    public static final int EXCEPTION_WRONG = 3;     // 错货
    public static final int EXCEPTION_OTHER = 4;     // 其他

    // ========== 扫码状态常量 ==========
    public static final int SCAN_NO = 0;   // 未扫码
    public static final int SCAN_YES = 1;  // 已扫码
}
