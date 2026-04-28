package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 货架配置表
 * 用于配置货架参数，支持批量生成库位
 */
@Data
@TableName("base_shelf_config")
public class BaseShelfConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private Long zoneId;

    private String zoneCode;

    private Long warehouseId;

    private String warehouseCode;

    /** 排号 */
    private Integer rowNum;

    /** 货架类型：1-立体货架 2-贯通式货架 3-自动化仓库 */
    private Integer shelfType;

    /** 起始层 */
    private Integer startLayer;

    /** 结束层 */
    private Integer endLayer;

    /** 每层位置数 */
    private Integer columnCount;

    /** 通道数(贯通式货架用) */
    private Integer aisleCount;

    /** 深度数(贯通式货架用，每个通道有几个深度位置) */
    private Integer depthCount;

    /** 存储类型：1-常温 2-冷藏 3-冷冻 4-恒温 */
    private Integer storageType;

    private BigDecimal maxLength;

    private BigDecimal maxWidth;

    private BigDecimal maxHeight;

    private BigDecimal maxWeight;

    private Integer status;

    /** 是否已生成库位：0-否 1-是 */
    private Integer isGenerated;

    /** 生成的库位数量 */
    private Integer locationCount;

    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 常量定义 ==========

    /** 货架类型：立体货架 */
    public static final int SHELF_TYPE_STANDARD = 1;
    /** 货架类型：贯通式货架 */
    public static final int SHELF_TYPE_DRIVE_IN = 2;
    /** 货架类型：自动化仓库 */
    public static final int SHELF_TYPE_ASRS = 3;

    /** 未生成库位 */
    public static final int GENERATED_NO = 0;
    /** 已生成库位 */
    public static final int GENERATED_YES = 1;

    /**
     * 计算总库位数
     */
    public int calculateLocationCount() {
        if (startLayer == null || endLayer == null || columnCount == null) {
            return 0;
        }
        int layers = endLayer - startLayer + 1;
        return layers * columnCount;
    }
}
