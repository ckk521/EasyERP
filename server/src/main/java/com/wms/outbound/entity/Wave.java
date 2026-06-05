package com.wms.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 波次表
 *
 * 状态流转：待释放(0) → 拣货中(1) → 已完成(2) → 已取消(9)
 */
@Data
@TableName("wms_wave")
public class Wave {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 波次号 W20260531001 */
    private String waveNo;

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

    /** 状态: 0待释放 1拣货中 2已完成 9已取消 */
    private Integer status;

    /** 总订单数 */
    private Integer totalOrders;

    /** 总SKU数 */
    private Integer totalSku;

    /** 总件数 */
    private Integer totalQty;

    /** 已拣件数 */
    private Integer pickedQty;

    /** 已打包件数 */
    private Integer packedQty;

    /** 已发货件数 */
    private Integer shippedQty;

    /** 分配人员ID */
    private Long assignedUserId;

    /** 分配人员姓名 */
    private String assignedUserName;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 待释放
    public static final int STATUS_PICKING = 1;      // 拣货中
    public static final int STATUS_COMPLETED = 2;    // 已完成
    public static final int STATUS_CANCELLED = 9;    // 已取消

    // ========== 策略类型常量 ==========
    public static final int STRATEGY_TIME = 1;       // 按时间
    public static final int STRATEGY_LOGISTICS = 2;  // 按物流
    public static final int STRATEGY_REGION = 3;     // 按区域
    public static final int STRATEGY_PRODUCT = 4;    // 按商品
    public static final int STRATEGY_CUSTOMER = 5;   // 按客户
}
