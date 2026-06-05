package com.wms.outbound.dto;

import lombok.Data;

/**
 * 缺货明细DTO
 */
@Data
public class ShortageItemDTO {

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 需求数量 */
    private Integer requiredQty;

    /** 可用库存 */
    private Integer availableQty;

    /** 缺货数量 */
    private Integer shortageQty;

    /** 订单号列表 */
    private String orderNos;
}
