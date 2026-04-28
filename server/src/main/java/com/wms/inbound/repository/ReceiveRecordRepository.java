package com.wms.inbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.inbound.entity.ReceiveRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 收货记录Repository
 */
@Mapper
public interface ReceiveRecordRepository extends BaseMapper<ReceiveRecord> {

    /**
     * 查询入库明细的所有收货记录
     */
    @Select("SELECT * FROM wms_receive_record WHERE inbound_item_id = #{itemId} ORDER BY receive_time ASC")
    List<ReceiveRecord> findByItemId(@Param("itemId") Long itemId);

    /**
     * 查询入库单的所有收货记录
     */
    @Select("SELECT * FROM wms_receive_record WHERE inbound_order_id = #{orderId} ORDER BY receive_time ASC")
    List<ReceiveRecord> findByOrderId(@Param("orderId") Long orderId);

    /**
     * 计算入库明细的总收货数量
     */
    @Select("SELECT COALESCE(SUM(receive_qty), 0) FROM wms_receive_record WHERE inbound_item_id = #{itemId}")
    Integer sumReceiveQtyByItemId(@Param("itemId") Long itemId);

    /**
     * 计算入库单的总收货数量
     */
    @Select("SELECT COALESCE(SUM(receive_qty), 0) FROM wms_receive_record WHERE inbound_order_id = #{orderId}")
    Integer sumReceiveQtyByOrderId(@Param("orderId") Long orderId);

    /**
     * 计算入库明细的收货次数
     */
    @Select("SELECT COUNT(*) FROM wms_receive_record WHERE inbound_item_id = #{itemId}")
    Integer countByItemId(@Param("itemId") Long itemId);

    /**
     * 查询最近的收货记录（全局）
     */
    @Select("SELECT * FROM wms_receive_record ORDER BY receive_time DESC LIMIT #{limit}")
    List<ReceiveRecord> findRecent(@Param("limit") int limit);
}
