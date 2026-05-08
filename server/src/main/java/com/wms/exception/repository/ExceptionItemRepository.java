package com.wms.exception.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.exception.entity.ExceptionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExceptionItemRepository extends BaseMapper<ExceptionItem> {

    /**
     * 查询入库明细已被隔离的数量（异常处理单状态为待处理或处理中）
     *
     * @param inboundItemId 入库明细ID
     * @return 已隔离数量
     */
    @Select("SELECT COALESCE(SUM(exception_qty), 0) FROM wms_exception_item " +
            "WHERE inbound_item_id = #{inboundItemId} " +
            "AND status IN (0, 1) " +
            "AND order_id IN (SELECT id FROM wms_exception_order WHERE status IN (0, 1))")
    Integer sumIsolatedQtyByInboundItemId(@Param("inboundItemId") Long inboundItemId);
}
