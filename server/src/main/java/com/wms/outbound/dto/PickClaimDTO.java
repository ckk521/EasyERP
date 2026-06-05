package com.wms.outbound.dto;

import lombok.Data;

/**
 * 拣货任务领取DTO
 */
@Data
public class PickClaimDTO {

    /** 波次ID（领取指定波次的任务） */
    private Long waveId;

    /** 拣货人ID */
    private Long pickUserId;

    /** 拣货人姓名 */
    private String pickUserName;
}