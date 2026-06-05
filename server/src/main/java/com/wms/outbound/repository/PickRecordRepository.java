package com.wms.outbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.outbound.entity.PickRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 拣货记录Repository
 */
@Mapper
public interface PickRecordRepository extends BaseMapper<PickRecord> {

    /**
     * 根据波次ID查询拣货记录
     */
    @Select("SELECT * FROM wms_pick_record WHERE wave_id = #{waveId} ORDER BY sort_order ASC")
    List<PickRecord> selectByWaveId(@Param("waveId") Long waveId);

    /**
     * 根据出库单ID查询拣货记录
     */
    @Select("SELECT * FROM wms_pick_record WHERE outbound_order_id = #{orderId} ORDER BY sort_order ASC")
    List<PickRecord> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据拣货人ID查询拣货记录
     */
    @Select("SELECT * FROM wms_pick_record WHERE pick_user_id = #{userId} AND status IN (0, 1) ORDER BY sort_order ASC")
    List<PickRecord> selectPendingByUserId(@Param("userId") Long userId);

    /**
     * 统计波次已完成的拣货数量
     */
    @Select("SELECT COALESCE(SUM(actual_qty), 0) FROM wms_pick_record WHERE wave_id = #{waveId} AND status = 2")
    int sumPickedQtyByWaveId(@Param("waveId") Long waveId);

    /**
     * 统计波次异常的拣货数量
     */
    @Select("SELECT COALESCE(SUM(exception_qty), 0) FROM wms_pick_record WHERE wave_id = #{waveId} AND is_exception = 1")
    int sumExceptionQtyByWaveId(@Param("waveId") Long waveId);
}