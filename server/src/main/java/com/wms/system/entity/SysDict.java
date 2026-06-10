package com.wms.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统字典表
 */
@Data
@TableName("sys_dict")
public class SysDict {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型 */
    private String dictType;

    /** 字典编码 */
    private Integer dictCode;

    /** 字典值 */
    private String dictValue;

    /** 排序 */
    private Integer sortOrder;

    /** 状态: 0禁用 1启用 */
    private Integer status;

    /** 是否预置: 0否 1是 */
    private Integer isSystem;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}