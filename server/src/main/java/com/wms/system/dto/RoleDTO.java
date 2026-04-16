package com.wms.system.dto;

import javax.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class RoleDTO {

    private Long id;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过50字符")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称长度不能超过100字符")
    private String name;

    private String description;

    private List<Long> permissionIds;

    private Integer status;
}
