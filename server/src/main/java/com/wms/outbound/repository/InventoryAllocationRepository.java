package com.wms.outbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.outbound.entity.InventoryAllocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 库存分配 Repository
 */
@Mapper
public interface InventoryAllocationRepository extends BaseMapper<InventoryAllocation> {

    /**
     * 根据出库单ID查询分配记录
     * @param orderId 出库单ID
     * @return 分配记录列表
     */
    @Select("SELECT * FROM wms_inventory_allocation WHERE outbound_order_id = #{orderId}")
    List<InventoryAllocation> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 获取商品可用库存总数
     * @param productId 商品ID
     * @return 可用库存数量
     */
    @Select("SELECT SUM(available_qty) FROM wms_inventory WHERE product_id = #{productId} AND status = 0")
    Integer getTotalAvailableStock(@Param("productId") Long productId);

    /**
     * 获取商品锁定库存总数
     * @param productId 商品ID
     * @return 锁定库存数量
     */
    @Select("SELECT SUM(locked_qty) FROM wms_inventory WHERE product_id = #{productId}")
    Integer getTotalLockedStock(@Param("productId") Long productId);

    /**
     * 锁定库存（原子操作）
     * @param productId 商品ID
     * @param locationId 库位ID
     * @param qty 锁定数量
     * @return 影响行数
     */
    @Update("UPDATE wms_inventory SET available_qty = available_qty - #{qty}, locked_qty = locked_qty + #{qty}, update_time = NOW() WHERE product_id = #{productId} AND location_id = #{locationId} AND available_qty >= #{qty}")
    int lockStock(@Param("productId") Long productId, @Param("locationId") Long locationId, @Param("qty") Integer qty);

    /**
     * 扣减库存（原子操作）
     * @param productId 商品ID
     * @param locationId 库位ID
     * @param qty 扣减数量
     * @return 影响行数
     */
    @Update("UPDATE wms_inventory SET qty = qty - #{qty}, locked_qty = locked_qty - #{qty}, update_time = NOW() WHERE product_id = #{productId} AND location_id = #{locationId} AND locked_qty >= #{qty}")
    int deductStock(@Param("productId") Long productId, @Param("locationId") Long locationId, @Param("qty") Integer qty);

    /**
     * 释放库存（原子操作）
     * @param productId 商品ID
     * @param locationId 库位ID
     * @param qty 释放数量
     * @return 影响行数
     */
    @Update("UPDATE wms_inventory SET locked_qty = locked_qty - #{qty}, available_qty = available_qty + #{qty}, update_time = NOW() WHERE product_id = #{productId} AND location_id = #{locationId} AND locked_qty >= #{qty}")
    int releaseStock(@Param("productId") Long productId, @Param("locationId") Long locationId, @Param("qty") Integer qty);
}
