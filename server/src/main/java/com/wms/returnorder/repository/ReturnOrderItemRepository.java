package com.wms.returnorder.repository;

import com.wms.returnorder.entity.ReturnOrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 退货单明细 Repository
 */
@Repository
public class ReturnOrderItemRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RowMapper<ReturnOrderItem> rowMapper = new BeanPropertyRowMapper<>(ReturnOrderItem.class);

    /**
     * 批量插入退货单明细
     */
    public int[] batchInsert(List<ReturnOrderItem> items) {
        String sql = "INSERT INTO wms_return_order_item (return_order_id, return_order_no, " +
                "product_id, sku_code, product_name, barcode, original_qty, expected_qty, received_qty, remark, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        List<Object[]> batchArgs = new java.util.ArrayList<>();
        for (ReturnOrderItem item : items) {
            batchArgs.add(new Object[]{
                    item.getReturnOrderId(),
                    item.getReturnOrderNo(),
                    item.getProductId(),
                    item.getSkuCode(),
                    item.getProductName(),
                    item.getBarcode(),
                    item.getOriginalQty(),
                    item.getExpectedQty(),
                    item.getReceivedQty() != null ? item.getReceivedQty() : 0,
                    item.getRemark()
            });
        }

        return jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    /**
     * 根据退货单ID查询明细
     */
    public List<ReturnOrderItem> selectByReturnOrderId(Long returnOrderId) {
        String sql = "SELECT * FROM wms_return_order_item WHERE return_order_id = ? ORDER BY id";
        return jdbcTemplate.query(sql, rowMapper, returnOrderId);
    }

    /**
     * 更新明细
     */
    public int updateById(ReturnOrderItem item) {
        String sql = "UPDATE wms_return_order_item SET received_qty = ?, remark = ?, update_time = NOW() WHERE id = ?";
        return jdbcTemplate.update(sql, item.getReceivedQty(), item.getRemark(), item.getId());
    }

    /**
     * 根据ID查询
     */
    public ReturnOrderItem selectById(Long id) {
        String sql = "SELECT * FROM wms_return_order_item WHERE id = ?";
        List<ReturnOrderItem> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }
}