package com.wms.inbound.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.wms.system.dto.PageDTO;

/**
 * 入库单查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InboundOrderQueryDTO extends PageDTO {

    /** 入库单号 */
    private String orderNo;

    /** 采购订单号 */
    private String poNo;

    /** 送货批次号 */
    private String deliveryBatchNo;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商名称 */
    private String supplierName;

    /** 仓库ID */
    private Long warehouseId;

    /** 入库类型 */
    private Integer orderType;

    /** 状态（支持多个状态，逗号分隔，如 "0,1"） */
    private String status;

    /** 关键字搜索(入库单号/采购单号/供应商) */
    private String keyword;

    /** 开始日期 */
    private String startDate;

    /** 结束日期 */
    private String endDate;
}
