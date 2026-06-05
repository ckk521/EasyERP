package com.wms.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 打包明细表
 */
@Data
@TableName("wms_pack_item")
public class PackItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 打包记录ID */
    private Long packRecordId;

    /** 包裹号 */
    private String packageNo;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 商品条码 */
    private String barcode;

    /** 打包数量 */
    private Integer qty;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
