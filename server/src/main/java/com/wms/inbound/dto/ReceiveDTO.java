package com.wms.inbound.dto;

import lombok.Data;
import java.time.LocalDate;

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

    /** 批次号（供应商提供的批次号） */
    private String batchNo;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 有效期/过期日期 */
    private LocalDate expiryDate;

    /** 是否部分收货 */
    private Boolean partialReceive;

    /** 差异原因 */
    private String diffReason;

    /** 是否拒收 */
    private Boolean rejectAll;
}
