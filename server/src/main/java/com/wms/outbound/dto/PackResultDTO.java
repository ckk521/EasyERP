package com.wms.outbound.dto;

import lombok.Data;

/**
 * 打包结果DTO
 */
@Data
public class PackResultDTO {

    /** 是否成功 */
    private Boolean success;

    /** 失败原因 */
    private String failReason;

    /** 出库单ID */
    private Long outboundOrderId;

    /** 出库单号 */
    private String outboundOrderNo;

    /** 包裹号 */
    private String packageNo;

    /** 新状态 */
    private Integer newStatus;

    /** 状态名称 */
    private String statusName;
}
