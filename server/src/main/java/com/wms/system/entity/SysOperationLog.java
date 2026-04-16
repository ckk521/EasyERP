package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDateTime time;

    private Long userId;

    private String username;

    private String module;

    private String action;

    private String object;

    private String ip;

    private String requestMethod;

    private String requestUrl;

    private String requestParams;

    private String realName;

    private String result;

    private String errorMessage;

    private Long responseTime;

    private String details;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
