package com.wms.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 打包记录表
 *
 * 状态流转：待打包(0) → 打包中(1) → 已打包(2) → 已发货(3) → 已取消(9)
 */
@Data
@TableName("wms_pack_record")
public class PackRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 出库单ID */
    private Long outboundOrderId;

    /** 出库单号 */
    private String outboundOrderNo;

    /** 包裹号 PK20260531001 */
    private String packageNo;

    /** 包装箱型（小箱/中箱/大箱/超大箱） */
    private String boxType;

    /** 箱型编码 */
    private String boxTypeCode;

    /** 包裹重量(kg) */
    private BigDecimal weight;

    /** 包裹体积(m³) */
    private BigDecimal volume;

    /** 包裹内商品总数量 */
    private Integer totalQty;

    /** 包裹内SKU种类数 */
    private Integer totalSku;

    /** 物流公司 */
    private String logisticsCompany;

    /** 物流单号 */
    private String trackingNo;

    /** 打包人ID */
    private Long packUserId;

    /** 打包人姓名 */
    private String packUserName;

    /** 状态: 0待打包 1打包中 2已打包 3已发货 9已取消 */
    private Integer status;

    /** 领取时间 */
    private LocalDateTime claimTime;

    /** 打包完成时间 */
    private LocalDateTime packTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 状态常量 ==========
    public static final int STATUS_PENDING = 0;      // 待打包
    public static final int STATUS_PACKING = 1;      // 打包中
    public static final int STATUS_PACKED = 2;       // 已打包
    public static final int STATUS_SHIPPED = 3;      // 已发货
    public static final int STATUS_CANCELLED = 9;    // 已取消
}
