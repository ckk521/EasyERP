package com.wms.returnorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 退货单主表
 */
@Data
@TableName("wms_return_order")
public class ReturnOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退货单号 RT202606060001 */
    private String returnNo;

    /** 原出库单ID */
    private Long originalOutboundId;

    /** 原出库单号 */
    private String originalOutboundNo;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 退货原因: 1质量问题 2发错货 3数量不符 4不满意 5无理由 6其他 */
    private Integer returnReason;

    /** 退货原因详细说明 */
    private String returnReasonText;

    /** 状态: 0待收货 1已收货 2已完成 9已取消 */
    private Integer status;

    /** 预计退货数量 */
    private Integer totalExpectedQty;

    /** 实际收货数量 */
    private Integer totalReceivedQty;

    /** 生成的入库单ID */
    private Long inboundOrderId;

    /** 生成的入库单号 */
    private String inboundOrderNo;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 取消原因 */
    private String cancelReason;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    private Long createUserId;

    /** 创建人姓名 */
    private String createUserName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 收货时间 */
    private LocalDateTime receiveTime;

    /** 收货人ID */
    private Long receiveUserId;

    /** 收货人姓名 */
    private String receiveUserName;

    /** 完成时间 */
    private LocalDateTime completeTime;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 待收货
    public static final int STATUS_RECEIVED = 1;     // 已收货
    public static final int STATUS_COMPLETED = 2;    // 已完成
    public static final int STATUS_CANCELLED = 9;    // 已取消

    // ========== 退货原因常量 ==========
    public static final int REASON_QUALITY = 1;       // 质量问题
    public static final int REASON_WRONG_GOODS = 2;   // 发错货
    public static final int REASON_QTY_MISMATCH = 3;  // 数量不符
    public static final int REASON_NOT_SATISFIED = 4; // 不满意
    public static final int REASON_NO_REASON = 5;     // 无理由
    public static final int REASON_OTHER = 6;         // 其他
}
