package com.wms.inbound.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.inbound.entity.InventoryTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 库存事务Repository
 */
@Mapper
public interface InventoryTransactionRepository extends BaseMapper<InventoryTransaction> {

    /**
     * 获取指定日期前缀的最大序号
     */
    @Select("SELECT MAX(CAST(SUBSTRING(transaction_no, 3, 4) AS UNSIGNED)) " +
            "FROM wms_inventory_transaction " +
            "WHERE transaction_no LIKE CONCAT('TX', #{datePrefix}, '%')")
    Integer getMaxSeqByDate(@Param("datePrefix") String datePrefix);
}
