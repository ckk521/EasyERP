package com.wms.inbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.inbound.entity.RejectRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 不合格品记录Repository
 */
@Mapper
public interface RejectRecordRepository extends BaseMapper<RejectRecord> {

    /**
     * 获取指定日期前缀的最大序号
     */
    @Select("SELECT MAX(CAST(SUBSTRING(return_order_no, 11, 4) AS UNSIGNED)) " +
            "FROM wms_reject_record " +
            "WHERE return_order_no LIKE CONCAT(#{datePrefix}, '%')")
    Integer getMaxReturnSeqByDate(@Param("datePrefix") String datePrefix);
}
