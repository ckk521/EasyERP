package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户仓库关联表
 */
@Data
@TableName("sys_user_warehouse")
public class SysUserWarehouse {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 仓库ID */
    private Long warehouseId;

    /** 权限类型: 1可操作 2仅查看 */
    private Integer permissionType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ========== 权限类型常量 ==========
    public static final int PERMISSION_OPERATE = 1;  // 可操作
    public static final int PERMISSION_VIEW = 2;     // 仅查看
}