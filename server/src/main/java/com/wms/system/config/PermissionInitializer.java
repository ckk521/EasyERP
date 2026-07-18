package com.wms.system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 权限数据初始化器
 * 应用启动时自动检查并初始化权限数据
 * 在 DatabaseSchemaPatcher 之后执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)  // 在 SchemaPatcher 之后执行
public class PermissionInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            // 检查权限数据是否已完整初始化（应有57条）
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_permission", Integer.class);

            if (count == null || count < 50) {
                log.info("权限数据不完整（共{}条），开始补充初始化...", count);
                initPermissions();
                log.info("权限数据初始化完成");
            } else {
                log.info("权限数据已完整，共{}条，跳过初始化", count);
            }

            // 检查角色权限关联
            Integer rolePermCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_permission WHERE role_id = " +
                "(SELECT id FROM sys_role WHERE code = 'SYSTEM_ADMIN')", Integer.class);

            if (rolePermCount == null || rolePermCount < 50) {
                log.info("系统管理员角色权限不完整（共{}条），开始补充...", rolePermCount);
                initRolePermissions();
            }

        } catch (Exception e) {
            log.error("权限初始化检查失败: {}", e.getMessage());
        }
    }

    private void initPermissions() {
        // 0. 清理旧的权限数据（不包含 :menu 或 :operation 的权限）
        int deleted = jdbcTemplate.update(
            "DELETE FROM sys_role_permission WHERE permission_id IN " +
            "(SELECT id FROM sys_permission WHERE code NOT LIKE '%:%')");
        if (deleted > 0) {
            log.info("清理旧角色权限关联{}条", deleted);
        }

        deleted = jdbcTemplate.update(
            "DELETE FROM sys_permission WHERE code NOT LIKE '%:%'");
        if (deleted > 0) {
            log.info("清理旧权限数据{}条", deleted);
        }

        // 1. 初始化菜单权限
        initMenuPermissions();

        // 2. 初始化操作权限
        initOperationPermissions();

        // 3. 初始化角色权限关联
        initRolePermissions();

        // 4. 更新系统管理员数据范围
        updateSystemAdminDataScope();
    }

    private void initMenuPermissions() {
        String[][] menuPermissions = {
            {"dashboard:menu", "仪表盘菜单", "dashboard", "menu", "1"},
            {"system:menu", "系统管理菜单", "system", "menu", "2"},
            {"inbound:menu", "入库管理菜单", "inbound", "menu", "3"},
            {"outbound:menu", "出库管理菜单", "outbound", "menu", "4"},
            {"inventory:menu", "库存管理菜单", "inventory", "menu", "5"},
            {"return:menu", "退换货管理菜单", "return", "menu", "6"},
            {"report:menu", "报表分析菜单", "report", "menu", "7"}
        };

        for (String[] perm : menuPermissions) {
            jdbcTemplate.update(
                "INSERT INTO sys_permission (code, name, module, type, sort_order) " +
                "SELECT ?, ?, ?, ?, ? FROM DUAL " +
                "WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = ?)",
                perm[0], perm[1], perm[2], perm[3], Integer.parseInt(perm[4]), perm[0]);
        }
        log.info("菜单权限初始化完成，共{}条", menuPermissions.length);
    }

    private void initOperationPermissions() {
        // 系统管理权限
        String[][] systemPerms = {
            {"system:user:view", "查看用户", "system", "10"},
            {"system:user:create", "创建用户", "system", "11"},
            {"system:user:edit", "编辑用户", "system", "12"},
            {"system:user:delete", "删除用户", "system", "13"},
            {"system:user:resetPwd", "重置密码", "system", "14"},
            {"system:role:view", "查看角色", "system", "15"},
            {"system:dict:view", "查看字典", "system", "16"}
        };

        // 仪表盘权限
        String[][] dashboardPerms = {
            {"dashboard:view", "查看仪表盘", "dashboard", "8"}
        };

        // 入库管理权限
        String[][] inboundPerms = {
            {"inbound:order:view", "查看到货通知单", "inbound", "20"},
            {"inbound:order:create", "创建到货通知单", "inbound", "21"},
            {"inbound:order:edit", "编辑到货通知单", "inbound", "22"},
            {"inbound:order:delete", "删除到货通知单", "inbound", "23"},
            {"inbound:order:approve", "审核到货通知单", "inbound", "24"},
            {"inbound:order:export", "导出到货通知单", "inbound", "25"},
            {"inbound:receive:view", "查看收货任务", "inbound", "26"},
            {"inbound:receive:operate", "执行收货", "inbound", "27"},
            {"inbound:inspect:view", "查看验收任务", "inbound", "28"},
            {"inbound:inspect:operate", "执行验收", "inbound", "29"},
            {"inbound:putaway:view", "查看上架任务", "inbound", "30"},
            {"inbound:putaway:operate", "执行上架", "inbound", "31"}
        };

        // 出库管理权限
        String[][] outboundPerms = {
            {"outbound:order:view", "查看出库单", "outbound", "40"},
            {"outbound:order:create", "创建出库单", "outbound", "41"},
            {"outbound:order:edit", "编辑出库单", "outbound", "42"},
            {"outbound:order:delete", "删除出库单", "outbound", "43"},
            {"outbound:order:approve", "审核出库单", "outbound", "44"},
            {"outbound:order:export", "导出出库单", "outbound", "45"},
            {"outbound:pick:view", "查看拣货任务", "outbound", "46"},
            {"outbound:pick:operate", "执行拣货", "outbound", "47"},
            {"outbound:pack:view", "查看打包任务", "outbound", "48"},
            {"outbound:pack:operate", "执行打包", "outbound", "49"},
            {"outbound:ship:view", "查看发货任务", "outbound", "50"},
            {"outbound:ship:operate", "执行发货", "outbound", "51"}
        };

        // 库存管理权限
        String[][] inventoryPerms = {
            {"inventory:query:view", "库存查询", "inventory", "60"},
            {"inventory:stocktake:view", "查看盘点单", "inventory", "61"},
            {"inventory:stocktake:create", "创建盘点单", "inventory", "62"},
            {"inventory:stocktake:edit", "编辑盘点单", "inventory", "63"},
            {"inventory:stocktake:delete", "删除盘点单", "inventory", "64"},
            {"inventory:stocktake:operate", "执行盘点", "inventory", "65"},
            {"inventory:stocktake:approve", "审核盘点单", "inventory", "66"},
            {"inventory:transfer:view", "查看调拨单", "inventory", "67"},
            {"inventory:transfer:create", "创建调拨单", "inventory", "68"},
            {"inventory:transfer:approve", "审核调拨单", "inventory", "69"}
        };

        // 退换货管理权限
        String[][] returnPerms = {
            {"return:order:view", "查看退货单", "return", "80"},
            {"return:order:create", "创建退货单", "return", "81"},
            {"return:order:edit", "编辑退货单", "return", "82"},
            {"return:order:delete", "删除退货单", "return", "83"},
            {"return:order:approve", "审核退货单", "return", "84"}
        };

        // 报表分析权限
        String[][] reportPerms = {
            {"report:inbound:view", "入库报表", "report", "90"},
            {"report:outbound:view", "出库报表", "report", "91"},
            {"report:inventory:view", "库存报表", "report", "92"},
            {"report:performance:view", "绩效报表", "report", "93"}
        };

        int total = 0;
        total += insertPermissions(systemPerms);
        total += insertPermissions(dashboardPerms);
        total += insertPermissions(inboundPerms);
        total += insertPermissions(outboundPerms);
        total += insertPermissions(inventoryPerms);
        total += insertPermissions(returnPerms);
        total += insertPermissions(reportPerms);

        log.info("操作权限初始化完成，共{}条", total);
    }

    private int insertPermissions(String[][] permissions) {
        int count = 0;
        for (String[] perm : permissions) {
            int updated = jdbcTemplate.update(
                "INSERT INTO sys_permission (code, name, module, type, sort_order) " +
                "SELECT ?, ?, ?, 'operation', ? FROM DUAL " +
                "WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = ?)",
                perm[0], perm[1], perm[2], Integer.parseInt(perm[3]), perm[0]);
            count += updated;
        }
        return count;
    }

    private void initRolePermissions() {
        // 为SYSTEM_ADMIN角色分配所有权限
        jdbcTemplate.update(
            "INSERT INTO sys_role_permission (role_id, permission_id) " +
            "SELECT r.id, p.id FROM sys_role r, sys_permission p " +
            "WHERE r.code = 'SYSTEM_ADMIN' " +
            "AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id)"
        );

        // 为WAREHOUSE_MANAGER分配业务权限（除系统管理外）
        jdbcTemplate.update(
            "INSERT INTO sys_role_permission (role_id, permission_id) " +
            "SELECT r.id, p.id FROM sys_role r, sys_permission p " +
            "WHERE r.code = 'WAREHOUSE_MANAGER' " +
            "AND p.code NOT LIKE 'system:%' " +
            "AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id)"
        );

        log.info("角色权限关联初始化完成");
    }

    private void updateSystemAdminDataScope() {
        // 更新系统管理员数据范围为全部仓库
        jdbcTemplate.update(
            "UPDATE sys_role SET data_scope = 1 WHERE code = 'SYSTEM_ADMIN'"
        );
        log.info("系统管理员数据范围已更新");
    }
}
