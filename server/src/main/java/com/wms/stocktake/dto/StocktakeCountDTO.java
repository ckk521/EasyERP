package com.wms.stocktake.dto;

import lombok.Data;

/**
 * 盘点作业DTO
 */
@Data
public class StocktakeCountDTO {

    /** 盘点单ID */
    private Long orderId;

    /** 盘点明细ID */
    private Long itemId;

    /** 盘点数量 */
    private Integer countedQty;

    /** 差异原因（有差异时必填） */
    private String diffReason;

    /** 差异说明 */
    private String diffRemark;

    /** 盘点轮次 */
    private Integer roundNo;
}
