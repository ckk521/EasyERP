package com.wms.outbound.dto;

import lombok.Data;

/**
 * 拣货确认DTO
 */
@Data
public class PickConfirmDTO {

    /** 拣货记录ID */
    private Long recordId;

    /** 库位ID */
    private Long locationId;

    /** 库位编码（扫码） */
    private String locationCode;

    /** 商品条码（扫码） */
    private String barcode;

    /** 实际拣货数量 */
    private Integer actualQty;

    /** 是否异常 */
    private Boolean isException;

    /** 异常类型: 1缺货 2破损 3错货 4其他 */
    private Integer exceptionType;

    /** 异常数量 */
    private Integer exceptionQty;

    /** 异常备注 */
    private String exceptionRemark;

    /** 差异原因 */
    private String diffReason;
}