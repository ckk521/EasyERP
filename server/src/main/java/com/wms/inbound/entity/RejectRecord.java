package com.wms.inbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 不合格品记录表
 */
@Data
@TableName("wms_reject_record")
public class RejectRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 来源入库单ID */
    private Long inboundOrderId;

    /** 来源入库单号 */
    private String inboundOrderNo;

    /** 来源入库明细ID */
    private Long inboundItemId;

    /** 送货批次号 */
    private String deliveryBatchNo;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商编码 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 不合格数量 */
    private Integer rejectQty;

    /** 不合格类型: 1包装破损 2商品损坏 3错货 4规格不符 5效期问题 6其他 */
    private Integer rejectType;

    /** 不合格原因描述 */
    private String rejectReason;

    /** 不合格照片URL(逗号分隔) */
    private String rejectImages;

    /** 发现环节: 1收货 2验收 */
    private Integer discoverStage;

    /** 处理状态: 0待处理 1已处理 */
    private Integer handleStatus;

    /** 处理方式: 1退货供应商 2降价销售 3报损 4内部使用 5销毁 */
    private Integer handleType;

    /** 处理数量 */
    private Integer handleQty;

    /** 处理备注 */
    private String handleRemark;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理人 */
    private Long handleUser;

    /** 退货单号 */
    private String returnOrderNo;

    /** 退货时间 */
    private LocalDateTime returnTime;

    /** 退货备注 */
    private String returnRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 常量 ==========
    public static final int DISCOVER_STAGE_RECEIVE = 1;   // 收货时发现
    public static final int DISCOVER_STAGE_INSPECT = 2;   // 验收时发现

    public static final int HANDLE_STATUS_PENDING = 0;    // 待处理
    public static final int HANDLE_STATUS_DONE = 1;       // 已处理

    public static final int HANDLE_TYPE_RETURN = 1;       // 退货供应商
    public static final int HANDLE_TYPE_DISCOUNT = 2;     // 降价销售
    public static final int HANDLE_TYPE_SCRAP = 3;        // 报损
    public static final int HANDLE_TYPE_INTERNAL = 4;     // 内部使用
    public static final int HANDLE_TYPE_DESTROY = 5;      // 销毁
}
