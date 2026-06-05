package com.wms.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 拣货任务表（按波次汇总）
 *
 * 状态流转：待领取(0) → 进行中(1) → 已完成(2) / 已取消(9)
 */
@Data
@TableName("wms_pick_task")
public class PickTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 波次ID */
    private Long waveId;

    /** 波次号 */
    private String waveNo;

    /** 任务号 PT20260531001 */
    private String taskNo;

    /** 拣货人ID */
    private Long pickUserId;

    /** 拣货人姓名 */
    private String pickUserName;

    /** 状态: 0待领取 1进行中 2已完成 9已取消 */
    private Integer status;

    /** 总拣货项数 */
    private Integer totalItems;

    /** 已完成项数 */
    private Integer completedItems;

    /** 总拣货数量 */
    private Integer totalQty;

    /** 已拣数量 */
    private Integer pickedQty;

    /** 领取时间 */
    private LocalDateTime claimTime;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 待领取
    public static final int STATUS_IN_PROGRESS = 1;  // 进行中
    public static final int STATUS_COMPLETED = 2;    // 已完成
    public static final int STATUS_CANCELLED = 9;    // 已取消
}