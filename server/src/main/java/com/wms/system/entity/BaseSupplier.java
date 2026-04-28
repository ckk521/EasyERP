package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("base_supplier")
public class BaseSupplier {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String contact;

    private String phone;

    private String email;

    private String address;

    private String bankName;

    private String bankAccount;

    private String taxNo;

    private String remark;

    private Integer status;

    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
