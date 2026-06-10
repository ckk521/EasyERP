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
            // 创建用户管理相关表（多角色、多仓库、字典）
            createUserManagementTables();
            // 初始化字典数据
            initDictData();
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

            // 添加出库单的客户电话、客户地址字段
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE wms_outbound_order ADD COLUMN customer_phone VARCHAR(50) NULL COMMENT '客户电话' AFTER customer_name"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE wms_outbound_order ADD COLUMN customer_address VARCHAR(200) NULL COMMENT '客户地址' AFTER customer_phone"
                );
                log.info("Added customer phone and address fields to wms_outbound_order");
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("Add customer fields to outbound_order skipped: {}", e.getMessage());
                }
            }

            // 添加出库单的供应商和目标仓库字段
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE wms_outbound_order ADD COLUMN supplier_id BIGINT NULL COMMENT '供应商ID(退货出库)' AFTER customer_address"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE wms_outbound_order ADD COLUMN supplier_code VARCHAR(50) NULL COMMENT '供应商编码' AFTER supplier_id"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE wms_outbound_order ADD COLUMN supplier_name VARCHAR(100) NULL COMMENT '供应商名称(退货出库)' AFTER supplier_code"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE wms_outbound_order ADD COLUMN target_warehouse_id BIGINT NULL COMMENT '目标仓库ID(调拨出库)' AFTER supplier_name"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE wms_outbound_order ADD COLUMN target_warehouse_code VARCHAR(50) NULL COMMENT '目标仓库编码' AFTER target_warehouse_id"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE wms_outbound_order ADD COLUMN target_warehouse_name VARCHAR(100) NULL COMMENT '目标仓库名称(调拨出库)' AFTER target_warehouse_code"
                );
                log.info("Added supplier and target warehouse fields to wms_outbound_order");
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("Add supplier/target warehouse fields skipped: {}", e.getMessage());
                }
            }

            // 添加调拨出库与调拨入库的关联字段
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE wms_outbound_order ADD COLUMN transfer_inbound_id BIGINT NULL COMMENT '调拨入库单ID(调拨出库自动生成)' AFTER target_warehouse_name"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE wms_outbound_order ADD COLUMN transfer_inbound_no VARCHAR(32) NULL COMMENT '调拨入库单号' AFTER transfer_inbound_id"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE wms_inbound_order ADD COLUMN source_outbound_id BIGINT NULL COMMENT '来源出库单ID(调拨入库时填写)' AFTER ref_exception_order_no"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE wms_inbound_order ADD COLUMN source_outbound_no VARCHAR(32) NULL COMMENT '来源出库单号' AFTER source_outbound_id"
                );
                log.info("Added transfer outbound-inbound relation fields");
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("Add transfer relation fields skipped: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("addReplacementFields skipped: {}", e.getMessage());
        }
    }

    /**
     * 创建用户管理相关表
     */
    private void createUserManagementTables() {
        try {
            // 创建用户角色关联表
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS sys_user_role (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "user_id BIGINT NOT NULL COMMENT '用户ID'," +
                "role_id BIGINT NOT NULL COMMENT '角色ID'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "UNIQUE KEY uk_user_role (user_id, role_id)," +
                "INDEX idx_user_id (user_id)," +
                "INDEX idx_role_id (role_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表'"
            );
            log.info("Created sys_user_role table");

            // 创建用户仓库关联表
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS sys_user_warehouse (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "user_id BIGINT NOT NULL COMMENT '用户ID'," +
                "warehouse_id BIGINT NOT NULL COMMENT '仓库ID'," +
                "permission_type INT DEFAULT 1 COMMENT '权限类型: 1可操作 2仅查看'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "UNIQUE KEY uk_user_warehouse (user_id, warehouse_id)," +
                "INDEX idx_user_id (user_id)," +
                "INDEX idx_warehouse_id (warehouse_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户仓库关联表'"
            );
            log.info("Created sys_user_warehouse table");

            // 创建系统字典表
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS sys_dict (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
                "dict_type VARCHAR(50) NOT NULL COMMENT '字典类型'," +
                "dict_code INT NOT NULL COMMENT '字典编码'," +
                "dict_value VARCHAR(100) NOT NULL COMMENT '字典值'," +
                "sort_order INT DEFAULT 0 COMMENT '排序'," +
                "status INT DEFAULT 1 COMMENT '状态: 0禁用 1启用'," +
                "is_system INT DEFAULT 0 COMMENT '是否预置: 0否 1是'," +
                "remark VARCHAR(200) COMMENT '备注'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "UNIQUE KEY uk_type_code (dict_type, dict_code)," +
                "INDEX idx_dict_type (dict_type)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统字典表'"
            );
            log.info("Created sys_dict table");

            // 修改 sys_user 表，添加新字段
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE sys_user ADD COLUMN employee_no VARCHAR(20) NULL COMMENT '员工工号' AFTER username"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE sys_user ADD UNIQUE INDEX idx_employee_no (employee_no)"
                );
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("Add employee_no skipped: {}", e.getMessage());
                }
            }

            try {
                jdbcTemplate.execute(
                    "ALTER TABLE sys_user ADD COLUMN department INT NULL COMMENT '部门' AFTER email"
                );
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("Add department skipped: {}", e.getMessage());
                }
            }

            try {
                jdbcTemplate.execute(
                    "ALTER TABLE sys_user ADD COLUMN position INT NULL COMMENT '岗位' AFTER department"
                );
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("Add position skipped: {}", e.getMessage());
                }
            }

            try {
                jdbcTemplate.execute(
                    "ALTER TABLE sys_user ADD COLUMN hire_date DATE NULL COMMENT '入职日期' AFTER position"
                );
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("Add hire_date skipped: {}", e.getMessage());
                }
            }

            try {
                jdbcTemplate.execute(
                    "ALTER TABLE sys_user ADD COLUMN work_status INT DEFAULT 1 COMMENT '工作状态' AFTER hire_date"
                );
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("Add work_status skipped: {}", e.getMessage());
                }
            }

            try {
                jdbcTemplate.execute(
                    "ALTER TABLE sys_user ADD COLUMN skill_level INT NULL COMMENT '技能等级' AFTER work_status"
                );
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("Add skill_level skipped: {}", e.getMessage());
                }
            }

            try {
                jdbcTemplate.execute(
                    "ALTER TABLE sys_user ADD COLUMN shift_type INT NULL COMMENT '班次类型' AFTER skill_level"
                );
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("Add shift_type skipped: {}", e.getMessage());
                }
            }

            // 添加 login_count 字段（如果不存在）
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE sys_user ADD COLUMN login_count INT DEFAULT 0 COMMENT '登录次数' AFTER last_login_ip"
                );
            } catch (Exception e) {
                if (!e.getMessage().contains("Duplicate column")) {
                    log.debug("Add login_count skipped: {}", e.getMessage());
                }
            }

            // 移除旧的单角色、单仓库字段（MySQL不支持 IF EXISTS，需要单独处理）
            // 先检查列是否存在，然后删除
            try {
                // 检查 role_id 列是否存在
                Integer roleIdExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'role_id'",
                    Integer.class
                );
                if (roleIdExists != null && roleIdExists > 0) {
                    // 先删除外键约束
                    try {
                        jdbcTemplate.execute("ALTER TABLE sys_user DROP FOREIGN KEY fk_user_role");
                    } catch (Exception e) {
                        log.debug("Drop foreign key fk_user_role skipped: {}", e.getMessage());
                    }
                    // 移除约束并删除列
                    jdbcTemplate.execute("ALTER TABLE sys_user MODIFY COLUMN role_id BIGINT NULL");
                    jdbcTemplate.execute("ALTER TABLE sys_user DROP COLUMN role_id");
                    log.info("Dropped role_id column from sys_user");
                }
            } catch (Exception e) {
                log.debug("Drop role_id skipped: {}", e.getMessage());
            }

            try {
                // 检查 warehouse_id 列是否存在
                Integer warehouseIdExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'warehouse_id'",
                    Integer.class
                );
                if (warehouseIdExists != null && warehouseIdExists > 0) {
                    // 先删除外键约束
                    try {
                        jdbcTemplate.execute("ALTER TABLE sys_user DROP FOREIGN KEY fk_user_warehouse");
                    } catch (Exception e) {
                        log.debug("Drop foreign key fk_user_warehouse skipped: {}", e.getMessage());
                    }
                    jdbcTemplate.execute("ALTER TABLE sys_user MODIFY COLUMN warehouse_id BIGINT NULL");
                    jdbcTemplate.execute("ALTER TABLE sys_user DROP COLUMN warehouse_id");
                    log.info("Dropped warehouse_id column from sys_user");
                }
            } catch (Exception e) {
                log.debug("Drop warehouse_id skipped: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.debug("createUserManagementTables skipped: {}", e.getMessage());
        }
    }

    /**
     * 初始化字典数据
     */
    private void initDictData() {
        try {
            // 检查是否已初始化
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_dict WHERE dict_type = 'department'", Integer.class
            );
            if (count != null && count > 0) {
                log.debug("Dict data already initialized");
                return;
            }

            // 部门字典
            jdbcTemplate.update(
                "INSERT INTO sys_dict (dict_type, dict_code, dict_value, sort_order, status, is_system) VALUES " +
                "('department', 1, '入库组', 1, 1, 1)," +
                "('department', 2, '出库组', 2, 1, 1)," +
                "('department', 3, '库存组', 3, 1, 1)," +
                "('department', 4, '管理组', 4, 1, 1)"
            );

            // 岗位字典
            jdbcTemplate.update(
                "INSERT INTO sys_dict (dict_type, dict_code, dict_value, sort_order, status, is_system) VALUES " +
                "('position', 1, '拣货员', 1, 1, 1)," +
                "('position', 2, '打包员', 2, 1, 1)," +
                "('position', 3, '发货员', 3, 1, 1)," +
                "('position', 4, '收货员', 4, 1, 1)," +
                "('position', 5, '质检员', 5, 1, 1)," +
                "('position', 6, '上架员', 6, 1, 1)," +
                "('position', 7, '盘点员', 7, 1, 1)," +
                "('position', 8, '仓库主管', 8, 1, 1)," +
                "('position', 9, '系统管理员', 9, 1, 1)"
            );

            // 工作状态字典
            jdbcTemplate.update(
                "INSERT INTO sys_dict (dict_type, dict_code, dict_value, sort_order, status, is_system) VALUES " +
                "('work_status', 1, '在职', 1, 1, 1)," +
                "('work_status', 2, '休假', 2, 1, 1)," +
                "('work_status', 3, '离职', 3, 1, 1)"
            );

            // 技能等级字典
            jdbcTemplate.update(
                "INSERT INTO sys_dict (dict_type, dict_code, dict_value, sort_order, status, is_system) VALUES " +
                "('skill_level', 1, '初级', 1, 1, 1)," +
                "('skill_level', 2, '中级', 2, 1, 1)," +
                "('skill_level', 3, '高级', 3, 1, 1)," +
                "('skill_level', 4, '专家', 4, 1, 1)"
            );

            // 班次类型字典
            jdbcTemplate.update(
                "INSERT INTO sys_dict (dict_type, dict_code, dict_value, sort_order, status, is_system) VALUES " +
                "('shift_type', 1, '早班', 1, 1, 1)," +
                "('shift_type', 2, '中班', 2, 1, 1)," +
                "('shift_type', 3, '晚班', 3, 1, 1)," +
                "('shift_type', 4, '常白班', 4, 1, 1)"
            );

            log.info("Initialized dict data");
        } catch (Exception e) {
            log.debug("initDictData skipped: {}", e.getMessage());
        }
    }
}
