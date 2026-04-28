package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("base_zone")
public class BaseZone {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private Long warehouseId;

    private String warehouseCode;

    private Integer type;

    private Integer tempRequire;

    /** 默认货架类型：1-立体货架 2-贯通式货架 3-自动化仓库 */
    private Integer defaultShelfType;

    /**
     * 存储类型（继承仓库，可细化）：1-常温 2-冷藏 3-冷冻 4-恒温
     * 逗号分隔，如 "1,2" 表示支持常温和冷藏
     */
    private String storageTypes;

    private Integer locationCount;

    private Integer status;

    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
