package com.wms.returnorder.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 退货单DTO（用于返回前端）
 */
@Data
public class ReturnOrderDTO {

    private Long id;
    private String returnNo;

    /** 原出库单信息 */
    private Long originalOutboundId;
    private String originalOutboundNo;

    /** 客户信息 */
    private Long customerId;
    private String customerName;

    /** 退货原因 */
    private Integer returnReason;
    private String returnReasonText;

    /** 状态 */
    private Integer status;

    /** 数量 */
    private Integer totalExpectedQty;
    private Integer totalReceivedQty;

    /** 关联入库单 */
    private Long inboundOrderId;
    private String inboundOrderNo;

    /** 仓库 */
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;

    /** 取消原因 */
    private String cancelReason;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private Long createUserId;
    private String createUserName;
    private LocalDateTime createTime;

    /** 收货人 */
    private Long receiveUserId;
    private String receiveUserName;
    private LocalDateTime receiveTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 退货商品明细 */
    private List<ReturnOrderItemDTO> items;

    /**
     * 退货商品明细DTO
     */
    @Data
    public static class ReturnOrderItemDTO {
        private Long id;
        private Long productId;
        private String skuCode;
        private String productName;
        private String barcode;
        private Integer originalQty;
        private Integer expectedQty;
        private Integer receivedQty;
        private String remark;
    }

    /**
     * 获取退货原因名称
     */
    public String getReturnReasonName() {
        if (returnReason == null) return null;
        switch (returnReason) {
            case 1: return "质量问题";
            case 2: return "发错货";
            case 3: return "数量不符";
            case 4: return "不满意";
            case 5: return "7天无理由";
            case 6: return "其他";
            default: return "";
        }
    }

    /**
     * 获取状态名称
     */
    public String getStatusName() {
        if (status == null) return null;
        switch (status) {
            case 0: return "待收货";
            case 1: return "已收货";
            case 2: return "已完成";
            case 9: return "已取消";
            default: return "";
        }
    }
}