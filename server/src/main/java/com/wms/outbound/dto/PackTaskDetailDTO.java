package com.wms.outbound.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 打包任务详情DTO
 */
@Data
public class PackTaskDetailDTO {

    /** 出库单ID */
    private Long outboundOrderId;

    /** 出库单号 */
    private String outboundOrderNo;

    /** 客户名称 */
    private String customerName;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

    /** 物流公司 */
    private String logisticsCompany;

    /** 总商品数量 */
    private Integer totalQty;

    /** 总SKU种类数 */
    private Integer totalSku;

    /** 状态 */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 商品明细列表 */
    private List<PackItemDTO> items;

    /** 推荐包装箱型 */
    private List<RecommendedBoxDTO> recommendedBoxes;

    /**
     * 商品明细
     */
    @Data
    public static class PackItemDTO {

        /** 商品ID */
        private Long productId;

        /** SKU编码 */
        private String skuCode;

        /** 商品名称 */
        private String productName;

        /** 商品条码 */
        private String barcode;

        /** 数量 */
        private Integer qty;

        /** 已打包数量 */
        private Integer packedQty;
    }

    /**
     * 推荐包装箱型
     */
    @Data
    public static class RecommendedBoxDTO {

        /** 箱型编码 */
        private String code;

        /** 箱型名称 */
        private String name;

        /** 长度(cm) */
        private BigDecimal length;

        /** 宽度(cm) */
        private BigDecimal width;

        /** 高度(cm) */
        private BigDecimal height;

        /** 体积(m³) */
        private BigDecimal volume;

        /** 最大承重(kg) */
        private BigDecimal maxWeight;

        /** 推荐指数（1-5星） */
        private Integer recommendLevel;

        /** 推荐理由 */
        private String reason;
    }
}
