package com.wms.inbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.inbound.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 库存Repository
 */
@Mapper
public interface InventoryRepository extends BaseMapper<Inventory> {

    /**
     * 按库位+商品+批次查询库存
     */
    @Select("SELECT * FROM wms_inventory WHERE location_id = #{locationId} AND product_id = #{productId} AND batch_no = #{batchNo}")
    Inventory findByLocationProductBatch(@Param("locationId") Long locationId,
                                         @Param("productId") Long productId,
                                         @Param("batchNo") String batchNo);
}
