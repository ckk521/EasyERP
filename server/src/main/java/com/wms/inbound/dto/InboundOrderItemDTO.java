package com.wms.inbound.dto;

import lombok.Data;
import javax.validation.constraints.*;

/**
 * 入库单明细DTO
 */
@Data
public class InboundOrderItemDTO {

    /** 商品ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** SKU编码 */
    @NotBlank(message = "SKU编码不能为空")
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 商品条码 */
    private String barcode;

    /** 预期数量 */
    @NotNull(message = "预期数量不能为空")
    @Min(value = 1, message = "预期数量必须大于0")
    private Integer expectedQty;
}
