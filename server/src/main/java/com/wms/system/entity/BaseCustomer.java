package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("base_customer")
public class BaseCustomer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private Integer type;

    private String contact;

    private String phone;

    private String email;

    private String address;

    private java.math.BigDecimal creditLimit;

    private Integer level;

    private String remark;

    private Integer status;

    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
