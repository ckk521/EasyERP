package com.wms.inbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.inbound.entity.InboundOrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入库单明细 Repository
 */
@Mapper
public interface InboundOrderItemRepository extends BaseMapper<InboundOrderItem> {
}
