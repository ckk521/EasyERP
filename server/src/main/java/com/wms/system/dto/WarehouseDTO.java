package com.wms.system.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WarehouseDTO {
    private String code;
    private String name;
    private Integer type;
    /**
     * 存储类型（多选，逗号分隔）：1-常温 2-冷藏 3-冷冻 4-恒温
     * 混合仓库(type=5)时必填，如 "1,2,3"
     */
    private String storageTypes;
    private String country;
    private String province;
    private String address;
    private String manager;
    private String phone;
    private BigDecimal area;
}
