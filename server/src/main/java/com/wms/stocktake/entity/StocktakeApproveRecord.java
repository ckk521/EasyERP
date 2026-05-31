package com.wms.stocktake.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 盘点审核记录表
 */
@Data
@TableName("wms_stocktake_approve_record")
public class StocktakeApproveRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 盘点单ID */
    private Long orderId;

    /** 盘点单号 */
    private String orderNo;

    /** 操作类型: approve通过/reject驳回 */
    private String action;

    /** 驳回原因 */
    private String reason;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作人角色名称 */
    private String operatorRoleName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ========== 操作类型常量 ==========
    public static final String ACTION_APPROVE = "approve";  // 通过
    public static final String ACTION_REJECT = "reject";    // 驳回
}
