package com.wms.outbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.outbound.entity.PickTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 拣货任务Repository
 */
@Mapper
public interface PickTaskRepository extends BaseMapper<PickTask> {

    /**
     * 根据波次ID查询拣货任务
     */
    @Select("SELECT * FROM wms_pick_task WHERE wave_id = #{waveId}")
    List<PickTask> selectByWaveId(@Param("waveId") Long waveId);

    /**
     * 查询拣货人待领取的任务
     */
    @Select("SELECT * FROM wms_pick_task WHERE pick_user_id = #{userId} AND status = 0")
    List<PickTask> selectPendingByUserId(@Param("userId") Long userId);

    /**
     * 查询拣货人进行中的任务
     */
    @Select("SELECT * FROM wms_pick_task WHERE pick_user_id = #{userId} AND status = 1")
    List<PickTask> selectInProgressByUserId(@Param("userId") Long userId);

    /**
     * 查询待领取的任务池
     */
    @Select("SELECT * FROM wms_pick_task WHERE status = 0 ORDER BY create_time ASC")
    List<PickTask> selectTaskPool();

    /**
     * 获取当日最大任务序号
     */
    @Select("SELECT MAX(CAST(SUBSTRING(task_no, 11) AS UNSIGNED)) FROM wms_pick_task WHERE task_no LIKE CONCAT(#{prefix}, '%')")
    Integer getMaxSeqByDate(@Param("prefix") String prefix);
}