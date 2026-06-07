package com.wms.returnorder.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.returnorder.entity.ReturnOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * 退货单 Repository
 */
@Repository
public class ReturnOrderRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RowMapper<ReturnOrder> rowMapper = new BeanPropertyRowMapper<>(ReturnOrder.class);

    /**
     * 插入退货单并返回生成的主键ID
     */
    public int insert(ReturnOrder order) {
        String sql = "INSERT INTO wms_return_order (return_no, original_outbound_id, original_outbound_no, " +
                "customer_id, customer_name, return_reason, return_reason_text, status, " +
                "total_expected_qty, total_received_qty, warehouse_id, warehouse_code, warehouse_name, " +
                "remark, create_user_id, create_user_name, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, order.getReturnNo());
            ps.setLong(2, order.getOriginalOutboundId());
            ps.setString(3, order.getOriginalOutboundNo());
            ps.setObject(4, order.getCustomerId());
            ps.setString(5, order.getCustomerName());
            ps.setObject(6, order.getReturnReason());
            ps.setString(7, order.getReturnReasonText());
            ps.setInt(8, order.getStatus() != null ? order.getStatus() : 0);
            ps.setInt(9, order.getTotalExpectedQty() != null ? order.getTotalExpectedQty() : 0);
            ps.setInt(10, order.getTotalReceivedQty() != null ? order.getTotalReceivedQty() : 0);
            ps.setObject(11, order.getWarehouseId());
            ps.setString(12, order.getWarehouseCode());
            ps.setString(13, order.getWarehouseName());
            ps.setString(14, order.getRemark());
            ps.setObject(15, order.getCreateUserId());
            ps.setString(16, order.getCreateUserName());
            ps.setObject(17, order.getCreateTime());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            order.setId(keyHolder.getKey().longValue());
        }
        return 1;
    }

    /**
     * 更新退货单
     */
    public int updateById(ReturnOrder order) {
        String sql = "UPDATE wms_return_order SET " +
                "original_outbound_id = ?, original_outbound_no = ?, " +
                "customer_id = ?, customer_name = ?, " +
                "return_reason = ?, return_reason_text = ?, " +
                "status = ?, total_expected_qty = ?, total_received_qty = ?, " +
                "inbound_order_id = ?, inbound_order_no = ?, " +
                "warehouse_id = ?, warehouse_code = ?, warehouse_name = ?, " +
                "cancel_reason = ?, remark = ?, " +
                "receive_time = ?, receive_user_id = ?, receive_user_name = ?, " +
                "complete_time = ?, update_time = NOW() " +
                "WHERE id = ?";
        return jdbcTemplate.update(sql,
                order.getOriginalOutboundId(),
                order.getOriginalOutboundNo(),
                order.getCustomerId(),
                order.getCustomerName(),
                order.getReturnReason(),
                order.getReturnReasonText(),
                order.getStatus(),
                order.getTotalExpectedQty(),
                order.getTotalReceivedQty(),
                order.getInboundOrderId(),
                order.getInboundOrderNo(),
                order.getWarehouseId(),
                order.getWarehouseCode(),
                order.getWarehouseName(),
                order.getCancelReason(),
                order.getRemark(),
                order.getReceiveTime(),
                order.getReceiveUserId(),
                order.getReceiveUserName(),
                order.getCompleteTime(),
                order.getId()
        );
    }

    /**
     * 根据ID查询
     */
    public ReturnOrder selectById(Long id) {
        String sql = "SELECT * FROM wms_return_order WHERE id = ?";
        List<ReturnOrder> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 根据退货单号查询
     */
    public ReturnOrder selectByReturnNo(String returnNo) {
        String sql = "SELECT * FROM wms_return_order WHERE return_no = ?";
        List<ReturnOrder> list = jdbcTemplate.query(sql, rowMapper, returnNo);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 分页查询
     */
    public Page<ReturnOrder> selectPage(Integer status, String keyword, int page, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM wms_return_order WHERE 1=1");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM wms_return_order WHERE 1=1");

        List<Object> params = new java.util.ArrayList<>();

        if (status != null) {
            sql.append(" AND status = ?");
            countSql.append(" AND status = ?");
            params.add(status);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (return_no LIKE ? OR original_outbound_no LIKE ?)");
            countSql.append(" AND (return_no LIKE ? OR original_outbound_no LIKE ?)");
            String likeKeyword = "%" + keyword.trim() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
        }

        // 查询总数
        Integer total = jdbcTemplate.queryForObject(countSql.toString(), Integer.class, params.toArray());

        // 分页查询
        sql.append(" ORDER BY create_time DESC LIMIT ?, ?");
        params.add((page - 1) * limit);
        params.add(limit);

        List<ReturnOrder> records = jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());

        Page<ReturnOrder> pageResult = new Page<>(page, limit);
        pageResult.setRecords(records);
        pageResult.setTotal(total != null ? total : 0);

        return pageResult;
    }

    /**
     * 根据原出库单ID查询退货单
     */
    public List<ReturnOrder> selectByOutboundId(Long outboundId) {
        String sql = "SELECT * FROM wms_return_order WHERE original_outbound_id = ? ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, rowMapper, outboundId);
    }
}