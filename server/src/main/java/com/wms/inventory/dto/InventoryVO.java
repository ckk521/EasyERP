package com.wms.inventory.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存列表VO
 */
@Data
public class InventoryVO {

    private Long id;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 库区ID */
    private Long zoneId;

    /** 库区编码 */
    private String zoneCode;

    /** 库区名称 */
    private String zoneName;

    /** 库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 条码 */
    private String barcode;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 过期日期 */
    private LocalDate expiryDate;

    /** 总数量 */
    private Integer qty;

    /** 可用数量 */
    private Integer availableQty;

    /** 锁定数量 */
    private Integer lockedQty;

    /** 效期状态：0正常 1预警 2临期 3已过期 */
    private Integer expiryStatus;

    /** 效期状态名称 */
    private String expiryStatusName;

    /** 剩余天数 */
    private Integer remainingDays;

    /** 入库时间 */
    private LocalDateTime inboundTime;

    /** 来源入库单号 */
    private String inboundOrderNo;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
