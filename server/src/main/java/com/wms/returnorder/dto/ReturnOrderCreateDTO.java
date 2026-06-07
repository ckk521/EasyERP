package com.wms.returnorder.dto;

import lombok.Data;
import java.util.List;

/**
 * 创建退货单DTO
 */
@Data
public class ReturnOrderCreateDTO {

    /** 原出库单ID */
    private Long originalOutboundId;

    /** 原出库单号 */
    private String originalOutboundNo;

    /** 退货原因: 1质量问题 2发错货 3数量不符 4不满意 5无理由 6其他 */
    private Integer returnReason;

    /** 退货原因详细说明 */
    private String returnReasonText;

    /** 退货商品明细 */
    private List<ReturnItemDTO> items;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    private Long createUserId;

    /** 创建人姓名 */
    private String createUserName;

    /**
     * 退货商品明细DTO
     */
    @Data
    public static class ReturnItemDTO {
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

        /** 退货数量 */
        private Integer expectedQty;

        /** 备注 */
        private String remark;
    }
}