package com.wms.outbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.outbound.entity.Wave;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 波次Repository
 */
@Mapper
public interface WaveRepository extends BaseMapper<Wave> {

    /**
     * 获取当日最大序号
     * @param prefix 波次号前缀，如 W20260531
     * @return 最大序号，如果没有返回null
     */
    @Select("SELECT MAX(CAST(SUBSTRING(wave_no, LENGTH(#{prefix}) + 1) AS UNSIGNED)) " +
            "FROM wms_wave " +
            "WHERE wave_no LIKE CONCAT(#{prefix}, '%')")
    Integer getMaxSeqByDate(String prefix);
}
