package com.wms.system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库Schema补丁
 * 确保权限表有正确的字段结构
 * 必须在 PermissionInitializer 之前执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)  // 最先执行
public class DatabaseSchemaPatcher {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void patch() {
        try {
            // 检查 sys_permission 表是否有 type 字段
            Boolean hasTypeField = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) > 0 FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND COLUMN_NAME = 'type'",
                Boolean.class);

            if (!Boolean.TRUE.equals(hasTypeField)) {
                log.info("sys_permission表缺少type字段，开始添加...");
                jdbcTemplate.execute(
                    "ALTER TABLE sys_permission ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'operation' " +
                    "COMMENT '权限类型: menu-菜单权限, operation-操作权限'");
                log.info("type字段添加成功");
            }

            // 检查 sys_role 表是否有 data_scope 字段
            Boolean hasDataScopeField = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) > 0 FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role' AND COLUMN_NAME = 'data_scope'",
                Boolean.class);

            if (!Boolean.TRUE.equals(hasDataScopeField)) {
                log.info("sys_role表缺少data_scope字段，开始添加...");
                jdbcTemplate.execute(
                    "ALTER TABLE sys_role ADD COLUMN data_scope TINYINT DEFAULT 2 " +
                    "COMMENT '数据权限范围: 1-全部仓库, 2-所属仓库'");
                log.info("data_scope字段添加成功");
            }

        } catch (Exception e) {
            log.error("数据库补丁执行失败: {}", e.getMessage());
        }
    }
}