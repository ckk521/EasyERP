package com.wms.outbound.dto;

import lombok.Data;
import java.util.List;

/**
 * 拣货完成结果DTO
 */
@Data
public class PickCompleteResultDTO {

    /** 是否成功 */
    private Boolean success;

    /** 失败原因 */
    private String failReason;

    /** 出库单ID */
    private Long outboundOrderId;

    /** 出库单号 */
    private String outboundOrderNo;

    /** 新状态 */
    private Integer newStatus;

    /** 是否有差异 */
    private Boolean hasDifference;

    /** 差异明细 */
    private List<PickDifferenceDTO> differenceList;

    /**
     * 拣货差异明细
     */
    @Data
    public static class PickDifferenceDTO {

        /** 商品ID */
        private Long productId;

        /** SKU编码 */
        private String skuCode;

        /** 商品名称 */
        private String productName;

        /** 计划数量 */
        private Integer planQty;

        /** 实际数量 */
        private Integer actualQty;

        /** 差异数量 */
        private Integer diffQty;

        /** 差异原因 */
        private String diffReason;

        /** 是否异常 */
        private Boolean isException;

        /** 异常类型 */
        private Integer exceptionType;
    }
}