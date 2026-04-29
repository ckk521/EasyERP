package com.wms.inventory.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 效期预警VO
 */
@Data
public class ExpiryWarningVO {

    private Long id;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 批次号 */
    private String batchNo;

    /** 库位编码 */
    private String locationCode;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 过期日期 */
    private LocalDate expiryDate;

    /** 剩余天数 */
    private Integer remainingDays;

    /** 效期状态：1预警 2临期 3已过期 */
    private Integer expiryStatus;

    /** 效期状态名称 */
    private String expiryStatusName;

    /** 库存数量 */
    private Integer qty;

    /** 预警级别：紧急/重要/一般 */
    private String warningLevel;

    /** 入库单号 */
    private String inboundOrderNo;

    /** 入库时间 */
    private LocalDateTime inboundTime;
}
