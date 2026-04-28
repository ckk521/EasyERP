package com.wms.inbound.dto;

import lombok.Data;

/**
 * 不合格品处理DTO
 */
@Data
public class RejectHandleDTO {

    /** 不合格记录ID */
    private Long rejectId;

    /** 处理方式: 1退货供应商 2降价销售 3报损 4内部使用 5销毁 */
    private Integer handleType;

    /** 处理数量 */
    private Integer handleQty;

    /** 处理备注 */
    private String handleRemark;
}
