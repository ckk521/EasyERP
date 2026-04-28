package com.wms.inbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 收货记录表
 * 记录每次收货操作，支持追溯
 */
@Data
@TableName("wms_receive_record")
public class ReceiveRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 来源入库单ID，关联 wms_inbound_order.id */
    private Long inboundOrderId;

    /** 来源入库单号 */
    private String inboundOrderNo;

    /** 来源入库明细ID，关联 wms_inbound_order_item.id */
    private Long inboundItemId;

    /** 商品ID，关联 base_product.id */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 本次收货数量 */
    private Integer receiveQty;

    /** 本次差异数量(与待收货数量的差异) */
    private Integer diffQty;

    /** 差异原因 */
    private String diffReason;

    /** 是否有异常: 0否 1是 */
    private Integer hasException;

    /** 异常类型: 包装破损/商品损坏/错货/串货 */
    private String exceptionType;

    /** 异常描述 */
    private String exceptionDesc;

    /** 异常照片URL(逗号分隔) */
    private String exceptionImages;

    /** 收货时间 */
    private LocalDateTime receiveTime;

    /** 收货人ID */
    private Long receiveUser;

    /** 收货人姓名 */
    private String receiveUserName;
}
