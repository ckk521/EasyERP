package com.wms.outbound.dto;

import lombok.Data;
import java.util.List;

/**
 * 发货确认DTO
 */
@Data
public class ShipConfirmDTO {

    /** 出库单ID */
    private Long outboundOrderId;

    /** 出库单号 */
    private String outboundOrderNo;

    /** 包裹号 */
    private String packageNo;

    /** 物流公司 */
    private String logisticsCompany;

    /** 物流公司编码 */
    private String logisticsCompanyCode;

    /** 物流单号 */
    private String trackingNo;

    /** 发货人ID */
    private Long shipUserId;

    /** 发货人姓名 */
    private String shipUserName;

    /** 备注 */
    private String remark;

    // ========== 批量发货字段 ==========

    /** 批量发货的出库单ID列表 */
    private List<Long> orderIds;

    /** 是否批量发货 */
    private Boolean isBatch;
}
