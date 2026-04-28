package com.wms.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.system.entity.BaseProduct;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BaseProductRepository extends BaseMapper<BaseProduct> {
}
