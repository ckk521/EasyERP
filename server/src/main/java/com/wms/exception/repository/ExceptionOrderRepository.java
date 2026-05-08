package com.wms.exception.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.exception.entity.ExceptionOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExceptionOrderRepository extends BaseMapper<ExceptionOrder> {
}
