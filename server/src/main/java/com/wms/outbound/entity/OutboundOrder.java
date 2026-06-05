package com.wms.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出库单主表
 *
 * 状态流转：待分配(0) → 已分配(1) → 拣货中(2) → 待打包(3) → 待发货(4) → 已发货(5) → 已取消(9)
 */
@Data
@TableName("wms_outbound_order")
public class OutboundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 出库单号 OB20260531001 */
    private String orderNo;

    /** 出库类型: 1销售 2调拨 3退货 4报废 5样品 */
    private Integer orderType;

    /** 来源类型: 1ERP推送 2手工创建 3调拨申请 */
    private Integer sourceType;

    /** 销售订单号(销售出库时填写) */
    private String soNo;

    /** 客户ID */
    private Long customerId;

    /** 客户编码 */
    private String customerCode;

    /** 客户名称 */
    private String customerName;

    private String customerPhone;

    private String customerAddress;

    private Long supplierId;

    private String supplierCode;

    private String supplierName;

    private Long targetWarehouseId;

    private String targetWarehouseCode;

    private String targetWarehouseName;

    /** 调拨入库单ID(调拨出库时自动生成) */
    private Long transferInboundId;

    /** 调拨入库单号 */
    private String transferInboundNo;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 优先级: 1紧急 2高 3中 4低 */
    private Integer priority;

    /** 物流公司 */
    private String logisticsCompany;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

    /** 状态: 0待分配 1已分配 2拣货中 3待打包 4待发货 5已发货 9已取消 */
    private Integer status;

    /** 总出库数量 */
    private Integer totalQty;

    /** 总拣货数量 */
    private Integer totalPickedQty;

    /** 总打包数量 */
    private Integer totalPackedQty;

    /** 总发货数量 */
    private Integer totalShippedQty;

    /** 波次ID */
    private Long waveId;

    /** 波次号 */
    private String waveNo;

    /** 发货人ID */
    private Long shipUserId;

    /** 发货人姓名 */
    private String shipUserName;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 物流单号 */
    private String trackingNo;

    /** 实际发货时间 */
    private LocalDateTime shippedTime;

    /** 备注 */
    private String remark;

    /** 取消原因 */
    private String cancelReason;

    /** 创建人 */
    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime completeTime;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 待分配
    public static final int STATUS_ALLOCATED = 1;    // 已分配
    public static final int STATUS_PICKING = 2;      // 拣货中
    public static final int STATUS_PACKING = 3;      // 待打包
    public static final int STATUS_SHIPPING = 4;     // 待发货
    public static final int STATUS_SHIPPED = 5;      // 已发货
    public static final int STATUS_CANCELLED = 9;    // 已取消

    // ========== 出库类型常量 ==========
    public static final int TYPE_SALES = 1;          // 销售出库
    public static final int TYPE_TRANSFER = 2;       // 调拨出库
    public static final int TYPE_RETURN = 3;         // 退货出库
    public static final int TYPE_SCRAP = 4;          // 报废出库
    public static final int TYPE_SAMPLE = 5;         // 样品出库

    // ========== 来源类型常量 ==========
    public static final int SOURCE_ERP = 1;          // ERP推送
    public static final int SOURCE_MANUAL = 2;       // 手工创建
    public static final int SOURCE_TRANSFER = 3;     // 调拨申请

    // ========== 优先级常量 ==========
    public static final int PRIORITY_URGENT = 1;     // 紧急
    public static final int PRIORITY_HIGH = 2;       // 高
    public static final int PRIORITY_NORMAL = 3;     // 中
    public static final int PRIORITY_LOW = 4;        // 低
}