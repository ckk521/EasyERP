package com.wms.exception.dto;

import lombok.Data;
import java.util.List;

/**
 * 隔离入库DTO
 */
@Data
public class IsolateDTO {

    /** 异常处理单ID */
    private Long orderId;

    /** 商品隔离信息列表 */
    private List<ItemLocationDTO> items;

    @Data
    public static class ItemLocationDTO {
        /** 异常明细ID */
        private Long itemId;

        /** 隔离库位ID */
        private Long locationId;

        /** 隔离库位编码 */
        private String locationCode;
    }
}
