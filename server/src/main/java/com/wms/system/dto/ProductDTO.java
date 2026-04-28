package com.wms.system.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductDTO {
    private String skuCode;
    private String barcode;
    private String nameCn;
    private String nameEn;
    private Long categoryId;
    private String brand;
    private BigDecimal weight;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private String mainImage;
    private String description;
    private Integer storageCond;
    private Integer shelfLife;
    private Integer expiryWarning;
    private Integer isDangerous;
    private Integer isFragile;
    private Integer isHighValue;
    private Integer needExpiryMgmt;
}
