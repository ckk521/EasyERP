-- ============================================
-- WMS系统管理模块 - 数据库初始化脚本
-- 数据库: wms_system
-- ============================================

CREATE DATABASE IF NOT EXISTS wms_system DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
USE wms_system;

-- ============================================
-- 1. 仓库表（基础数据）
-- ============================================
CREATE TABLE IF NOT EXISTS sys_warehouse (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(20) NOT NULL UNIQUE COMMENT '仓库编码',
    name            VARCHAR(100) NOT NULL COMMENT '仓库名称',
    type            TINYINT NOT NULL COMMENT '类型：1-自营仓 2-第三方仓 3-海外仓',
    country         VARCHAR(50) COMMENT '国家',
    province        VARCHAR(100) COMMENT '省份/城市',
    address         VARCHAR(200) COMMENT '详细地址',
    manager         VARCHAR(50) COMMENT '负责人',
    phone           VARCHAR(20) COMMENT '联系电话',
    area            DECIMAL(10,2) COMMENT '总面积(㎡)',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

-- ============================================
-- 2. 角色表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    name            VARCHAR(100) NOT NULL COMMENT '角色名称',
    type            TINYINT NOT NULL DEFAULT 2 COMMENT '1-预置 2-自定义',
    description     VARCHAR(500) COMMENT '角色描述',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    is_system       TINYINT NOT NULL DEFAULT 0 COMMENT '0-否 1-是',
    create_user     BIGINT,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user     BIGINT,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_type (type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ============================================
-- 3. 权限表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_permission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    name            VARCHAR(100) NOT NULL COMMENT '权限名称',
    module          VARCHAR(50) NOT NULL COMMENT '所属模块',
    description     VARCHAR(500) COMMENT '权限描述',
    parent_id       BIGINT COMMENT '父权限ID',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_module (module),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ============================================
-- 4. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password        VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    name            VARCHAR(100) NOT NULL COMMENT '姓名',
    phone           VARCHAR(20) COMMENT '手机号',
    email           VARCHAR(100) COMMENT '邮箱',
    role_id         BIGINT NOT NULL COMMENT '角色ID',
    warehouse_id    BIGINT COMMENT '所属仓库ID',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip   VARCHAR(50) COMMENT '最后登录IP',
    create_user     BIGINT COMMENT '创建人ID',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user     BIGINT COMMENT '更新人ID',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role_id (role_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 5. 角色权限关联表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id         BIGINT NOT NULL COMMENT '角色ID',
    permission_id   BIGINT NOT NULL COMMENT '权限ID',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================================
-- 6. 操作日志表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    time            DATETIME NOT NULL COMMENT '操作时间',
    user_id         BIGINT COMMENT '操作用户ID',
    username        VARCHAR(50) NOT NULL COMMENT '操作用户名',
    module          VARCHAR(50) COMMENT '操作模块',
    action          VARCHAR(50) COMMENT '操作类型',
    object          VARCHAR(200) COMMENT '操作对象',
    ip              VARCHAR(50) COMMENT 'IP地址',
    result          VARCHAR(20) COMMENT '操作结果',
    details         JSON COMMENT '详细数据',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_time (time),
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_module (module),
    INDEX idx_action (action),
    INDEX idx_result (result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================
-- 7. 登录日志表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_login_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    time            DATETIME NOT NULL COMMENT '登录时间',
    user_id         BIGINT COMMENT '用户ID',
    username        VARCHAR(50) NOT NULL COMMENT '用户名',
    action          VARCHAR(20) NOT NULL COMMENT '动作',
    ip              VARCHAR(50) COMMENT 'IP地址',
    device          VARCHAR(100) COMMENT '登录设备',
    browser         VARCHAR(100) COMMENT '浏览器',
    result          VARCHAR(20) NOT NULL COMMENT '结果',
    fail_reason     VARCHAR(200) COMMENT '失败原因',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_time (time),
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_result (result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ============================================
-- 8. 库存变动日志表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_inventory_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    time            DATETIME NOT NULL COMMENT '变动时间',
    user_id         BIGINT COMMENT '操作用户ID',
    username        VARCHAR(50) NOT NULL COMMENT '操作用户名',
    sku_code        VARCHAR(50) NOT NULL COMMENT 'SKU编码',
    sku_name        VARCHAR(200) COMMENT 'SKU名称',
    warehouse_id    BIGINT COMMENT '仓库ID',
    warehouse_code  VARCHAR(20) COMMENT '仓库编码',
    location_code   VARCHAR(20) COMMENT '库位编码',
    batch_no        VARCHAR(50) COMMENT '批次号',
    change_type     VARCHAR(20) NOT NULL COMMENT '变动类型',
    change_quantity INT NOT NULL COMMENT '变动数量',
    quantity_before INT NOT NULL COMMENT '变动前库存',
    quantity_after  INT NOT NULL COMMENT '变动后库存',
    reference_no    VARCHAR(50) COMMENT '关联单号',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_time (time),
    INDEX idx_user_id (user_id),
    INDEX idx_sku_code (sku_code),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_change_type (change_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动日志表';

-- ============================================
-- 9. 系统配置表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_config (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    category        VARCHAR(50) NOT NULL COMMENT '配置分类',
    code            VARCHAR(100) NOT NULL UNIQUE COMMENT '配置编码',
    name            VARCHAR(100) NOT NULL COMMENT '配置名称',
    value           TEXT COMMENT '配置值',
    type            VARCHAR(20) NOT NULL COMMENT '类型',
    default_value   TEXT COMMENT '默认值',
    options         JSON COMMENT '可选值',
    validation_rule VARCHAR(500) COMMENT '校验规则',
    description     VARCHAR(500) COMMENT '描述',
    is_system       TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统内置',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_code (code),
    INDEX idx_type (type),
    INDEX idx_is_system (is_system)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================
-- 10. 配置历史表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_config_history (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_id       BIGINT NOT NULL COMMENT '配置ID',
    old_value       TEXT COMMENT '修改前值',
    new_value       TEXT COMMENT '修改后值',
    operator_id     BIGINT NOT NULL COMMENT '操作人ID',
    operator_name   VARCHAR(100) NOT NULL COMMENT '操作人姓名',
    reason          VARCHAR(500) COMMENT '修改原因',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_config_id (config_id),
    INDEX idx_operator_id (operator_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置历史表';

-- ============================================
-- 添加外键约束
-- ============================================
ALTER TABLE sys_user ADD CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role(id);
ALTER TABLE sys_user ADD CONSTRAINT fk_user_warehouse FOREIGN KEY (warehouse_id) REFERENCES sys_warehouse(id);
ALTER TABLE sys_role_permission ADD CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role(id);
ALTER TABLE sys_role_permission ADD CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id);
ALTER TABLE sys_config_history ADD CONSTRAINT fk_ch_config FOREIGN KEY (config_id) REFERENCES sys_config(id);

-- ============================================
-- 初始化预设数据
-- ============================================

-- 预设仓库
INSERT INTO sys_warehouse (code, name, type, country, province, status) VALUES
('WH-VN-001', '越南胡志明仓', 3, '越南', '胡志明市', 1),
('WH-ID-001', '印尼雅加达仓', 3, '印尼', '雅加达', 1),
('WH-CN-001', '深圳总仓', 1, '中国', '深圳', 1);

-- 预设角色
INSERT INTO sys_role (code, name, type, description, status, is_system) VALUES
('SUPER_ADMIN', '超级管理员', 1, '拥有全部系统权限', 1, 1),
('WH_ADMIN', '仓库管理员', 1, '本仓库全部业务权限', 1, 1),
('INBOUND_OPERATOR', '入库员', 1, '入库相关功能权限', 1, 1),
('OUTBOUND_OPERATOR', '出库员', 1, '出库相关功能权限', 1, 1),
('INVENTORY_ADMIN', '库存管理员', 1, '库存查询、盘点、调拨权限', 1, 1),
('REPORT_VIEWER', '报表查看员', 1, '仅查看报表权限', 1, 1);

-- 预设管理员用户 (密码: admin123)
INSERT INTO sys_user (username, password, name, phone, email, role_id, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', '13800138000', 'admin@wms.com', 1, 1),
('wh_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '仓库管理员', '13800138001', 'wh_admin@wms.com', 2, 1);

-- 预设权限
INSERT INTO sys_permission (code, name, module, description) VALUES
-- 系统管理模块
('system:user:view', '用户查看', '系统管理', '查看用户列表和详情'),
('system:user:create', '用户创建', '系统管理', '创建新用户'),
('system:user:edit', '用户编辑', '系统管理', '编辑用户信息'),
('system:user:delete', '用户删除', '系统管理', '删除用户'),
('system:user:enable', '用户启用', '系统管理', '启用用户'),
('system:user:disable', '用户禁用', '系统管理', '禁用用户'),
('system:user:resetpwd', '重置密码', '系统管理', '重置用户密码'),
('system:role:view', '角色查看', '系统管理', '查看角色列表和详情'),
('system:role:create', '角色创建', '系统管理', '创建新角色'),
('system:role:edit', '角色编辑', '系统管理', '编辑角色信息'),
('system:role:delete', '角色删除', '系统管理', '删除角色'),
('system:config:view', '配置查看', '系统管理', '查看系统配置'),
('system:config:edit', '配置编辑', '系统管理', '编辑系统配置'),
('system:log:view', '日志查看', '系统管理', '查看操作日志'),
-- 入库管理模块
('inbound:view', '入库查看', '入库管理', '查看入库单'),
('inbound:create', '入库创建', '入库管理', '创建入库单'),
('inbound:receive', '收货作业', '入库管理', '执行收货操作'),
('inbound:accept', '验收作业', '入库管理', '执行验收操作'),
('inbound:putaway', '上架作业', '入库管理', '执行上架操作'),
-- 出库管理模块
('outbound:view', '出库查看', '出库管理', '查看出库单'),
('outbound:create', '出库创建', '出库管理', '创建出库单'),
('outbound:wave', '波次分配', '出库管理', '执行波次分配'),
('outbound:pick', '拣货作业', '出库管理', '执行拣货操作'),
('outbound:pack', '打包作业', '出库管理', '执行打包操作'),
('outbound:ship', '发货作业', '出库管理', '执行发货操作'),
-- 库存管理模块
('inventory:view', '库存查看', '库存管理', '查看库存'),
('inventory:stocktake', '库存盘点', '库存管理', '执行盘点'),
('inventory:transfer', '库存调拨', '库存管理', '执行调拨');

-- 角色权限关联（超级管理员拥有所有权限）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- 仓库管理员权限（仓库业务权限）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE module IN ('入库管理', '出库管理', '库存管理');

-- 入库员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE module = '入库管理';

-- 出库员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE module = '出库管理';

-- 库存管理员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 5, id FROM sys_permission WHERE module = '库存管理';

-- 报表查看员权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 6, id FROM sys_permission WHERE module = '系统管理' AND code LIKE '%view%';

-- 系统配置
INSERT INTO sys_config (category, code, name, value, type, default_value, description, is_system) VALUES
-- 基础参数
('基础参数', 'system.name', '系统名称', 'WMS仓库管理系统', 'string', 'WMS仓库管理系统', '系统显示名称', 1),
('基础参数', 'system.language', '默认语言', 'zh-CN', 'select', 'zh-CN', '系统默认语言', 1),
('基础参数', 'system.timezone', '时区', 'GMT+8', 'select', 'GMT+8', '系统时区', 1),
('基础参数', 'system.currency', '货币单位', 'CNY', 'select', 'CNY', '货币单位', 1),
-- 业务参数
('业务参数', 'business.safety_days', '默认安全库存天数', '30', 'number', '30', '安全库存预警天数', 1),
('业务参数', 'business.validity_warning_days', '效期预警天数', '30', 'number', '30', '商品效期提前预警天数', 1),
('业务参数', 'business.stagnation_days', '呆滞天数', '90', 'number', '90', '库存呆滞预警天数', 1),
('业务参数', 'business.pick_path_strategy', '拣货路径策略', 'S形路径', 'select', 'S形路径', '拣货路径优化策略', 1),
-- 编码规则
('编码规则', 'code.inbound_prefix', '入库单前缀', 'RK', 'string', 'RK', '入库单号前缀', 1),
('编码规则', 'code.outbound_prefix', '出库单前缀', 'CK', 'string', 'CK', '出库单号前缀', 1),
('编码规则', 'code.stocktake_prefix', '盘点单前缀', 'PD', 'string', 'PD', '盘点单号前缀', 1),
('编码规则', 'code.serial_length', '流水号位数', '4', 'number', '4', '单号流水号位数', 1),
-- 会话管理
('会话管理', 'session.timeout_minutes', '会话超时时间', '30', 'number', '30', '会话超时时间（分钟）', 1),
('会话管理', 'session.token_expire_hours', 'Access Token有效期', '1', 'number', '1', 'Access Token有效期（小时）', 1),
('会话管理', 'session.refresh_token_days', 'Refresh Token有效期', '7', 'number', '7', 'Refresh Token有效期（天）', 1),
-- 安全配置
('安全配置', 'security.min_password_length', '密码最小长度', '6', 'number', '6', '密码最小长度', 1),
('安全配置', 'security.login_fail_lock_count', '登录失败锁定次数', '5', 'number', '5', '连续登录失败锁定次数', 1),
('安全配置', 'security.log_retention_days', '日志保留天数', '180', 'number', '180', '操作日志保留天数', 1);
