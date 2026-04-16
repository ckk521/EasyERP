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
