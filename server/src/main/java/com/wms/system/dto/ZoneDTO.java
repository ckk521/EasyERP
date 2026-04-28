package com.wms.system.dto;

import lombok.Data;

@Data
public class ZoneDTO {
    private String code;
    private String name;
    private Long warehouseId;
    private Integer type;
    private Integer tempRequire;
    /**
     * 存储类型（继承仓库，可细化）：逗号分隔
     */
    private String storageTypes;
}
