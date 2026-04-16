package com.wms.system.dto;

import lombok.Data;

@Data
public class PageDTO {
    private Integer page = 1;
    private Integer limit = 20;

    public Integer getOffset() {
        return (page - 1) * limit;
    }
}
