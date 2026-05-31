package com.wms.stocktake.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.stocktake.entity.StocktakeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 盘点单Repository
 */
@Mapper
public interface StocktakeOrderRepository extends BaseMapper<StocktakeOrder> {

    /**
     * 获取当天最大序号
     * 盘点单号格式：ST + yyyyMMdd + 4位序号
     * 只取后4位作为序号，避免异常订单号导致的溢出问题
     */
    @Select("SELECT IFNULL(MAX(CAST(RIGHT(order_no, 4) AS SIGNED)), 0) FROM wms_stocktake_order WHERE order_no LIKE CONCAT(#{prefix}, '%') AND LENGTH(order_no) = 14")
    Integer getMaxSeqByDate(@Param("prefix") String prefix);
}
