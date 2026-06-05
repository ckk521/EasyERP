package com.wms.outbound.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 波次创建DTO
 */
@Data
public class WaveCreateDTO {

    /** 策略类型: 1按时间 2按物流 3按区域 4按商品 5按客户 */
    private Integer strategyType;

    /** 策略名称 */
    private String strategyName;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 开始时间（按时间策略时使用） */
    private LocalDateTime startTime;

    /** 结束时间（按时间策略时使用） */
    private LocalDateTime endTime;

    /** 物流公司（按物流策略时使用） */
    private String logisticsCompany;

    /** 区域（按区域策略时使用） */
    private String region;

    /** SKU重复阈值（按商品策略时使用） */
    private Integer skuRepeatThreshold;

    /** 客户ID（按客户策略时使用） */
    private Long customerId;

    /** 最大订单数限制 */
    private Integer maxOrders;

    /** 备注 */
    private String remark;
}
