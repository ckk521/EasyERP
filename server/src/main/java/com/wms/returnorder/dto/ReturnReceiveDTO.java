package com.wms.returnorder.dto;

import lombok.Data;
import java.util.List;

/**
 * 退货收货DTO
 */
@Data
public class ReturnReceiveDTO {

    /** 退货单ID */
    private Long returnOrderId;

    /** 收货人ID */
    private Long receiveUserId;

    /** 收货人姓名 */
    private String receiveUserName;

    /** 收货商品明细 */
    private List<ReceiveItemDTO> items;

    /**
     * 收货商品明细DTO
     */
    @Data
    public static class ReceiveItemDTO {
        /** 退货单明细ID */
        private Long itemId;

        /** 实收数量 */
        private Integer receivedQty;

        /** 收货备注 */
        private String remark;
    }
}