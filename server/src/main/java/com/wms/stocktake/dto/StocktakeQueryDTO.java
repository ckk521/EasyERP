package com.wms.stocktake.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 盘点单查询DTO
 */
@Data
public class StocktakeQueryDTO {

    /** 盘点单号 */
    private String orderNo;

    /** 仓库ID */
    private Long warehouseId;

    /** 盘点类型 */
    private Integer stocktakeType;

    /** 状态 */
    private Integer status;

    /** 计划日期-开始 */
    private LocalDate planDateStart;

    /** 计划日期-结束 */
    private LocalDate planDateEnd;

    /** 关键字搜索 */
    private String keyword;

    /** 页码 */
    private Integer page = 1;

    /** 每页数量 */
    private Integer limit = 20;
}
