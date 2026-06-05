package com.wms.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 包装箱型配置表
 */
@Data
@TableName("wms_box_type")
public class BoxType {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 箱型编码 */
    private String code;

    /** 箱型名称 */
    private String name;

    /** 长度(cm) */
    private BigDecimal length;

    /** 宽度(cm) */
    private BigDecimal width;

    /** 高度(cm) */
    private BigDecimal height;

    /** 体积(m³) */
    private BigDecimal volume;

    /** 最大承重(kg) */
    private BigDecimal maxWeight;

    /** 状态: 0禁用 1启用 */
    private Integer status;

    /** 排序 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
