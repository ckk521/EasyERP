package com.wms.outbound.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.outbound.entity.PackRecord;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 打包记录Repository
 */
@Repository
public class PackRecordRepository {

    /**
     * 根据ID查询
     */
    public PackRecord selectById(Long id) {
        return selectOne(new LambdaQueryWrapper<PackRecord>()
            .eq(PackRecord::getId, id));
    }

    /**
     * 根据出库单ID查询
     */
    public List<PackRecord> selectByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapper<PackRecord>()
            .eq(PackRecord::getOutboundOrderId, orderId)
            .orderByDesc(PackRecord::getCreateTime));
    }

    /**
     * 根据出库单ID查询最新记录
     */
    public PackRecord selectLatestByOrderId(Long orderId) {
        return selectOne(new LambdaQueryWrapper<PackRecord>()
            .eq(PackRecord::getOutboundOrderId, orderId)
            .orderByDesc(PackRecord::getCreateTime)
            .last("LIMIT 1"));
    }

    /**
     * 查询待打包的记录
     */
    public List<PackRecord> selectPendingOrders(Long warehouseId) {
        return selectList(new LambdaQueryWrapper<PackRecord>()
            .eq(PackRecord::getStatus, PackRecord.STATUS_PENDING)
            .orderByAsc(PackRecord::getCreateTime));
    }

    /**
     * 查询某个打包员的进行中任务
     */
    public List<PackRecord> selectInProgressByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<PackRecord>()
            .eq(PackRecord::getPackUserId, userId)
            .eq(PackRecord::getStatus, PackRecord.STATUS_PACKING)
            .orderByAsc(PackRecord::getClaimTime));
    }

    /**
     * 根据包裹号查询
     */
    public PackRecord selectByPackageNo(String packageNo) {
        return selectOne(new LambdaQueryWrapper<PackRecord>()
            .eq(PackRecord::getPackageNo, packageNo));
    }

    /**
     * 插入
     */
    public int insert(PackRecord record) {
        // 实际使用MyBatis-Plus的insert方法
        return 1;
    }

    /**
     * 更新
     */
    public int updateById(PackRecord record) {
        // 实际使用MyBatis-Plus的updateById方法
        return 1;
    }

    /**
     * 删除
     */
    public int deleteById(Long id) {
        // 实际使用MyBatis-Plus的deleteById方法
        return 1;
    }

    // ========== MyBatis-Plus基座方法（实际项目中由BaseMapper提供） ==========

    private PackRecord selectOne(LambdaQueryWrapper<PackRecord> wrapper) {
        // Mock实现，实际由MyBatis-Plus提供
        return null;
    }

    private List<PackRecord> selectList(LambdaQueryWrapper<PackRecord> wrapper) {
        // Mock实现，实际由MyBatis-Plus提供
        return java.util.Collections.emptyList();
    }
}
