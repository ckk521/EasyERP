package com.wms.outbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.outbound.entity.OutboundOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 出库单明细 Repository
 */
@Mapper
public interface OutboundOrderItemRepository extends BaseMapper<OutboundOrderItem> {

    /**
     * 获取商品可用库存
     * @param productId 商品ID
     * @return 可用库存数量
     */
    @Select("SELECT SUM(available_qty) FROM wms_inventory WHERE product_id = #{productId}")
    Integer getAvailableStock(@Param("productId") Long productId);

    /**
     * 释放锁定库存
     * @param productId 商品ID
     * @param qty 释放数量
     * @return 是否成功
     */
    @Update("UPDATE wms_inventory SET locked_qty = locked_qty - #{qty}, available_qty = available_qty + #{qty} WHERE product_id = #{productId} AND locked_qty >= #{qty}")
    boolean releaseLockedStock(@Param("productId") Long productId, @Param("qty") Integer qty);

    /**
     * 汇总出库单的已拣货数量
     * @param orderId 出库单ID
     * @return 已拣货数量
     */
    @Select("SELECT SUM(picked_qty) FROM wms_outbound_order_item WHERE order_id = #{orderId}")
    Integer sumPickedQtyByOrderId(@Param("orderId") Long orderId);

    /**
     * 汇总出库单的已打包数量
     * @param orderId 出库单ID
     * @return 已打包数量
     */
    @Select("SELECT SUM(packed_qty) FROM wms_outbound_order_item WHERE order_id = #{orderId}")
    Integer sumPackedQtyByOrderId(@Param("orderId") Long orderId);

    /**
     * 汇总出库单的已发货数量
     * @param orderId 出库单ID
     * @return 已发货数量
     */
    @Select("SELECT SUM(shipped_qty) FROM wms_outbound_order_item WHERE order_id = #{orderId}")
    Integer sumShippedQtyByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据出库单ID查询明细列表
     * @param orderId 出库单ID
     * @return 明细列表
     */
    @Select("SELECT * FROM wms_outbound_order_item WHERE order_id = #{orderId} ORDER BY id ASC")
    java.util.List<OutboundOrderItem> selectByOrderId(@Param("orderId") Long orderId);
}