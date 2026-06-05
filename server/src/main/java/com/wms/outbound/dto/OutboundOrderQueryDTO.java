package com.wms.outbound.dto;

import lombok.Data;

/**
 * 出库单查询DTO
 */
@Data
public class OutboundOrderQueryDTO {

    /** 页码 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer limit = 20;

    /** 出库单号 */
    private String orderNo;

    /** 销售订单号 */
    private String soNo;

    /** 客户ID */
    private Long customerId;

    /** 仓库ID */
    private Long warehouseId;

    /** 出库类型 */
    private Integer orderType;

    /** 状态（支持多个状态，逗号分隔） */
    private String status;

    /** 优先级 */
    private Integer priority;

    /** 波次号 */
    private String waveNo;

    /** 物流公司 */
    private String logisticsCompany;

    /** 关键字搜索（出库单号/销售单号/客户名称） */
    private String keyword;

    /** 开始日期 */
    private String startDate;

    /** 结束日期 */
    private String endDate;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;
}