package com.wms.stocktake.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建盘点单DTO
 */
@Data
public class StocktakeCreateDTO {

    /** 仓库ID */
    private Long warehouseId;

    /** 盘点类型: 1全盘 2抽盘 3循环盘 */
    private Integer stocktakeType;

    /** 盲盘模式: 0明盘 1盲盘 */
    private Integer blindMode;

    /** 筛选方式: all/zone/category/abc/sku/random */
    private String scopeType;

    /** 库区ID（全盘-指定库区时使用） */
    private Long zoneId;

    /** 库区ID列表（抽盘-按库区时使用） */
    private List<Long> zoneIds;

    /** 商品分类ID列表（抽盘-按分类时使用） */
    private List<Long> categoryIds;

    /** ABC分类（抽盘-按ABC时使用） */
    private String abcClass;

    /** SKU编码列表（抽盘-指定SKU时使用） */
    private List<String> skuCodes;

    /** 随机抽取比例（抽盘-随机抽取时使用） */
    private Integer randomPercent;

    /** 计划日期 */
    private LocalDate planDate;

    /** 备注 */
    private String remark;

    // ========== 循环盘配置 ==========

    /** 周期类型: daily/weekly/monthly */
    private String cycleType;

    /** 盘点日: 周几(1-7) 或 每月第几天(1-31) */
    private Integer cycleDay;

    /** 轮转策略: zone_rotation/sku_rotation/fixed */
    private String cycleStrategy;

    /** 轮转库区ID列表（按库区轮转时使用） */
    private List<Long> cycleZoneIds;

    /** SKU轮转比例（按SKU轮转时使用，每次盘点百分比） */
    private Integer cycleSkuPercent;
}
