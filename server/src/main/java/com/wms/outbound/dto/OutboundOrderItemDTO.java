package com.wms.outbound.dto;

import lombok.Data;
import javax.validation.constraints.*;

/**
 * 出库单明细DTO
 */
@Data
public class OutboundOrderItemDTO {

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    @NotBlank(message = "SKU编码不能为空")
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 商品条码 */
    private String barcode;

    /** 出库数量 */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer qty;

    /** 库位ID(指定出库库位) */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 批次号(指定出库批次) */
    private String batchNo;
}