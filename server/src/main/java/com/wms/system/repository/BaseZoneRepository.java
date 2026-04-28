package com.wms.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.system.entity.BaseZone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BaseZoneRepository extends BaseMapper<BaseZone> {

    @Insert("INSERT INTO base_zone (code, name, warehouse_id, warehouse_code, type, temp_require, storage_types, location_count, status, create_time, update_time) " +
            "VALUES (#{code}, #{name}, #{warehouseId}, #{warehouseCode}, #{type}, #{tempRequire}, #{storageTypes}, #{locationCount}, #{status}, #{createTime}, #{updateTime})")
    int insertZone(BaseZone zone);
}
