package com.wms.outbound.dto;

import lombok.Data;

/**
 * 波次释放DTO
 */
@Data
public class WaveReleaseDTO {

    /** 波次ID */
    private Long waveId;

    /** 是否自动分配 */
    private Boolean autoAssign;

    /** 指定拣货员ID（手动分配时使用） */
    private Long assignedUserId;

    /** 指定拣货员姓名（手动分配时使用） */
    private String assignedUserName;

    /** 是否允许部分释放（库存不足时） */
    private Boolean allowPartialRelease;
}
