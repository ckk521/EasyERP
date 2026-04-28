package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商商品关系表
 * 记录供应商能供应哪些商品，以及对应的采购价、供应商商品编码等
 */
@Data
@TableName("base_supplier_product")
public class BaseSupplierProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long supplierId;

    private String supplierCode;

    private String supplierName;

    private Long productId;

    private String skuCode;

    private String productName;

    private String supplierSkuCode;

    private BigDecimal purchasePrice;

    private BigDecimal minOrderQty;

    private BigDecimal leadTime;

    private Integer status;

    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
