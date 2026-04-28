package com.wms.inbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 验收记录表
 * 记录每次验收操作，支持追溯
 */
@Data
@TableName("wms_inspect_record")
public class InspectRecord {

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

    /** 本次验收数量 */
    private Integer inspectQty;

    /** 本次合格数量 */
    private Integer qualifiedQty;

    /** 本次不合格数量 */
    private Integer rejectedQty;

    /** 批次号(验收时生成) */
    private String batchNo;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 过期日期 */
    private LocalDate expiryDate;

    /** 验收时间 */
    private LocalDateTime inspectTime;

    /** 验收人ID */
    private Long inspectUser;

    /** 验收人姓名 */
    private String inspectUserName;
}
