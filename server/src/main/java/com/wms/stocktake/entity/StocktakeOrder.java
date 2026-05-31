package com.wms.stocktake.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 盘点单主表
 */
@Data
@TableName("wms_stocktake_order")
public class StocktakeOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 盘点单号 */
    private String orderNo;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 盘点类型: 1全盘 2抽盘 3循环盘 */
    private Integer stocktakeType;

    /** 盲盘模式: 0明盘 1盲盘 */
    private Integer blindMode;

    /** 筛选方式: zone/category/abc/sku/random */
    private String scopeType;

    /** 筛选条件JSON */
    private String scopeConfig;

    /** 状态: 0待盘点 1盘点中 2待审核 3已完成 4已取消 */
    private Integer status;

    /** 总SKU数 */
    private Integer totalItems;

    /** 已盘点SKU数 */
    private Integer countedItems;

    /** 差异SKU数 */
    private Integer diffItems;

    /** 准确率(%) */
    private BigDecimal accuracyRate;

    /** 计划盘点日期 */
    private LocalDate planDate;

    /** 实际开始时间 */
    private LocalDateTime startTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 审批人ID */
    private Long approveUserId;

    /** 审批人姓名 */
    private String approveUserName;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 备注 */
    private String remark;

    // ========== 循环盘配置字段 ==========

    /** 周期类型: daily每日/weekly每周/monthly每月 */
    private String cycleType;

    /** 盘点日: 周几(1-7) 或 每月第几天(1-31) */
    private Integer cycleDay;

    /** 轮转策略: zone_rotation库区轮转/sku_rotation按SKU轮转/fixed固定范围 */
    private String cycleStrategy;

    /** 轮转配置JSON: 如库区ID列表、SKU比例等 */
    private String cycleConfig;

    /** 当前轮转索引 */
    private Integer cycleIndex;

    /** 上次执行日期 */
    private LocalDate lastCycleDate;

    /** 下次执行日期 */
    private LocalDate nextCycleDate;

    /** 父策略ID（循环盘生成的盘点单关联到策略） */
    private Long parentStrategyId;

    /** 创建人ID */
    private Long createUserId;

    /** 创建人姓名 */
    private String createUserName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 盘点类型常量 ==========
    public static final int TYPE_FULL = 1;       // 全盘
    public static final int TYPE_SAMPLE = 2;     // 抽盘
    public static final int TYPE_CYCLE = 3;      // 循环盘

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 待盘点
    public static final int STATUS_COUNTING = 1;     // 盘点中
    public static final int STATUS_REVIEWING = 2;    // 待审核
    public static final int STATUS_COMPLETED = 3;    // 已完成
    public static final int STATUS_CANCELLED = 4;    // 已取消

    // ========== 盲盘模式常量 ==========
    public static final int BLIND_MODE_OFF = 0;  // 明盘
    public static final int BLIND_MODE_ON = 1;   // 盲盘

    // ========== 循环盘周期类型常量 ==========
    public static final String CYCLE_TYPE_DAILY = "daily";     // 每日
    public static final String CYCLE_TYPE_WEEKLY = "weekly";   // 每周
    public static final String CYCLE_TYPE_MONTHLY = "monthly"; // 每月

    // ========== 循环盘轮转策略常量 ==========
    public static final String CYCLE_STRATEGY_ZONE = "zone_rotation";      // 按库区轮转
    public static final String CYCLE_STRATEGY_SKU = "sku_rotation";        // 按SKU轮转
    public static final String CYCLE_STRATEGY_FIXED = "fixed";             // 固定范围
}
