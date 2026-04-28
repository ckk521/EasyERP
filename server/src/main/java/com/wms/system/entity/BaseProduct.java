package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("base_product")
public class BaseProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String skuCode;

    private String barcode;

    private String nameCn;

    private String nameEn;

    private Long categoryId;

    private String categoryName;

    private String brand;

    private BigDecimal weight;

    private BigDecimal length;

    private BigDecimal width;

    private BigDecimal height;

    private BigDecimal volume;

    private String mainImage;

    private String images;

    private String description;

    private Integer storageCond;

    private BigDecimal humidityMin;

    private BigDecimal humidityMax;

    private Integer shelfLife;

    private Integer expiryWarning;

    private Integer isDangerous;

    private Integer isFragile;

    private Integer isHighValue;

    private BigDecimal highValueThreshold;

    private Integer securityLevel;

    private Integer needExpiryMgmt;

    private Integer status;

    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
