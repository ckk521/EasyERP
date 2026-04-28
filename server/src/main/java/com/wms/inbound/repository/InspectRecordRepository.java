package com.wms.inbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.inbound.entity.InspectRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 验收记录Repository
 */
@Mapper
public interface InspectRecordRepository extends BaseMapper<InspectRecord> {

    /**
     * 查询入库明细的所有验收记录
     */
    @Select("SELECT * FROM wms_inspect_record WHERE inbound_item_id = #{itemId} ORDER BY inspect_time ASC")
    List<InspectRecord> findByItemId(@Param("itemId") Long itemId);

    /**
     * 查询入库单的所有验收记录
     */
    @Select("SELECT * FROM wms_inspect_record WHERE inbound_order_id = #{orderId} ORDER BY inspect_time ASC")
    List<InspectRecord> findByOrderId(@Param("orderId") Long orderId);

    /**
     * 计算入库明细的总合格数量
     */
    @Select("SELECT COALESCE(SUM(qualified_qty), 0) FROM wms_inspect_record WHERE inbound_item_id = #{itemId}")
    Integer sumQualifiedQtyByItemId(@Param("itemId") Long itemId);

    /**
     * 计算入库明细的总不合格数量
     */
    @Select("SELECT COALESCE(SUM(rejected_qty), 0) FROM wms_inspect_record WHERE inbound_item_id = #{itemId}")
    Integer sumRejectedQtyByItemId(@Param("itemId") Long itemId);

    /**
     * 计算入库单的总合格数量
     */
    @Select("SELECT COALESCE(SUM(qualified_qty), 0) FROM wms_inspect_record WHERE inbound_order_id = #{orderId}")
    Integer sumQualifiedQtyByOrderId(@Param("orderId") Long orderId);

    /**
     * 计算入库单的总不合格数量
     */
    @Select("SELECT COALESCE(SUM(rejected_qty), 0) FROM wms_inspect_record WHERE inbound_order_id = #{orderId}")
    Integer sumRejectedQtyByOrderId(@Param("orderId") Long orderId);

    /**
     * 计算入库明细的验收次数
     */
    @Select("SELECT COUNT(*) FROM wms_inspect_record WHERE inbound_item_id = #{itemId}")
    Integer countByItemId(@Param("itemId") Long itemId);

    /**
     * 查询最近的验收记录（全局）
     */
    @Select("SELECT * FROM wms_inspect_record ORDER BY inspect_time DESC LIMIT #{limit}")
    List<InspectRecord> findRecent(@Param("limit") int limit);
}
