package com.wms.system.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建用户DTO
 */
@Data
public class UserCreateDTO {

    /** 员工工号 */
    private String employeeNo;

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 部门 */
    private Integer department;

    /** 岗位 */
    private Integer position;

    /** 入职日期 */
    private LocalDate hireDate;

    /** 工作状态 */
    private Integer workStatus;

    /** 技能等级 */
    private Integer skillLevel;

    /** 班次 */
    private Integer shiftType;

    /** 角色ID列表 */
    private List<Long> roleIds;

    /** 仓库ID列表 */
    private List<Long> warehouseIds;

    /** 密码（可选，不填则使用默认密码） */
    private String password;
}