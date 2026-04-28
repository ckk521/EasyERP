package com.wms.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.system.entity.BaseCustomer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BaseCustomerRepository extends BaseMapper<BaseCustomer> {
}
