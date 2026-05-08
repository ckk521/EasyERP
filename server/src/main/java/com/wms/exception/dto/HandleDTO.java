package com.wms.exception.dto;

import lombok.Data;

/**
 * 异常处理DTO
 */
@Data
public class HandleDTO {

    /** 处理方式: 1退货 2换货 3报废 4降价销售 */
    private Integer handleType;

    /** 处理结果说明 */
    private String handleResult;

    /** 物流公司（退货时使用） */
    private String logisticsCompany;

    /** 物流单号（退货时使用） */
    private String logisticsNo;

    /** 降价比例（降价销售时使用） */
    private Integer discountPercent;

    /** 降价金额（降价销售时使用） */
    private java.math.BigDecimal discountAmount;
}
