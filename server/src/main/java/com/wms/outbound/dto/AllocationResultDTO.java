package com.wms.outbound.dto;

import lombok.Data;

/**
 * 库存分配结果DTO
 */
@Data
public class AllocationResultDTO {

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 申请数量 */
    private Integer requestedQty;

    /** 实际分配数量 */
    private Integer allocatedQty;

    /** 是否成功 */
    private Boolean success;

    /** 失败原因 */
    private String failReason;

    /** 库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 批次号 */
    private String batchNo;

    /** 可用库存（分配前） */
    private Integer availableBefore;

    /** 可用库存（分配后） */
    private Integer availableAfter;

    /** 锁定库存（分配前） */
    private Integer lockedBefore;

    /** 锁定库存（分配后） */
    private Integer lockedAfter;
}