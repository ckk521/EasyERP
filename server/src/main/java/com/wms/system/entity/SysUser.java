package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 系统用户/员工表
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 密码 */
    private String password;

    /** 员工工号 */
    private String employeeNo;

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 部门（字典） */
    private Integer department;

    /** 岗位（字典） */
    private Integer position;

    /** 入职日期 */
    private LocalDate hireDate;

    /** 工作状态: 1在职 2休假 3离职 */
    private Integer workStatus;

    /** 技能等级: 1初级 2中级 3高级 4专家 */
    private Integer skillLevel;

    /** 班次: 1早班 2中班 3晚班 4常白班 */
    private Integer shiftType;

    /** 账号状态: 0禁用 1启用 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 登录次数 */
    private Integer loginCount;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 工作状态常量 ==========
    public static final int WORK_STATUS_ACTIVE = 1;    // 在职
    public static final int WORK_STATUS_LEAVE = 2;     // 休假
    public static final int WORK_STATUS_RESIGNED = 3;  // 离职

    // ========== 账号状态常量 ==========
    public static final int STATUS_DISABLED = 0;  // 禁用
    public static final int STATUS_ENABLED = 1;  // 启用
}
