package com.wms.inventory.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.inbound.entity.Inventory;
import com.wms.inbound.entity.InspectRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 库存Repository扩展
 */
@Mapper
public interface InventoryRepositoryExt extends BaseMapper<Inventory> {

    /**
     * 按库位+商品+批次查询库存
     */
    @Select("SELECT * FROM wms_inventory WHERE location_id = #{locationId} AND product_id = #{productId} AND batch_no = #{batchNo}")
    Inventory findByLocationProductBatch(@Param("locationId") Long locationId,
                                         @Param("productId") Long productId,
                                         @Param("batchNo") String batchNo);

    /**
     * 按商品ID查询库存分布
     */
    @Select("SELECT i.*, l.zone_id, l.zone_code " +
            "FROM wms_inventory i " +
            "LEFT JOIN base_location l ON i.location_id = l.id " +
            "WHERE i.product_id = #{productId} " +
            "ORDER BY i.warehouse_id, l.zone_id, i.location_code")
    List<Inventory> findByProductId(@Param("productId") Long productId);

    /**
     * 按商品ID和仓库查询库存分布
     */
    @Select("SELECT i.*, l.zone_id, l.zone_code " +
            "FROM wms_inventory i " +
            "LEFT JOIN base_location l ON i.location_id = l.id " +
            "WHERE i.product_id = #{productId} AND i.warehouse_id = #{warehouseId} " +
            "ORDER BY l.zone_id, i.location_code")
    List<Inventory> findByProductIdAndWarehouse(@Param("productId") Long productId,
                                                 @Param("warehouseId") Long warehouseId);

    /**
     * 查询效期预警库存
     */
    @Select("SELECT i.*, p.name_cn as product_name, p.sku_code, p.barcode " +
            "FROM wms_inventory i " +
            "LEFT JOIN base_product p ON i.product_id = p.id " +
            "WHERE i.warehouse_id = #{warehouseId} AND i.expiry_status >= 1 " +
            "ORDER BY i.expiry_status DESC, i.expiry_date ASC")
    List<Inventory> findExpiryWarnings(@Param("warehouseId") Long warehouseId);

    /**
     * 查询指定效期状态的库存
     */
    @Select("SELECT i.*, p.name_cn as product_name, p.sku_code, p.barcode " +
            "FROM wms_inventory i " +
            "LEFT JOIN base_product p ON i.product_id = p.id " +
            "WHERE i.warehouse_id = #{warehouseId} AND i.expiry_status = #{expiryStatus} " +
            "ORDER BY i.expiry_date ASC")
    List<Inventory> findByExpiryStatus(@Param("warehouseId") Long warehouseId,
                                        @Param("expiryStatus") Integer expiryStatus);

    /**
     * 查询所有需要更新效期状态的库存
     */
    @Select("SELECT i.*, p.expiry_warning " +
            "FROM wms_inventory i " +
            "LEFT JOIN base_product p ON i.product_id = p.id " +
            "WHERE i.expiry_date IS NOT NULL")
    List<Inventory> findAllWithExpiryDate();

    /**
     * 通过入库单号和商品ID查找验收记录
     */
    @Select("SELECT * FROM wms_inspect_record " +
            "WHERE inbound_order_no = #{orderNo} AND product_id = #{productId} " +
            "ORDER BY inspect_time DESC LIMIT 1")
    InspectRecord findInspectRecordByOrderNoAndProductId(@Param("orderNo") String orderNo,
                                                          @Param("productId") Long productId);
}
