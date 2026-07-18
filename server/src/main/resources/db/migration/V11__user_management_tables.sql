-- 用户管理功能数据表
-- 创建时间: 2026-06-07

-- 1. 修改用户表，新增字段
ALTER TABLE sys_user
ADD COLUMN employee_no VARCHAR(20) COMMENT '员工工号',
ADD COLUMN department INT COMMENT '部门（字典）',
ADD COLUMN position INT COMMENT '岗位（字典）',
ADD COLUMN hire_date DATE COMMENT '入职日期',
ADD COLUMN work_status INT DEFAULT 1 COMMENT '工作状态: 1在职 2休假 3离职',
ADD COLUMN skill_level INT COMMENT '技能等级: 1初级 2中级 3高级 4专家',
ADD COLUMN shift_type INT COMMENT '班次: 1早班 2中班 3晚班 4常白班',
ADD COLUMN login_count INT DEFAULT 0 COMMENT '登录次数';

-- 创建员工工号唯一索引
CREATE UNIQUE INDEX idx_employee_no ON sys_user(employee_no);

-- 2. 创建用户角色关联表（多角色）
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 3. 创建用户仓库关联表（多仓库）
CREATE TABLE IF NOT EXISTS sys_user_warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_warehouse (user_id, warehouse_id),
    INDEX idx_user_id (user_id),
    INDEX idx_warehouse_id (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户仓库关联表';

-- 4. 创建系统字典表
CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_type VARCHAR(50) NOT NULL COMMENT '字典类型',
    dict_code INT NOT NULL COMMENT '字典编码',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status INT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    is_system INT DEFAULT 0 COMMENT '是否预置: 0否 1是',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_type_code (dict_type, dict_code),
    INDEX idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典表';

-- 5. 初始化字典数据

-- 部门
INSERT INTO sys_dict (dict_type, dict_code, dict_value, sort_order, is_system) VALUES
('department', 1, '入库组', 1, 1),
('department', 2, '出库组', 2, 1),
('department', 3, '库存组', 3, 1),
('department', 4, '管理组', 4, 1);

-- 岗位
INSERT INTO sys_dict (dict_type, dict_code, dict_value, sort_order, is_system) VALUES
('position', 1, '拣货员', 1, 1),
('position', 2, '打包员', 2, 1),
('position', 3, '发货员', 3, 1),
('position', 4, '收货员', 4, 1),
('position', 5, '质检员', 5, 1),
('position', 6, '上架员', 6, 1),
('position', 7, '盘点员', 7, 1),
('position', 8, '仓库主管', 8, 1),
('position', 9, '系统管理员', 9, 1);

-- 工作状态
INSERT INTO sys_dict (dict_type, dict_code, dict_value, sort_order, is_system) VALUES
('work_status', 1, '在职', 1, 1),
('work_status', 2, '休假', 2, 1),
('work_status', 3, '离职', 3, 1);

-- 技能等级
INSERT INTO sys_dict (dict_type, dict_code, dict_value, sort_order, is_system) VALUES
('skill_level', 1, '初级', 1, 1),
('skill_level', 2, '中级', 2, 1),
('skill_level', 3, '高级', 3, 1),
('skill_level', 4, '专家', 4, 1);

-- 班次类型
INSERT INTO sys_dict (dict_type, dict_code, dict_value, sort_order, is_system) VALUES
('shift_type', 1, '早班', 1, 1),
('shift_type', 2, '中班', 2, 1),
('shift_type', 3, '晚班', 3, 1),
('shift_type', 4, '常白班', 4, 1);

-- 6. 初始化角色数据（如果不存在）
INSERT INTO sys_role (code, name, type, description, status, is_system)
SELECT 'PICKER', '拣货员', 1, '负责拣货作业', 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'PICKER');

INSERT INTO sys_role (code, name, type, description, status, is_system)
SELECT 'PACKER', '打包员', 1, '负责打包作业', 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'PACKER');

INSERT INTO sys_role (code, name, type, description, status, is_system)
SELECT 'SHIPPER', '发货员', 1, '负责发货作业', 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'SHIPPER');

INSERT INTO sys_role (code, name, type, description, status, is_system)
SELECT 'RECEIVER', '收货员', 1, '负责收货作业', 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'RECEIVER');

INSERT INTO sys_role (code, name, type, description, status, is_system)
SELECT 'INSPECTOR', '质检员', 1, '负责验收作业', 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'INSPECTOR');

INSERT INTO sys_role (code, name, type, description, status, is_system)
SELECT 'PUTAWAY_CLERK', '上架员', 1, '负责上架作业', 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'PUTAWAY_CLERK');

INSERT INTO sys_role (code, name, type, description, status, is_system)
SELECT 'STOCKTAKER', '盘点员', 1, '负责盘点作业', 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'STOCKTAKER');

INSERT INTO sys_role (code, name, type, description, status, is_system)
SELECT 'WAREHOUSE_MANAGER', '仓库主管', 1, '管理审批', 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'WAREHOUSE_MANAGER');

INSERT INTO sys_role (code, name, type, description, status, is_system)
SELECT 'SYSTEM_ADMIN', '系统管理员', 1, '系统配置', 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'SYSTEM_ADMIN');

-- 7. 初始化角色权限（角色权限是固定的）
-- 拣货员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'PICKER' AND p.code IN ('outbound:pick:view', 'outbound:pick:operate')
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 打包员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'PACKER' AND p.code IN ('outbound:pack:view', 'outbound:pack:operate')
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 发货员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'SHIPPER' AND p.code IN ('outbound:ship:view', 'outbound:ship:operate')
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 收货员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'RECEIVER' AND p.code IN ('inbound:receive:view', 'inbound:receive:operate')
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 质检员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'INSPECTOR' AND p.code IN ('inbound:inspect:view', 'inbound:inspect:operate')
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 上架员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'PUTAWAY_CLERK' AND p.code IN ('inbound:putaway:view', 'inbound:putaway:operate')
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 盘点员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'STOCKTAKER' AND p.code IN ('inventory:stocktake:view', 'inventory:stocktake:operate')
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 仓库主管权限（查看 + 审批）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'WAREHOUSE_MANAGER'
AND p.code IN ('dashboard:view', 'order:view', 'order:approve', 'inbound:order:view', 'outbound:order:view', 'return:order:view')
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 系统管理员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'SYSTEM_ADMIN'
AND p.code LIKE 'system:%'
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 8. 迁移现有用户的 role_id 到关联表
INSERT INTO sys_user_role (user_id, role_id)
SELECT id, role_id FROM sys_user WHERE role_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = sys_user.id);
