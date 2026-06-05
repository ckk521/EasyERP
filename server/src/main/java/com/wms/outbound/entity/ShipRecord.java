package com.wms.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 发货记录表
 */
@Data
@TableName("wms_ship_record")
public class ShipRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 出库单ID */
    private Long outboundOrderId;

    /** 出库单号 */
    private String outboundOrderNo;

    /** 包裹号 */
    private String packageNo;

    /** 物流公司 */
    private String logisticsCompany;

    /** 物流公司编码 */
    private String logisticsCompanyCode;

    /** 物流单号 */
    private String trackingNo;

    /** 发货人ID */
    private Long shipUserId;

    /** 发货人姓名 */
    private String shipUserName;

    /** 状态: 0待发货 1已发货 9已取消 */
    private Integer status;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 备注 */
    private String remark;

    /** 是否已通知ERP: 0未通知 1已通知 */
    private Integer erpNotified;

    /** ERP通知时间 */
    private LocalDateTime erpNotifyTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 待发货
    public static final int STATUS_SHIPPED = 1;      // 已发货
    public static final int STATUS_CANCELLED = 9;    // 已取消
}
