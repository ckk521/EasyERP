package com.wms.inbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.inbound.entity.InboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 入库单 Repository
 */
@Mapper
public interface InboundOrderRepository extends BaseMapper<InboundOrder> {

    /**
     * 获取指定日期的最大序号
     * @param datePrefix 日期前缀，如 IN20260421
     * @return 最大序号
     */
    @Select("SELECT MAX(CAST(SUBSTRING(order_no, -4) AS UNSIGNED)) FROM wms_inbound_order WHERE order_no LIKE CONCAT(#{datePrefix}, '%')")
    Integer getMaxSeqByDate(@Param("datePrefix") String datePrefix);
}
