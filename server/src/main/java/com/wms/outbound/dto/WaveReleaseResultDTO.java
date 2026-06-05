package com.wms.outbound.dto;

import lombok.Data;
import java.util.List;

/**
 * 波次释放结果DTO
 */
@Data
public class WaveReleaseResultDTO {

    /** 是否成功 */
    private Boolean success;

    /** 新状态 */
    private Integer newStatus;

    /** 拣货任务数量 */
    private Integer pickTaskCount;

    /** 是否部分释放 */
    private Boolean partialRelease;

    /** 成功释放的订单数 */
    private Integer releasedOrderCount;

    /** 失败的订单数 */
    private Integer failedOrderCount;

    /** 缺货清单 */
    private List<ShortageItemDTO> shortageList;

    /** 失败原因 */
    private String failReason;
}
