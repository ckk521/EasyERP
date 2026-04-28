package com.wms.system.dto;

import lombok.Data;

@Data
public class SupplierDTO {
    private String code;
    private String name;
    private String contact;
    private String phone;
    private String email;
    private String address;
    private String bankName;
    private String bankAccount;
    private String taxNo;
    private String remark;
}
