package com.wms.outbound.dto;

import lombok.Data;
import java.util.List;

/**
 * 发货结果DTO
 */
@Data
public class ShipResultDTO {

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

    /** 物流单号 */
    private String trackingNo;

    /** 新状态 */
    private Integer newStatus;

    /** 状态名称 */
    private String statusName;

    // ========== 批量发货结果 ==========

    /** 成功数量 */
    private Integer successCount;

    /** 失败数量 */
    private Integer failCount;

    /** 失败明细 */
    private List<ShipFailItem> failItems;

    /**
     * 发货失败项
     */
    @Data
    public static class ShipFailItem {

        /** 出库单号 */
        private String outboundOrderNo;

        /** 失败原因 */
        private String failReason;
    }
}
