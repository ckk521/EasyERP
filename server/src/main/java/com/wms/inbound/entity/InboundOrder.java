package com.wms.inbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入库单主表
 */
@Data
@TableName("wms_inbound_order")
public class InboundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 入库单号 IN202604210001 */
    private String orderNo;

    /** 送货批次号 SZ-0421-AM */
    private String deliveryBatchNo;

    /** 入库类型: 1采购 2退货 3调拨 4赠品 5其他 */
    private Integer orderType;

    /** 采购订单号(采购入库) */
    private String poNo;

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

    /** 仓库名称 */
    private String warehouseName;

    /** 预计到货日期 */
    private LocalDate expectedDate;

    /** 实际到货日期 */
    private LocalDate actualArrivalDate;

    /** 状态: 0待收货 1收货中 2验收中 3待上架 4已完成 9已取消 */
    private Integer status;

    /** 收货进度百分比 */
    private Integer progressReceive;

    /** 验收进度百分比 */
    private Integer progressInspect;

    /** 上架进度百分比 */
    private Integer progressPutaway;

    /** 总预期数量 */
    private Integer totalExpectedQty;

    /** 总收货数量 */
    private Integer totalReceivedQty;

    /** 总合格数量 */
    private Integer totalQualifiedQty;

    /** 总不合格数量 */
    private Integer totalRejectedQty;

    /** 总上架数量 */
    private Integer totalPutawayQty;

    /** 总退货数量 */
    private Integer totalReturnQty;

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

    /** 完成时间 */
    private LocalDateTime completeTime;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 待收货
    public static final int STATUS_RECEIVING = 1;    // 收货中
    public static final int STATUS_INSPECTING = 2;   // 验收中
    public static final int STATUS_PUTAWAY = 3;      // 待上架
    public static final int STATUS_COMPLETED = 4;    // 已完成
    public static final int STATUS_CANCELLED = 9;    // 已取消

    // ========== 入库类型常量 ==========
    public static final int TYPE_PURCHASE = 1;       // 采购入库
    public static final int TYPE_RETURN = 2;         // 退货入库
    public static final int TYPE_TRANSFER = 3;       // 调拨入库
    public static final int TYPE_GIFT = 4;           // 赠品入库
    public static final int TYPE_OTHER = 5;          // 其他入库
}
