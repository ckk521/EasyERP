package com.wms.inbound.dto;

import lombok.Data;

/**
 * 收货作业DTO
 */
@Data
public class ReceiveDTO {

    /** 入库单ID */
    private Long orderId;

    /** 入库明细ID */
    private Long itemId;

    /** 实际收货数量 */
    private Integer receivedQty;

    /** 是否部分收货 */
    private Boolean partialReceive;

    /** 差异原因 */
    private String diffReason;

    /** 是否拒收 */
    private Boolean rejectAll;
}
