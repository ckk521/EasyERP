package com.wms.inbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.inbound.entity.PutawayRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 上架记录Repository
 */
@Mapper
public interface PutawayRecordRepository extends BaseMapper<PutawayRecord> {

    /**
     * 查询入库明细的所有上架记录
     */
    @Select("SELECT * FROM wms_putaway_record WHERE inbound_item_id = #{itemId} ORDER BY putaway_time ASC")
    List<PutawayRecord> findByItemId(@Param("itemId") Long itemId);

    /**
     * 查询入库单的所有上架记录
     */
    @Select("SELECT * FROM wms_putaway_record WHERE inbound_order_id = #{orderId} ORDER BY putaway_time ASC")
    List<PutawayRecord> findByOrderId(@Param("orderId") Long orderId);

    /**
     * 计算入库明细的总上架数量
     */
    @Select("SELECT COALESCE(SUM(putaway_qty), 0) FROM wms_putaway_record WHERE inbound_item_id = #{itemId}")
    Integer sumPutawayQtyByItemId(@Param("itemId") Long itemId);

    /**
     * 计算入库单的总上架数量
     */
    @Select("SELECT COALESCE(SUM(putaway_qty), 0) FROM wms_putaway_record WHERE inbound_order_id = #{orderId}")
    Integer sumPutawayQtyByOrderId(@Param("orderId") Long orderId);

    /**
     * 计算入库明细的上架次数
     */
    @Select("SELECT COUNT(*) FROM wms_putaway_record WHERE inbound_item_id = #{itemId}")
    Integer countByItemId(@Param("itemId") Long itemId);

    /**
     * 查询最近的上架记录（全局）
     */
    @Select("SELECT * FROM wms_putaway_record ORDER BY putaway_time DESC LIMIT #{limit}")
    List<PutawayRecord> findRecent(@Param("limit") int limit);
}
