package com.wms.returnorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 退货单明细表
 */
@Data
@TableName("wms_return_order_item")
public class ReturnOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退货单ID */
    private Long returnOrderId;

    /** 退货单号 */
    private String returnOrderNo;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 条码 */
    private String barcode;

    /** 原出库数量 */
    private Integer originalQty;

    /** 预计退货数量 */
    private Integer expectedQty;

    /** 实际收货数量 */
    private Integer receivedQty;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
