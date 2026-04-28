package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("base_location")
public class BaseLocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private Long zoneId;

    private String zoneCode;

    private Long warehouseId;

    private String warehouseCode;

    private Integer rowNum;

    private Integer colNum;

    private Integer layerNum;

    private Integer type;

    /** 货架类型：1-立体货架 2-贯通式货架 3-自动化仓库 */
    private Integer shelfType;

    /** 通道号(贯通式货架用) */
    private Integer aisleNum;

    /** 深度号(贯通式货架用，从外到里递增) */
    private Integer depthNum;

    /**
     * 存储类型：1-常温 2-冷藏 3-冷冻 4-恒温
     * 必须在仓库支持的 storageTypes 范围内
     */
    private Integer storageType;

    private BigDecimal maxLength;

    private BigDecimal maxWidth;

    private BigDecimal maxHeight;

    private BigDecimal maxWeight;

    private String allowedSku;

    private Integer status;

    /** 是否被挡住(贯通式货架用)：0-否 1-是 */
    private Integer isBlocked;

    private Integer currentQty;

    private LocalDateTime lastCheckTime;

    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
