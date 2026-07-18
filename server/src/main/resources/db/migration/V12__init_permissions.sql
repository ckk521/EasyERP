-- ========================================
-- V12: 权限数据初始化脚本
-- 创建时间: 2026-06-10
-- 说明: 初始化所有权限数据和角色权限关联
-- ========================================

-- ========================================
-- 1. 修改权限表结构，增加type字段
-- ========================================
ALTER TABLE sys_permission
ADD COLUMN IF NOT EXISTS type VARCHAR(20) NOT NULL DEFAULT 'operation'
COMMENT '权限类型: menu-菜单权限, operation-操作权限';

-- ========================================
-- 2. 修改角色表结构，增加data_scope字段
-- ========================================
ALTER TABLE sys_role
ADD COLUMN IF NOT EXISTS data_scope TINYINT DEFAULT 2
COMMENT '数据权限范围: 1-全部仓库, 2-所属仓库';

-- ========================================
-- 3. 初始化菜单权限 (7个模块)
-- ========================================

-- 仪表盘
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'dashboard:menu', '仪表盘菜单', 'dashboard', 'menu', 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'dashboard:menu');

-- 系统管理
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'system:menu', '系统管理菜单', 'system', 'menu', 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:menu');

-- 入库管理
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:menu', '入库管理菜单', 'inbound', 'menu', 3
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:menu');

-- 出库管理
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:menu', '出库管理菜单', 'outbound', 'menu', 4
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:menu');

-- 库存管理
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inventory:menu', '库存管理菜单', 'inventory', 'menu', 5
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:menu');

-- 退换货管理
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'return:menu', '退换货管理菜单', 'return', 'menu', 6
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'return:menu');

-- 报表分析
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'report:menu', '报表分析菜单', 'report', 'menu', 7
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'report:menu');

-- ========================================
-- 4. 初始化仪表盘权限 (operation)
-- ========================================
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'dashboard:view', '查看仪表盘', 'dashboard', 'operation', 8
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'dashboard:view');

-- ========================================
-- 5. 初始化系统管理权限 (operation)
-- ========================================
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'system:user:view', '查看用户', 'system', 'operation', 10
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'system:user:create', '创建用户', 'system', 'operation', 11
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:create');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'system:user:edit', '编辑用户', 'system', 'operation', 12
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:edit');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'system:user:delete', '删除用户', 'system', 'operation', 13
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:delete');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'system:user:resetPwd', '重置密码', 'system', 'operation', 14
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:user:resetPwd');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'system:role:view', '查看角色', 'system', 'operation', 15
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:role:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'system:dict:view', '查看字典', 'system', 'operation', 16
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:dict:view');

-- ========================================
-- 6. 初始化入库管理权限 (operation)
-- ========================================
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:order:view', '查看出库单', 'inbound', 'operation', 20
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:order:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:order:create', '创建入库单', 'inbound', 'operation', 21
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:order:create');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:order:edit', '编辑入库单', 'inbound', 'operation', 22
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:order:edit');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:order:delete', '删除入库单', 'inbound', 'operation', 23
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:order:delete');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:order:approve', '审核入库单', 'inbound', 'operation', 24
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:order:approve');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:order:export', '导出入库单', 'inbound', 'operation', 25
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:order:export');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:receive:view', '查看收货任务', 'inbound', 'operation', 26
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:receive:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:receive:operate', '执行收货', 'inbound', 'operation', 27
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:receive:operate');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:inspect:view', '查看验收任务', 'inbound', 'operation', 28
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:inspect:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:inspect:operate', '执行验收', 'inbound', 'operation', 29
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:inspect:operate');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:putaway:view', '查看上架任务', 'inbound', 'operation', 30
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:putaway:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inbound:putaway:operate', '执行上架', 'inbound', 'operation', 31
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:putaway:operate');

-- ========================================
-- 7. 初始化出库管理权限 (operation)
-- ========================================
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:order:view', '查看出库单', 'outbound', 'operation', 40
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:order:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:order:create', '创建出库单', 'outbound', 'operation', 41
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:order:create');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:order:edit', '编辑出库单', 'outbound', 'operation', 42
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:order:edit');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:order:delete', '删除出库单', 'outbound', 'operation', 43
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:order:delete');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:order:approve', '审核出库单', 'outbound', 'operation', 44
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:order:approve');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:order:export', '导出出库单', 'outbound', 'operation', 45
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:order:export');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:pick:view', '查看拣货任务', 'outbound', 'operation', 46
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:pick:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:pick:operate', '执行拣货', 'outbound', 'operation', 47
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:pick:operate');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:pack:view', '查看打包任务', 'outbound', 'operation', 48
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:pack:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:pack:operate', '执行打包', 'outbound', 'operation', 49
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:pack:operate');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:ship:view', '查看发货任务', 'outbound', 'operation', 50
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:ship:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'outbound:ship:operate', '执行发货', 'outbound', 'operation', 51
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'outbound:ship:operate');

