package com.wms.system.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 用于标注需要按仓库过滤的查询方法
 *
 * 使用示例：
 * @DataScope(warehouseField = "warehouse_id")
 * public List<InboundOrder> listOrders() { ... }
 *
 * @DataScope(warehouseField = "source_warehouse_id", type = DataScope.Type.OR)
 * public List<TransferOrder> listTransfers() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 仓库字段名（默认为warehouse_id）
     */
    String warehouseField() default "warehouse_id";

    /**
     * 数据权限类型
     */
    Type type() default Type.DEFAULT;

    /**
     * 数据权限类型枚举
     */
    enum Type {
        /**
         * 默认：warehouse_id IN (用户仓库列表)
         */
        DEFAULT,

        /**
         * OR关系：source_warehouse_id IN (...) OR target_warehouse_id IN (...)
         * 用于调拨单等跨仓库场景
         */
        OR
    }
}
