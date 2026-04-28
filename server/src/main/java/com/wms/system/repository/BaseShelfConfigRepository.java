package com.wms.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.system.entity.BaseShelfConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BaseShelfConfigRepository extends BaseMapper<BaseShelfConfig> {

    @Select("SELECT * FROM base_shelf_config WHERE zone_id = #{zoneId} ORDER BY row_num ASC")
    List<BaseShelfConfig> findByZoneId(Long zoneId);

    @Select("SELECT * FROM base_shelf_config WHERE warehouse_id = #{warehouseId} ORDER BY row_num ASC")
    List<BaseShelfConfig> findByWarehouseId(Long warehouseId);

    @Select("SELECT * FROM base_shelf_config WHERE code = #{code}")
    BaseShelfConfig findByCode(String code);

    @Select("SELECT COUNT(*) FROM base_shelf_config WHERE zone_id = #{zoneId} AND row_num = #{rowNum}")
    int countByZoneIdAndRowNum(Long zoneId, Integer rowNum);

    default boolean existsByZoneIdAndRowNum(Long zoneId, Integer rowNum) {
        return countByZoneIdAndRowNum(zoneId, rowNum) > 0;
    }
}
