package com.wms.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.system.entity.BaseLocation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BaseLocationRepository extends BaseMapper<BaseLocation> {
}
