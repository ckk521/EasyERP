package com.wms.outbound.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/**
 * 出库单创建/更新DTO
 */
@Data
public class OutboundOrderDTO {

    /** 出库类型: 1销售 2调拨 3退货 4报废 5样品 */
    @NotNull(message = "出库类型不能为空")
    private Integer orderType;

    /** 来源类型: 1ERP推送 2手工创建 3调拨申请 */
    @NotNull(message = "来源类型不能为空")
    private Integer sourceType;

    /** 销售订单号(销售出库时必填) */
    private String soNo;

    /** 客户ID */
    private Long customerId;

    /** 客户编码 */
    private String customerCode;

    /** 客户名称 */
    private String customerName;

    /** 客户电话 */
    private String customerPhone;

    /** 客户地址 */
    private String customerAddress;

    /** 仓库ID */
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 优先级: 1紧急 2高 3中 4低 */
    private Integer priority;

    /** 物流公司 */
    @Size(max = 50, message = "物流公司名称长度不能超过50")
    private String logisticsCompany;

    /** 收货人姓名 */
    @Size(max = 50, message = "收货人姓名长度不能超过50")
    private String receiverName;

    /** 收货人电话 */
    @Size(max = 20, message = "收货人电话长度不能超过20")
    private String receiverPhone;

    /** 收货地址 */
    @Size(max = 200, message = "收货地址长度不能超过200")
    private String receiverAddress;

    /** 供应商ID(退货出库) */
    private Long supplierId;

    /** 供应商编码 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 目标仓库ID(调拨出库) */
    private Long targetWarehouseId;

    /** 目标仓库编码 */
    private String targetWarehouseCode;

    /** 目标仓库名称 */
    private String targetWarehouseName;

    /** 预计发货日期 */
    private LocalDate expectedShipDate;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    /** 商品明细列表 */
    @NotEmpty(message = "商品明细不能为空")
    private List<OutboundOrderItemDTO> items;

    /** 取消原因(取消时使用) */
    private String cancelReason;
}