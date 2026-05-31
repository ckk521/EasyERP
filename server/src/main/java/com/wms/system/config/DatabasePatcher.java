package com.wms.system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库补丁执行器
 * 在应用启动后执行必要的数据库修复
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabasePatcher {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void applyPatches() {
        try {
            // 修复 base_location.code 字段长度
            patchLocationCodeLength();
            // 清理孤立的库位和货架配置（库区已不存在）
            cleanOrphanedData();
            // 修复孤立的货架配置（zoneId 不匹配）
            fixOrphanedShelfConfigs();
            // 创建异常管理模块表
            createExceptionTables();
            // 添加补货入库相关字段
            addReplacementFields();
            log.info("Database patches applied successfully");
        } catch (Exception e) {
            log.warn("Database patch failed (may already be applied): {}", e.getMessage());
        }
    }

    private void patchLocationCodeLength() {
        try {
            jdbcTemplate.execute("ALTER TABLE base_location MODIFY COLUMN code VARCHAR(50) NOT NULL COMMENT '库位编码'");
            log.info("Patched base_location.code to VARCHAR(50)");
        } catch (Exception e) {
            // 忽略已执行的补丁
            if (!e.getMessage().contains("Unknown column") && !e.getMessage().contains("already exists")) {
                log.debug("patchLocationCodeLength skipped: {}", e.getMessage());
            }
        }
    }

    private void cleanOrphanedData() {
        try {
            // 删除孤立的库位（库区不存在）
            int locationsDeleted = jdbcTemplate.update(
                "DELETE FROM base_location WHERE zone_id NOT IN (SELECT id FROM base_zone)"
            );
            if (locationsDeleted > 0) {
                log.info("Deleted {} orphaned locations", locationsDeleted);
            }

            // 删除孤立的货架配置（库区不存在）
            int shelfConfigsDeleted = jdbcTemplate.update(
                "DELETE FROM base_shelf_config WHERE zone_id NOT IN (SELECT id FROM base_zone)"
            );
            if (shelfConfigsDeleted > 0) {
                log.info("Deleted {} orphaned shelf configs", shelfConfigsDeleted);
            }
        } catch (Exception e) {
            log.debug("cleanOrphanedData skipped: {}", e.getMessage());
        }
    }

    private void fixOrphanedShelfConfigs() {
        try {
            // 修复货架配置的 zoneId，使其指向正确的库区
            // 问题：货架配置中 zoneId=4，但实际库区 ID=5（相同编码 WH-TEST-608883-CC-01）
            int updated = jdbcTemplate.update(
                "UPDATE base_shelf_config sc " +
                "JOIN base_zone z ON sc.zone_code = z.code " +
                "SET sc.zone_id = z.id " +
                "WHERE sc.zone_id != z.id"
            );
            if (updated > 0) {
                log.info("Fixed {} orphaned shelf configs", updated);
            }
        } catch (Exception e) {
            log.debug("fixOrphanedShelfConfigs skipped: {}", e.getMessage());
        }
    }

    private void createExceptionTables() {
        try {
            // 创建异常处理单主表
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS wms_exception_order (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "order_no VARCHAR(32) NOT NULL COMMENT '异常处理单号'," +
                "inbound_order_id BIGINT COMMENT '关联入库单ID'," +
                "inbound_order_no VARCHAR(32) COMMENT '入库单号'," +
                "purchase_order_id BIGINT COMMENT '关联采购订单ID'," +
                "purchase_order_no VARCHAR(32) COMMENT '采购订单号'," +
                "supplier_id BIGINT COMMENT '供应商ID'," +
                "supplier_code VARCHAR(50) COMMENT '供应商编码'," +
                "supplier_name VARCHAR(100) COMMENT '供应商名称'," +
                "warehouse_id BIGINT NOT NULL COMMENT '仓库ID'," +
                "warehouse_code VARCHAR(20) NOT NULL COMMENT '仓库编码'," +
                "zone_id BIGINT NULL COMMENT '隔离库区ID'," +
                "zone_code VARCHAR(20) NULL COMMENT '隔离库区编码'," +
                "exception_type TINYINT NOT NULL COMMENT '异常类型'," +
                "total_qty INT NOT NULL COMMENT '异常总数量'," +
                "exception_reason VARCHAR(500) COMMENT '异常原因说明'," +
                "status TINYINT NOT NULL DEFAULT 0 COMMENT '状态'," +
                "handle_type TINYINT COMMENT '处理方式'," +
                "handle_result VARCHAR(500) COMMENT '处理结果说明'," +
                "handle_time DATETIME COMMENT '处理完成时间'," +
                "handle_user_id BIGINT COMMENT '处理人ID'," +
                "handle_user_name VARCHAR(50) COMMENT '处理人姓名'," +
                "source_type TINYINT NOT NULL DEFAULT 1 COMMENT '来源类型'," +
                "remark VARCHAR(500) COMMENT '备注'," +
                "create_user_id BIGINT COMMENT '创建人ID'," +
                "create_user_name VARCHAR(50) COMMENT '创建人姓名'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "UNIQUE KEY uk_order_no (order_no)," +
                "KEY idx_inbound_order (inbound_order_id)," +
                "KEY idx_supplier (supplier_id)," +
                "KEY idx_status (status)," +
                "KEY idx_create_time (create_time)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常处理单主表'"
            );
            log.info("Created wms_exception_order table");

            // 修改 zone_id 允许 NULL（如果表已存在）
            try {
                jdbcTemplate.execute("ALTER TABLE wms_exception_order MODIFY COLUMN zone_id BIGINT NULL COMMENT '隔离库区ID'");
                jdbcTemplate.execute("ALTER TABLE wms_exception_order MODIFY COLUMN zone_code VARCHAR(20) NULL COMMENT '隔离库区编码'");
            } catch (Exception e) {
                // 忽略
            }

            // 创建异常处理明细表
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS wms_exception_item (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "order_id BIGINT NOT NULL COMMENT '异常处理单ID'," +
                "order_no VARCHAR(32) NOT NULL COMMENT '异常处理单号'," +
                "product_id BIGINT NOT NULL COMMENT '商品ID'," +
                "sku_code VARCHAR(50) NOT NULL COMMENT 'SKU编码'," +
                "product_name VARCHAR(200) NOT NULL COMMENT '商品名称'," +
                "barcode VARCHAR(50) COMMENT '条码'," +
                "batch_no VARCHAR(50) COMMENT '批次号'," +
                "exception_qty INT NOT NULL COMMENT '异常数量'," +
                "exception_type TINYINT NOT NULL COMMENT '异常类型'," +
                "exception_reason VARCHAR(500) COMMENT '异常原因'," +
                "location_id BIGINT COMMENT '隔离库位ID'," +
                "location_code VARCHAR(50) COMMENT '隔离库位编码'," +
                "status TINYINT NOT NULL DEFAULT 0 COMMENT '状态'," +
                "handle_type TINYINT COMMENT '处理方式'," +
                "handle_qty INT COMMENT '处理数量'," +
                "handle_result VARCHAR(500) COMMENT '处理结果'," +
                "inbound_item_id BIGINT COMMENT '关联入库明细ID'," +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "KEY idx_order_id (order_id)," +
                "KEY idx_product (product_id)," +
                "KEY idx_status (status)," +
                "KEY idx_inbound_item (inbound_item_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常处理明细表'"
            );
            log.info("Created wms_exception_item table");
        } catch (Exception e) {
            log.debug("createExceptionTables skipped (may already exist): {}", e.getMessage());
        }
    }

    private void addReplacementFields() {
        try {
            // 添加入库单的补货入库关联字段
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE wms_inbound_order ADD COLUMN ref_exception_order_id BIGINT NULL COMMENT '关联的异常处理单ID（补货入库时填写）'"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE wms_inbound_order ADD COLUMN ref_exception_order_no VARCHAR(32) NULL COMMENT '关联的异常处理单号'"
                );
                log.info("Added replacement fields to wms_inbound_order");
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("addReplacementFields to inbound_order skipped: {}", e.getMessage());
                }
            }

            // 添加异常处理单的补货入库关联字段
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE wms_exception_order ADD COLUMN replacement_inbound_order_id BIGINT NULL COMMENT '补货入库单ID（供应商补货后关联）'"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE wms_exception_order ADD COLUMN replacement_inbound_order_no VARCHAR(32) NULL COMMENT '补货入库单号'"
                );
                log.info("Added replacement fields to wms_exception_order");
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("addReplacementFields to exception_order skipped: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("addReplacementFields skipped: {}", e.getMessage());
        }
    }
}
