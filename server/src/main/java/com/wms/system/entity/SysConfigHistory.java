package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_config_history")
public class SysConfigHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long configId;

    private String oldValue;

    private String newValue;

    private Long operatorId;

    private String operatorName;

    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