-- ========================================
-- 8. 初始化库存管理权限 (operation)
-- ========================================
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inventory:query:view', '库存查询', 'inventory', 'operation', 60
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:query:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inventory:stocktake:view', '查看盘点单', 'inventory', 'operation', 61
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:stocktake:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inventory:stocktake:create', '创建盘点单', 'inventory', 'operation', 62
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:stocktake:create');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inventory:stocktake:edit', '编辑盘点单', 'inventory', 'operation', 63
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:stocktake:edit');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inventory:stocktake:delete', '删除盘点单', 'inventory', 'operation', 64
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:stocktake:delete');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inventory:stocktake:operate', '执行盘点', 'inventory', 'operation', 65
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:stocktake:operate');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inventory:stocktake:approve', '审核盘点单', 'inventory', 'operation', 66
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:stocktake:approve');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inventory:transfer:view', '查看调拨单', 'inventory', 'operation', 67
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:transfer:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inventory:transfer:create', '创建调拨单', 'inventory', 'operation', 68
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:transfer:create');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'inventory:transfer:approve', '审核调拨单', 'inventory', 'operation', 69
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:transfer:approve');

-- ========================================
-- 9. 初始化退换货管理权限 (operation)
-- ========================================
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'return:order:view', '查看退货单', 'return', 'operation', 80
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'return:order:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'return:order:create', '创建退货单', 'return', 'operation', 81
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'return:order:create');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'return:order:edit', '编辑退货单', 'return', 'operation', 82
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'return:order:edit');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'return:order:delete', '删除退货单', 'return', 'operation', 83
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'return:order:delete');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'return:order:approve', '审核退货单', 'return', 'operation', 84
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'return:order:approve');

-- ========================================
-- 10. 初始化报表分析权限 (operation)
-- ========================================
INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'report:inbound:view', '入库报表', 'report', 'operation', 90
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'report:inbound:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'report:outbound:view', '出库报表', 'report', 'operation', 91
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'report:outbound:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'report:inventory:view', '库存报表', 'report', 'operation', 92
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'report:inventory:view');

INSERT INTO sys_permission (code, name, module, type, sort_order)
SELECT 'report:performance:view', '绩效报表', 'report', 'operation', 93
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'report:performance:view');

-- ========================================
-- 11. 更新角色数据范围
-- ========================================
-- 系统管理员可以查看全部仓库
UPDATE sys_role SET data_scope = 1 WHERE code = 'SYSTEM_ADMIN';

-- 其他角色只能查看所属仓库
UPDATE sys_role SET data_scope = 2 WHERE code IN (
    'WAREHOUSE_MANAGER', 'PICKER', 'PACKER', 'SHIPPER',
    'RECEIVER', 'INSPECTOR', 'PUTAWAY_CLERK', 'STOCKTAKER'
);

