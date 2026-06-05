package com.wms.outbound.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.outbound.entity.BoxType;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 包装箱型Repository
 */
@Repository
public class BoxTypeRepository {

    /**
     * 查询所有启用的箱型
     */
    public List<BoxType> selectAllEnabled() {
        return selectList(new LambdaQueryWrapper<BoxType>()
            .eq(BoxType::getStatus, 1)
            .orderByAsc(BoxType::getSortOrder));
    }

    /**
     * 根据编码查询
     */
    public BoxType selectByCode(String code) {
        return selectOne(new LambdaQueryWrapper<BoxType>()
            .eq(BoxType::getCode, code));
    }

    /**
     * 根据ID查询
     */
    public BoxType selectById(Long id) {
        return selectOne(new LambdaQueryWrapper<BoxType>()
            .eq(BoxType::getId, id));
    }

    // ========== MyBatis-Plus基座方法 ==========

    private BoxType selectOne(LambdaQueryWrapper<BoxType> wrapper) {
        return null;
    }

    private List<BoxType> selectList(LambdaQueryWrapper<BoxType> wrapper) {
        return java.util.Collections.emptyList();
    }
}
