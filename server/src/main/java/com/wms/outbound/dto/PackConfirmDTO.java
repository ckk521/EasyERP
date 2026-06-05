package com.wms.outbound.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 打包确认DTO
 */
@Data
public class PackConfirmDTO {

    /** 出库单ID */
    private Long outboundOrderId;

    /** 出库单号 */
    private String outboundOrderNo;

    /** 出库明细ID（单商品打包时使用） */
    private Long itemId;

    /** 打包数量（单商品打包时使用） */
    private Integer packQty;

    /** 包装箱型编码 */
    private String boxTypeCode;

    /** 包裹重量(kg) */
    private BigDecimal weight;

    /** 打包人ID */
    private Long packUserId;

    /** 打包人姓名 */
    private String packUserName;

    /** 备注 */
    private String remark;
}