-- ========================================
-- 12. 初始化角色权限关联 - 系统管理员
-- ========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'SYSTEM_ADMIN'
AND p.code IN (
    -- 菜单权限 (全部)
    'dashboard:menu', 'system:menu', 'inbound:menu', 'outbound:menu', 'inventory:menu', 'return:menu', 'report:menu',
    -- 仪表盘
    'dashboard:view',
    -- 系统管理 (全部)
    'system:user:view', 'system:user:create', 'system:user:edit', 'system:user:delete', 'system:user:resetPwd',
    'system:role:view', 'system:dict:view',
    -- 入库管理 (全部)
    'inbound:order:view', 'inbound:order:create', 'inbound:order:edit', 'inbound:order:delete', 'inbound:order:approve', 'inbound:order:export',
    'inbound:receive:view', 'inbound:receive:operate',
    'inbound:inspect:view', 'inbound:inspect:operate',
    'inbound:putaway:view', 'inbound:putaway:operate',
    -- 出库管理 (全部)
    'outbound:order:view', 'outbound:order:create', 'outbound:order:edit', 'outbound:order:delete', 'outbound:order:approve', 'outbound:order:export',
    'outbound:pick:view', 'outbound:pick:operate',
    'outbound:pack:view', 'outbound:pack:operate',
    'outbound:ship:view', 'outbound:ship:operate',
    -- 库存管理 (全部)
    'inventory:query:view',
    'inventory:stocktake:view', 'inventory:stocktake:create', 'inventory:stocktake:edit', 'inventory:stocktake:delete', 'inventory:stocktake:operate', 'inventory:stocktake:approve',
    'inventory:transfer:view', 'inventory:transfer:create', 'inventory:transfer:approve',
    -- 退换货 (全部)
    'return:order:view', 'return:order:create', 'return:order:edit', 'return:order:delete', 'return:order:approve',
    -- 报表 (全部)
    'report:inbound:view', 'report:outbound:view', 'report:inventory:view', 'report:performance:view'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ========================================
-- 13. 初始化角色权限关联 - 仓库主管
-- ========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'WAREHOUSE_MANAGER'
AND p.code IN (
    -- 菜单权限
    'dashboard:menu', 'inbound:menu', 'outbound:menu', 'inventory:menu', 'return:menu', 'report:menu',
    -- 仪表盘
    'dashboard:view',
    -- 入库管理 (全部)
    'inbound:order:view', 'inbound:order:create', 'inbound:order:edit', 'inbound:order:delete', 'inbound:order:approve', 'inbound:order:export',
    'inbound:receive:view', 'inbound:receive:operate',
    'inbound:inspect:view', 'inbound:inspect:operate',
    'inbound:putaway:view', 'inbound:putaway:operate',
    -- 出库管理 (全部)
    'outbound:order:view', 'outbound:order:create', 'outbound:order:edit', 'outbound:order:delete', 'outbound:order:approve', 'outbound:order:export',
    'outbound:pick:view', 'outbound:pick:operate',
    'outbound:pack:view', 'outbound:pack:operate',
    'outbound:ship:view', 'outbound:ship:operate',
    -- 库存管理 (全部)
    'inventory:query:view',
    'inventory:stocktake:view', 'inventory:stocktake:create', 'inventory:stocktake:edit', 'inventory:stocktake:delete', 'inventory:stocktake:operate', 'inventory:stocktake:approve',
    'inventory:transfer:view', 'inventory:transfer:create', 'inventory:transfer:approve',
    -- 退换货 (全部)
    'return:order:view', 'return:order:create', 'return:order:edit', 'return:order:delete', 'return:order:approve',
    -- 报表 (全部)
    'report:inbound:view', 'report:outbound:view', 'report:inventory:view', 'report:performance:view'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ========================================
-- 14. 初始化角色权限关联 - 拣货员
-- ========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'PICKER'
AND p.code IN (
    -- 菜单权限
    'dashboard:menu', 'outbound:menu',
    -- 仪表盘
    'dashboard:view',
    -- 出库管理 (拣货相关)
    'outbound:order:view',
    'outbound:pick:view', 'outbound:pick:operate',
    -- 库存管理 (查看)
    'inventory:query:view',
    -- 报表 (出库报表)
    'report:outbound:view'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ========================================
-- 15. 初始化角色权限关联 - 打包员
-- ========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'PACKER'
AND p.code IN (
    -- 菜单权限
    'dashboard:menu', 'outbound:menu',
    -- 仪表盘
    'dashboard:view',
    -- 出库管理 (打包相关)
    'outbound:order:view',
    'outbound:pack:view', 'outbound:pack:operate',
    -- 库存管理 (查看)
    'inventory:query:view',
    -- 报表 (出库报表)
    'report:outbound:view'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ========================================
-- 16. 初始化角色权限关联 - 发货员
-- ========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'SHIPPER'
AND p.code IN (
    -- 菜单权限
    'dashboard:menu', 'outbound:menu',
    -- 仪表盘
    'dashboard:view',
    -- 出库管理 (发货相关)
    'outbound:order:view',
    'outbound:ship:view', 'outbound:ship:operate',
    -- 库存管理 (查看)
    'inventory:query:view',
    -- 报表 (出库报表)
    'report:outbound:view'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ========================================
-- 17. 初始化角色权限关联 - 收货员
-- ========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'RECEIVER'
AND p.code IN (
    -- 菜单权限
    'dashboard:menu', 'inbound:menu',
    -- 仪表盘
    'dashboard:view',
    -- 入库管理 (收货相关)
    'inbound:order:view',
    'inbound:receive:view', 'inbound:receive:operate',
    -- 库存管理 (查看)
    'inventory:query:view',
    -- 报表 (入库报表)
    'report:inbound:view'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ========================================
-- 18. 初始化角色权限关联 - 质检员
-- ========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'INSPECTOR'
AND p.code IN (
    -- 菜单权限
    'dashboard:menu', 'inbound:menu',
    -- 仪表盘
    'dashboard:view',
    -- 入库管理 (验收相关)
    'inbound:order:view',
    'inbound:inspect:view', 'inbound:inspect:operate',
    -- 库存管理 (查看)
    'inventory:query:view',
    -- 报表 (入库报表)
    'report:inbound:view'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ========================================
-- 19. 初始化角色权限关联 - 上架员
-- ========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'PUTAWAY_CLERK'
AND p.code IN (
    -- 菜单权限
    'dashboard:menu', 'inbound:menu',
    -- 仪表盘
    'dashboard:view',
    -- 入库管理 (上架相关)
    'inbound:order:view',
    'inbound:putaway:view', 'inbound:putaway:operate',
    -- 库存管理 (查看)
    'inventory:query:view',
    -- 报表 (入库报表)
    'report:inbound:view'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ========================================
-- 20. 初始化角色权限关联 - 盘点员
-- ========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'STOCKTAKER'
AND p.code IN (
    -- 菜单权限
    'dashboard:menu', 'inventory:menu',
    -- 仪表盘
    'dashboard:view',
    -- 库存管理 (盘点相关)
    'inventory:query:view',
    'inventory:stocktake:view', 'inventory:stocktake:create', 'inventory:stocktake:edit', 'inventory:stocktake:operate',
    -- 报表 (库存报表)
    'report:inventory:view'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ========================================
-- 完成：权限初始化脚本
-- ========================================
-- 权限总数: 57 (7菜单 + 50操作)
-- 角色: 9个预置角色
-- 角色权限关联: 已全部配置