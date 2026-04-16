package com.wms.system.dto;

import javax.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class ConfigDTO {

    private Long id;

    @NotBlank(message = "配置编码不能为空")
    private String code;

    @NotBlank(message = "配置名称不能为空")
    private String name;

    private String value;

    private String type;

    private String defaultValue;

    private String options;

    private String description;

    private Integer isSystem;

    private String reason;
}
