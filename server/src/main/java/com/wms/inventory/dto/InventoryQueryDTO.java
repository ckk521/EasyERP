package com.wms.inventory.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 库存查询条件DTO
 */
@Data
public class InventoryQueryDTO {

    /** SKU编码（模糊查询） */
    private String skuCode;

    /** 商品名称（模糊查询） */
    private String productName;

    /** 条码（精确查询） */
    private String barcode;

    /** 仓库ID */
    private Long warehouseId;

    /** 库区ID */
    private Long zoneId;

    /** 库位ID */
    private Long locationId;

    /** 批次号 */
    private String batchNo;

    /** 效期状态：0正常 1预警 2临期 3已过期 */
    private Integer expiryStatus;

    /** ABC分类 */
    private String abcCategory;

    /** 页码 */
    private Integer page = 1;

    /** 每页数量 */
    private Integer limit = 20;
}
