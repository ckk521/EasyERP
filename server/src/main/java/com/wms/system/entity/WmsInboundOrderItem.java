package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入库单明细表
 */
@Data
@TableName("wms_inbound_order_item")
public class WmsInboundOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 入库单ID */
    private Long orderId;

    /** 入库单号 */
    private String orderNo;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 商品条码 */
    private String barcode;

    /** 预期数量 */
    private Integer expectedQty;

    /** 收货数量 */
    private Integer receivedQty;

    /** 合格数量 */
    private Integer qualifiedQty;

    /** 不合格数量 */
    private Integer rejectedQty;

    /** 上架数量 */
    private Integer putawayQty;

    /** 退货数量 */
    private Integer returnQty;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 过期日期 */
    private LocalDate expiryDate;

    /** 状态: 0待收货 1已收货 2已验收 3已上架 4部分退货 5全部退货 */
    private Integer status;

    /** 收货差异原因 */
    private String diffReason;

    /** 收货时间 */
    private LocalDateTime receiveTime;

    /** 收货人 */
    private Long receiveUser;

    /** 验收时间 */
    private LocalDateTime inspectTime;

    /** 验收人 */
    private Long inspectUser;

    /** 上架时间 */
    private LocalDateTime putawayTime;

    /** 上架人 */
    private Long putawayUser;

    // ========== 常量定义 ==========

    /** 状态: 待收货 */
    public static final int STATUS_PENDING = 0;
    /** 状态: 已收货 */
    public static final int STATUS_RECEIVED = 1;
    /** 状态: 已验收 */
    public static final int STATUS_INSPECTED = 2;
    /** 状态: 已上架 */
    public static final int STATUS_PUTAWAY = 3;
    /** 状态: 部分退货 */
    public static final int STATUS_PARTIAL_RETURN = 4;
    /** 状态: 全部退货 */
    public static final int STATUS_FULL_RETURN = 5;
}
