package com.wms.outbound.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.outbound.entity.ShipRecord;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 发货记录Repository
 */
@Repository
public class ShipRecordRepository {

    /**
     * 根据ID查询
     */
    public ShipRecord selectById(Long id) {
        return selectOne(new LambdaQueryWrapper<ShipRecord>()
            .eq(ShipRecord::getId, id));
    }

    /**
     * 根据出库单ID查询
     */
    public List<ShipRecord> selectByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapper<ShipRecord>()
            .eq(ShipRecord::getOutboundOrderId, orderId)
            .orderByDesc(ShipRecord::getShipTime));
    }

    /**
     * 根据包裹号查询
     */
    public ShipRecord selectByPackageNo(String packageNo) {
        return selectOne(new LambdaQueryWrapper<ShipRecord>()
            .eq(ShipRecord::getPackageNo, packageNo));
    }

    /**
     * 根据物流单号查询
     */
    public ShipRecord selectByTrackingNo(String trackingNo) {
        return selectOne(new LambdaQueryWrapper<ShipRecord>()
            .eq(ShipRecord::getTrackingNo, trackingNo));
    }

    /**
     * 查询待发货记录
     */
    public List<ShipRecord> selectPendingShipments(Long warehouseId) {
        return selectList(new LambdaQueryWrapper<ShipRecord>()
            .eq(ShipRecord::getStatus, ShipRecord.STATUS_PENDING)
            .orderByAsc(ShipRecord::getCreateTime));
    }

    /**
     * 查询某个时间段的发货记录
     */
    public List<ShipRecord> selectByTimeRange(String startTime, String endTime) {
        return selectList(new LambdaQueryWrapper<ShipRecord>()
            .eq(ShipRecord::getStatus, ShipRecord.STATUS_SHIPPED)
            .orderByDesc(ShipRecord::getShipTime));
    }

    /**
     * 插入
     */
    public int insert(ShipRecord record) {
        return 1;
    }

    /**
     * 更新
     */
    public int updateById(ShipRecord record) {
        return 1;
    }

    /**
     * 删除
     */
    public int deleteById(Long id) {
        return 1;
    }

    // ========== MyBatis-Plus基座方法 ==========

    private ShipRecord selectOne(LambdaQueryWrapper<ShipRecord> wrapper) {
        return null;
    }

    private List<ShipRecord> selectList(LambdaQueryWrapper<ShipRecord> wrapper) {
        return java.util.Collections.emptyList();
    }
}
