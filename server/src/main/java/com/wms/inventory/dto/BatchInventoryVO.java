package com.wms.inventory.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 批次库存VO
 */
@Data
public class BatchInventoryVO {

    private Long id;

    /** 批次号 */
    private String batchNo;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 过期日期 */
    private LocalDate expiryDate;

    /** 总数量（汇总） */
    private Integer totalQty;

    /** 可用数量（汇总） */
    private Integer totalAvailableQty;

    /** 锁定数量（汇总） */
    private Integer totalLockedQty;

    /** 库位数量（分布在多少个库位） */
    private Integer locationCount;

    /** 效期状态 */
    private Integer expiryStatus;

    /** 效期状态名称 */
    private String expiryStatusName;

    /** 剩余天数 */
    private Integer remainingDays;

    /** 最早入库时间 */
    private LocalDateTime earliestInboundTime;

    /** 最新入库单号 */
    private String inboundOrderNo;
}