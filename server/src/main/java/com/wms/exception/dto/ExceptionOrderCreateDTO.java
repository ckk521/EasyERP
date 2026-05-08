package com.wms.exception.dto;

import lombok.Data;
import java.util.List;

/**
 * 创建异常处理单DTO
 */
@Data
public class ExceptionOrderCreateDTO {

    /** 入库单ID */
    private Long inboundOrderId;

    /** 入库单号 */
    private String inboundOrderNo;

    /** 采购订单ID */
    private Long purchaseOrderId;

    /** 采购订单号 */
    private String purchaseOrderNo;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商编码 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库编码 */
    private String warehouseCode;

    /** 隔离库区ID */
    private Long zoneId;

    /** 隔离库区编码 */
    private String zoneCode;

    /** 异常类型 */
    private Integer exceptionType;

    /** 异常原因 */
    private String exceptionReason;

    /** 来源类型: 1收货异常 2验收异常 */
    private Integer sourceType;

    /** 备注 */
    private String remark;

    /** 异常商品明细列表 */
    private List<ExceptionItemDTO> items;

    @Data
    public static class ExceptionItemDTO {
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

        /** 异常数量 */
        private Integer exceptionQty;

        /** 异常类型 */
        private Integer exceptionType;

        /** 异常原因 */
        private String exceptionReason;

        /** 关联入库明细ID */
        private Long inboundItemId;
    }
}
