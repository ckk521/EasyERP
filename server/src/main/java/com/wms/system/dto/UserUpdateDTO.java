package com.wms.system.dto;

import javax.validation.constraints.*;
import lombok.Data;

@Data
public class UserUpdateDTO {

    private Long id;

    // 更新时用户名不允许修改，只做格式校验
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20字符之间")
    private String username;

    // 更新时密码非必填，如果为空则不更新密码
    @Size(min = 6, max = 20, message = "密码长度必须在6-20字符之间")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 100, message = "姓名长度必须在2-100字符之间")
    private String name;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @NotNull(message = "角色不能为空")
    private Long roleId;

    private Long warehouseId;

    private Integer status;
}
