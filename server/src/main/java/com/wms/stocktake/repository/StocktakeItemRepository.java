package com.wms.stocktake.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.stocktake.entity.StocktakeItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 盘点明细Repository
 */
@Mapper
public interface StocktakeItemRepository extends BaseMapper<StocktakeItem> {

    /**
     * 查询盘点单的所有明细
     */
    @Select("SELECT * FROM wms_stocktake_item WHERE order_id = #{orderId} ORDER BY id")
    List<StocktakeItem> findByOrderId(@Param("orderId") Long orderId);

    /**
     * 统计已盘点数量
     */
    @Select("SELECT COUNT(*) FROM wms_stocktake_item WHERE order_id = #{orderId} AND status >= 1")
    Integer countByOrderIdAndCounted(@Param("orderId") Long orderId);

    /**
     * 统计差异数量
     */
    @Select("SELECT COUNT(*) FROM wms_stocktake_item WHERE order_id = #{orderId} AND diff_qty != 0")
    Integer countByOrderIdAndDiff(@Param("orderId") Long orderId);
}
