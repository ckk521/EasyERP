package com.wms.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.system.entity.BaseCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BaseCategoryRepository extends BaseMapper<BaseCategory> {
}
