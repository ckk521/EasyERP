package com.wms.system.aspect;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wms.system.annotation.DataScope;
import com.wms.system.repository.SysUserWarehouseRepository;
import com.wms.system.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据权限切面
 * 自动为查询方法添加仓库过滤条件
 *
 * MVP阶段简化实现：直接修改QueryWrapper/LambdaQueryWrapper
 * Phase 2可升级为自定义SQL拦截器
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    private final SysUserWarehouseRepository userWarehouseRepository;

    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint point, DataScope dataScope) throws Throwable {
        // 获取当前用户ID
        Long userId = UserContext.get() != null ? UserContext.get().getUserId() : null;
        if (userId == null) {
            log.warn("数据权限：未获取到用户ID，跳过过滤");
            return point.proceed();
        }

        // 获取用户仓库列表
        List<Long> warehouseIds = userWarehouseRepository.selectWarehouseIdsByUserId(userId);

        // 如果用户没有分配仓库，返回空结果或继续执行
        if (warehouseIds.isEmpty()) {
            log.warn("数据权限：用户未分配仓库, userId={}", userId);
            return point.proceed();
        }

        // 修改查询参数
        Object[] args = point.getArgs();
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof LambdaQueryWrapper) {
                    LambdaQueryWrapper<?> wrapper = (LambdaQueryWrapper<?>) arg;
                    applyDataScopeToLambda(wrapper, dataScope, warehouseIds);
                    break;
                } else if (arg instanceof QueryWrapper) {
                    QueryWrapper<?> wrapper = (QueryWrapper<?>) arg;
                    applyDataScope(wrapper, dataScope, warehouseIds);
                    break;
                }
            }
        }

        return point.proceed();
    }

    /**
     * 应用数据权限过滤（QueryWrapper）
     */
    private void applyDataScope(QueryWrapper<?> wrapper, DataScope dataScope, List<Long> warehouseIds) {
        String warehouseField = dataScope.warehouseField();

        if (dataScope.type() == DataScope.Type.OR) {
            // OR关系：用于调拨单等场景
            log.debug("数据权限：OR类型需要在Mapper中自定义SQL, field={}, warehouseIds={}", warehouseField, warehouseIds);
            wrapper.in(warehouseField, warehouseIds);
        } else {
            log.debug("数据权限：应用IN过滤, field={}, warehouseIds={}", warehouseField, warehouseIds);
            wrapper.in(warehouseField, warehouseIds);
        }
    }

    /**
     * 应用数据权限过滤（LambdaQueryWrapper）
     * 注意：LambdaQueryWrapper使用in方法时需要传入实体类的Lambda表达式
     * 这里使用原生SQL片段作为临时方案
     */
    private void applyDataScopeToLambda(LambdaQueryWrapper<?> wrapper, DataScope dataScope, List<Long> warehouseIds) {
        String warehouseField = dataScope.warehouseField();

        if (dataScope.type() == DataScope.Type.OR) {
            log.debug("数据权限：OR类型需要在Mapper中自定义SQL, field={}, warehouseIds={}", warehouseField, warehouseIds);
        }

        log.debug("数据权限：应用IN过滤, field={}, warehouseIds={}", warehouseField, warehouseIds);

        // 使用apply方法直接拼接SQL片段
        // 生成占位符列表
        String inClause = warehouseIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        wrapper.apply(warehouseField + " IN (" + inClause + ")");
    }
}
