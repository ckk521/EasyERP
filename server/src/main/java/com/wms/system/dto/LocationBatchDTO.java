package com.wms.system.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class LocationBatchDTO {
    private Long zoneId;
    private Integer startRow;
    private Integer endRow;
    private Integer startCol;
    private Integer endCol;
    private Integer startLayer;
    private Integer endLayer;
    private Integer type;
    /**
     * 存储类型：1-常温 2-冷藏 3-冷冻 4-恒温
     */
    private Integer storageType;
    private BigDecimal maxLength;
    private BigDecimal maxWidth;
    private BigDecimal maxHeight;
    private BigDecimal maxWeight;
}
