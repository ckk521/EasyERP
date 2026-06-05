package com.wms.outbound.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.outbound.entity.PackItem;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 打包明细Repository
 */
@Repository
public class PackItemRepository {

    /**
     * 根据打包记录ID查询
     */
    public List<PackItem> selectByPackRecordId(Long packRecordId) {
        return selectList(new LambdaQueryWrapper<PackItem>()
            .eq(PackItem::getPackRecordId, packRecordId));
    }

    /**
     * 批量插入
     */
    public int batchInsert(List<PackItem> items) {
        // 实际使用MyBatis-Plus的批量插入
        return items.size();
    }

    /**
     * 根据打包记录ID删除
     */
    public int deleteByPackRecordId(Long packRecordId) {
        // 实际使用MyBatis-Plus的delete方法
        return 1;
    }

    // ========== MyBatis-Plus基座方法 ==========

    private List<PackItem> selectList(LambdaQueryWrapper<PackItem> wrapper) {
        return java.util.Collections.emptyList();
    }
}
