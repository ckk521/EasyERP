package com.wms.exception.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 异常处理明细表
 */
@Data
@TableName("wms_exception_item")
public class ExceptionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 异常处理单ID */
    private Long orderId;

    /** 异常处理单号 */
    private String orderNo;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 条码 */
    private String barcode;

    /** 批次号 */
    private String batchNo;

    /** 异常数量 */
    private Integer exceptionQty;

    /** 异常类型 */
    private Integer exceptionType;

    /** 异常原因 */
    private String exceptionReason;

    /** 隔离库位ID */
    private Long locationId;

    /** 隔离库位编码 */
    private String locationCode;

    /** 状态: 0待处理 1已隔离 2已处理 */
    private Integer status;

    /** 处理方式 */
    private Integer handleType;

    /** 处理数量 */
    private Integer handleQty;

    /** 处理结果 */
    private String handleResult;

    /** 关联入库明细ID */
    private Long inboundItemId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 待处理
    public static final int STATUS_ISOLATED = 1;     // 已隔离
    public static final int STATUS_HANDLED = 2;      // 已处理
}
