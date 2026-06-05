package com.wms.outbound.dto;

import lombok.Data;
import java.util.List;

/**
 * 拣货任务详情DTO
 */
@Data
public class PickTaskDetailDTO {

    /** 任务ID */
    private Long taskId;

    /** 任务号 */
    private String taskNo;

    /** 波次ID */
    private Long waveId;

    /** 波次号 */
    private String waveNo;

    /** 状态 */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 拣货人ID */
    private Long pickUserId;

    /** 拣货人姓名 */
    private String pickUserName;

    /** 总项数 */
    private Integer totalItems;

    /** 已完成项数 */
    private Integer completedItems;

    /** 总数量 */
    private Integer totalQty;

    /** 已拣数量 */
    private Integer pickedQty;

    /** 拣货明细列表 */
    private List<PickItemDTO> pickItems;

    /**
     * 拣货明细项
     */
    @Data
    public static class PickItemDTO {

        /** 拣货记录ID */
        private Long recordId;

        /** 出库单号 */
        private String orderNo;

        /** 商品ID */
        private Long productId;

        /** SKU编码 */
        private String skuCode;

        /** 商品名称 */
        private String productName;

        /** 商品条码 */
        private String barcode;

        /** 库位ID */
        private Long locationId;

        /** 库位编码 */
        private String locationCode;

        /** 批次号 */
        private String batchNo;

        /** 计划数量 */
        private Integer planQty;

        /** 实际数量 */
        private Integer actualQty;

        /** 差异数量 */
        private Integer diffQty;

        /** 状态 */
        private Integer status;

        /** 状态名称 */
        private String statusName;

        /** 库位是否已扫码 */
        private Boolean locationScanned;

        /** 商品是否已扫码 */
        private Boolean productScanned;

        /** 拣货顺序 */
        private Integer sortOrder;

        /** 是否异常 */
        private Boolean isException;

        /** 异常类型 */
        private Integer exceptionType;

        /** 异常数量 */
        private Integer exceptionQty;

        /** 异常备注 */
        private String exceptionRemark;
    }
}