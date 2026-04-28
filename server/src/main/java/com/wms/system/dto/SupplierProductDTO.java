package com.wms.system.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SupplierProductDTO {
    private Long supplierId;
    private Long productId;
    private String supplierSkuCode;
    private BigDecimal purchasePrice;
    private BigDecimal minOrderQty;
    private BigDecimal leadTime;
}
