package com.wms.exception.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 异常处理单主表
 */
@Data
@TableName("wms_exception_order")
public class ExceptionOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 异常处理单号 EX202605070001 */
    private String orderNo;

    /** 关联入库单ID */
    private Long inboundOrderId;

    /** 入库单号 */
    private String inboundOrderNo;

    /** 关联采购订单ID */
    private Long purchaseOrderId;

    /** 采购订单号 */
    private String purchaseOrderNo;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商编码 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库编码 */
    private String warehouseCode;

    /** 隔离库区ID */
    private Long zoneId;

    /** 隔离库区编码 */
    private String zoneCode;

    /** 异常类型: 1破损 2短缺 3质量不合格 4错货 5其他 */
    private Integer exceptionType;

    /** 异常总数量 */
    private Integer totalQty;

    /** 异常原因说明 */
    private String exceptionReason;

    /** 状态: 0待处理 1处理中 2已完成 3已取消 */
    private Integer status;

    /** 处理方式: 1退货 2换货 3报废 4降价销售 */
    private Integer handleType;

    /** 处理结果说明 */
    private String handleResult;

    /** 处理完成时间 */
    private LocalDateTime handleTime;

    /** 处理人ID */
    private Long handleUserId;

    /** 处理人姓名 */
    private String handleUserName;

    /** 来源类型: 1收货异常 2验收异常 */
    private Integer sourceType;

    /** 补货入库单ID（供应商补货后关联） */
    private Long replacementInboundOrderId;

    /** 补货入库单号 */
    private String replacementInboundOrderNo;

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

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 待处理
    public static final int STATUS_PROCESSING = 1;   // 处理中
    public static final int STATUS_COMPLETED = 2;    // 已完成
    public static final int STATUS_CANCELLED = 3;    // 已取消

    // ========== 异常类型常量 ==========
    public static final int TYPE_DAMAGED = 1;        // 破损
    public static final int TYPE_SHORTAGE = 2;       // 短缺
    public static final int TYPE_QUALITY = 3;        // 质量不合格
    public static final int TYPE_WRONG = 4;          // 错货
    public static final int TYPE_OTHER = 5;          // 其他

    // ========== 处理方式常量 ==========
    public static final int HANDLE_RETURN = 1;       // 退货
    public static final int HANDLE_EXCHANGE = 2;     // 换货
    public static final int HANDLE_SCRAP = 3;        // 报废
    public static final int HANDLE_DISCOUNT = 4;     // 降价销售

    // ========== 来源类型常量 ==========
    public static final int SOURCE_RECEIVE = 1;      // 收货异常
    public static final int SOURCE_INSPECT = 2;      // 验收异常
}
