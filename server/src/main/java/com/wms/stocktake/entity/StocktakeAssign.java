package com.wms.stocktake.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 盘点任务分配表
 * 对应数据库表: wms_stocktake_assign
 */
@Data
@TableName("wms_stocktake_assign")
public class StocktakeAssign {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 盘点单ID */
    private Long stocktakeId;

    /** 盘点人ID */
    private Long userId;

    /** 盘点人姓名 */
    private String userName;

    /** 分配库区ID（按库区分配时） */
    private Long zoneId;

    /** 库区编码 */
    private String zoneCode;

    /** 分配SKU数量 */
    private Integer skuCount;

    /** 已完成数量 */
    private Integer completedCount;

    /** 状态: 0未开始 1进行中 2已完成 */
    private Integer status;

    /** 分配时间 */
    private LocalDateTime assignTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 未开始
    public static final int STATUS_IN_PROGRESS = 1;  // 进行中
    public static final int STATUS_COMPLETED = 2;    // 已完成
}
