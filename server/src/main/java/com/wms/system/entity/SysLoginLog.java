package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDateTime time;

    private Long userId;

    private String username;

    private String action;

    private String ip;

    private String device;

    private String browser;

    private String result;

    private String failReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
