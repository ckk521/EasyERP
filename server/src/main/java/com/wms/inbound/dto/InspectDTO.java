package com.wms.inbound.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 验收作业DTO
 */
@Data
public class InspectDTO {

    /** 入库单ID */
    private Long orderId;

    /** 入库明细ID */
    private Long itemId;

    /** 验收结果: true合格 false不合格 */
    private Boolean qualified;

    /** 合格数量 */
    private Integer qualifiedQty;

    /** 不合格数量 */
    private Integer rejectedQty;

    /** 不合格类型: 1包装破损 2商品损坏 3错货 4规格不符 5效期问题 6其他 */
    private Integer rejectType;

    /** 不合格原因 */
    private String rejectReason;

    /** 不合格照片URL(逗号分隔) */
    private String rejectImages;

    /** 生产日期(效期商品) */
    private LocalDate productionDate;

    /** 过期日期(效期商品) */
    private LocalDate expiryDate;

    /** 是否批量验收 */
    private Boolean batchInspect;

    /** 批量验收的明细ID列表 */
    private java.util.List<Long> itemIds;
}
