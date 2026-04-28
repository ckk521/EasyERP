package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_warehouse")
public class SysWarehouse {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private Integer type;

    /**
     * 存储类型（支持多选，逗号分隔）：1-常温 2-冷藏 3-冷冻 4-恒温 5-混合
     * 混合仓库可配置多种存储类型，如 "1,2,3" 表示支持常温、冷藏、冷冻
     */
    private String storageTypes;

    private String country;

    private String province;

    private String address;

    private String manager;

    private String phone;

    private BigDecimal area;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
