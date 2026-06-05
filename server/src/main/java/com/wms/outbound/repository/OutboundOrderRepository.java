package com.wms.outbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.outbound.entity.OutboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 出库单 Repository
 */
@Mapper
public interface OutboundOrderRepository extends BaseMapper<OutboundOrder> {

    /**
     * 获取指定日期的最大序号
     * @param datePrefix 日期前缀，如 OB20260601
     * @return 最大序号
     */
    @Select("SELECT MAX(CAST(SUBSTRING(order_no, 11) AS UNSIGNED)) FROM wms_outbound_order WHERE order_no LIKE CONCAT(#{datePrefix}, '%')")
    Integer getMaxSeqByDate(@Param("datePrefix") String datePrefix);

    /**
     * 获取销售订单号的最大序号
     * @param datePrefix 日期前缀，如 SO20260601
     * @return 最大序号
     */
    @Select("SELECT MAX(CAST(SUBSTRING(so_no, 11) AS UNSIGNED)) FROM wms_outbound_order WHERE so_no LIKE CONCAT(#{datePrefix}, '%')")
    Integer getMaxSoSeqByDate(@Param("datePrefix") String datePrefix);

    /**
     * 根据订单号查询
     * @param orderNo 订单号
     * @return 出库单
     */
    @Select("SELECT * FROM wms_outbound_order WHERE order_no = #{orderNo}")
    OutboundOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据状态和仓库查询
     * @param status 状态
     * @param warehouseId 仓库ID
     * @return 出库单列表
     */
    @Select("SELECT * FROM wms_outbound_order WHERE status = #{status} AND warehouse_id = #{warehouseId} ORDER BY priority ASC, create_time ASC")
    java.util.List<OutboundOrder> selectByStatusAndWarehouse(@Param("status") Integer status, @Param("warehouseId") Long warehouseId);
}