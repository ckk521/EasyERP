package com.wms.inbound.dto;

import lombok.Data;
import java.util.List;

/**
 * 入库单链路DTO
 * 用于展示完整的单据流转和数量关系
 */
@Data
public class InboundChainDTO {

    /** 入库单ID */
    private Long inboundOrderId;

    /** 入库单号 */
    private String inboundOrderNo;

    /** 入库类型 */
    private Integer orderType;

    /** 入库类型名称 */
    private String orderTypeName;

    /** 采购订单号 */
    private String poNo;

    /** 供应商名称 */
    private String supplierName;

    /** 数量节点 */
    private QuantityNode quantityNode;

    /** 关联的异常处理单链路 */
    private List<ExceptionChainDTO> exceptionChains;

    /**
     * 数量节点
     */
    @Data
    public static class QuantityNode {
        /** 预期数量 */
        private Integer expectedQty;

        /** 收货数量 */
        private Integer receivedQty;

        /** 合格数量 */
        private Integer qualifiedQty;

        /** 不合格数量 */
        private Integer rejectedQty;

        /** 上架数量 */
        private Integer putawayQty;

        /** 隔离数量 */
        private Integer isolatedQty;
    }

    /**
     * 异常处理单链路
     */
    @Data
    public static class ExceptionChainDTO {
        /** 异常处理单ID */
        private Long exceptionOrderId;

        /** 异常处理单号 */
        private String exceptionOrderNo;

        /** 异常类型 */
        private Integer exceptionType;

        /** 异常类型名称 */
        private String exceptionTypeName;

        /** 来源类型 */
        private Integer sourceType;

        /** 来源类型名称 */
        private String sourceTypeName;

        /** 异常数量 */
        private Integer exceptionQty;

        /** 处理方式 */
        private Integer handleType;

        /** 处理方式名称 */
        private String handleTypeName;

        /** 状态 */
        private Integer status;

        /** 状态名称 */
        private String statusName;

        /** 补货入库单 */
        private ReplacementInboundDTO replacementInbound;
    }

    /**
     * 补货入库单信息
     */
    @Data
    public static class ReplacementInboundDTO {
        /** 入库单ID */
        private Long inboundOrderId;

        /** 入库单号 */
        private String inboundOrderNo;

        /** 数量节点 */
        private QuantityNode quantityNode;

        /** 该补货单产生的异常链路 */
        private List<ExceptionChainDTO> subExceptionChains;
    }
}
