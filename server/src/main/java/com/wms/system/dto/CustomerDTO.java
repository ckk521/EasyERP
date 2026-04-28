package com.wms.system.dto;

import lombok.Data;

@Data
public class CustomerDTO {
    private String code;
    private String name;
    private Integer type;
    private String contact;
    private String phone;
    private String email;
    private String address;
    private java.math.BigDecimal creditLimit;
    private Integer level;
    private String remark;
}
