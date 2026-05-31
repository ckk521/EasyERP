package com.wms.stocktake.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.stocktake.entity.StocktakeApproveRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

/**
 * 盘点审核记录Repository
 */
@Mapper
public interface StocktakeApproveRecordRepository extends BaseMapper<StocktakeApproveRecord> {

    /**
     * 查询盘点单的审核记录
     */
    @Select("SELECT r.id, r.order_id, r.order_no, r.action, r.reason, r.operator_id, r.operator_name, " +
            "r.operator_role_name, r.create_time " +
            "FROM wms_stocktake_approve_record r WHERE r.order_id = #{orderId} ORDER BY r.create_time DESC")
    List<Map<String, Object>> selectByOrderId(@Param("orderId") Long orderId);
}