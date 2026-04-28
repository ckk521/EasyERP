package com.wms.inbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 上架记录表
 */
@Data
@TableName("wms_putaway_record")
public class PutawayRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 来源入库单ID */
    private Long inboundOrderId;

    /** 来源入库单号 */
    private String inboundOrderNo;

    /** 来源入库明细ID */
    private Long inboundItemId;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String productName;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private java.time.LocalDate productionDate;

    /** 过期日期 */
    private java.time.LocalDate expiryDate;

    /** 仓库ID */
    private Long warehouseId;

    /** 库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 上架数量 */
    private Integer putawayQty;

    /** 是否使用推荐库位: 0否 1是 */
    private Integer isRecommended;

    /** 推荐的库位ID */
    private Long recommendedLocationId;

    /** 上架时间 */
    private LocalDateTime putawayTime;

    /** 上架人 */
    private Long putawayUser;

    /** 上架人姓名 */
    private String putawayUserName;
}
