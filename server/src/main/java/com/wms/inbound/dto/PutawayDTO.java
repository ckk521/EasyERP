package com.wms.inbound.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 上架作业DTO
 */
@Data
public class PutawayDTO {

    /** 入库单ID */
    private Long orderId;

    /** 入库明细ID */
    private Long itemId;

    /** 库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 上架数量 */
    private Integer putawayQty;

    /** 是否使用推荐库位 */
    private Boolean useRecommended;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 过期日期 */
    private LocalDate expiryDate;
}
