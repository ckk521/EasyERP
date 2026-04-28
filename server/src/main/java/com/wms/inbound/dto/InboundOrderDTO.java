package com.wms.inbound.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/**
 * 入库单创建/更新DTO
 */
@Data
public class InboundOrderDTO {

    /** 入库类型: 1采购 2退货 3调拨 4赠品 5其他 */
    @NotNull(message = "入库类型不能为空")
    private Integer orderType;

    /** 采购订单号(采购入库时必填) */
    private String poNo;

    /** 送货批次号 */
    @Size(max = 50, message = "送货批次号长度不能超过50")
    private String deliveryBatchNo;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商编码 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 仓库ID */
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 预计到货日期 */
    private LocalDate expectedDate;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    /** 商品明细列表 */
    @NotEmpty(message = "商品明细不能为空")
    private List<InboundOrderItemDTO> items;

    /** 是否保存为草稿 */
    private Boolean isDraft = false;

    /** 取消原因(取消时使用) */
    private String cancelReason;
}
