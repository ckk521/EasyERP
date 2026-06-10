package com.wms.system.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户DTO（用于返回前端）
 */
@Data
public class UserDTO {

    private Long id;
    private String username;
    private String employeeNo;
    private String name;
    private String phone;
    private String email;
    private Integer department;
    private String departmentName;
    private Integer position;
    private String positionName;
    private LocalDate hireDate;
    private Integer workStatus;
    private String workStatusName;
    private Integer skillLevel;
    private String skillLevelName;
    private Integer shiftType;
    private String shiftTypeName;
    private Integer status;
    private String statusName;
    private LocalDateTime lastLoginTime;
    private Integer loginCount;
    private LocalDateTime createTime;

    /** 角色列表 */
    private List<RoleInfo> roles;

    /** 仓库列表 */
    private List<WarehouseInfo> warehouses;

    /**
     * 角色信息
     */
    @Data
    public static class RoleInfo {
        private Long id;
        private String code;
        private String name;
    }

    /**
     * 仓库信息
     */
    @Data
    public static class WarehouseInfo {
        private Long id;
        private String code;
        private String name;
    }
}