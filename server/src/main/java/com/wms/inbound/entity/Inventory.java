package com.wms.inbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存表
 */
@Data
@TableName("wms_inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库编码 */
    private String warehouseCode;

    /** 库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 过期日期 */
    private LocalDate expiryDate;

    /** 总数量 */
    private Integer qty;

    /** 可用数量 */
    private Integer availableQty;

    /** 锁定数量 */
    private Integer lockedQty;

    /** 来源入库单ID */
    private Long inboundOrderId;

    /** 来源入库单号 */
    private String inboundOrderNo;

    /** 入库时间 */
    private LocalDateTime inboundTime;

    /** 效期状态: 0正常 1预警 2临期 3已过期 */
    private Integer expiryStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 效期状态常量 ==========
    public static final int EXPIRY_STATUS_NORMAL = 0;     // 正常
    public static final int EXPIRY_STATUS_WARNING = 1;    // 预警
    public static final int EXPIRY_STATUS_NEAR = 2;       // 临期
    public static final int EXPIRY_STATUS_EXPIRED = 3;    // 已过期
}
