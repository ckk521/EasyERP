-- ============================================
-- 基础数据模块 - 数据库表结构
-- 数据库: easy_erp
-- 创建时间: 2026-04-19
-- ============================================

USE easy_erp;

-- ============================================
-- 1. 库区表
-- ============================================
CREATE TABLE IF NOT EXISTS base_zone (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(20) NOT NULL COMMENT '库区编码',
    name            VARCHAR(100) NOT NULL COMMENT '库区名称',
    warehouse_id    BIGINT NOT NULL COMMENT '所属仓库ID',
    warehouse_code  VARCHAR(20) COMMENT '仓库编码',
    type            TINYINT NOT NULL DEFAULT 3 COMMENT '库区类型：1-收货区 2-质检区 3-存储区 4-拣货区 5-打包区 6-发货区 7-退货区 8-残次品区',
    temp_require    TINYINT NOT NULL DEFAULT 1 COMMENT '温度要求：1-常温 2-冷藏 3-冷冻',
    location_count  INT NOT NULL DEFAULT 0 COMMENT '库位数量',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    create_user     BIGINT COMMENT '创建人ID',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user     BIGINT COMMENT '更新人ID',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_type (type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库区表';

-- ============================================
-- 2. 库位表
-- ============================================
CREATE TABLE IF NOT EXISTS base_location (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(30) NOT NULL COMMENT '库位编码',
    zone_id         BIGINT NOT NULL COMMENT '所属库区ID',
    zone_code       VARCHAR(20) COMMENT '库区编码',
    warehouse_id    BIGINT NOT NULL COMMENT '所属仓库ID',
    warehouse_code  VARCHAR(20) COMMENT '仓库编码',
    row_num         INT NOT NULL COMMENT '排号',
    col_num         INT NOT NULL COMMENT '列号',
    layer_num       INT NOT NULL COMMENT '层号',
    type            TINYINT NOT NULL DEFAULT 1 COMMENT '库位类型：1-标准位 2-大件位 3-冷藏位 4-高值位 5-危险品位',
    max_length      DECIMAL(10,2) COMMENT '最大长度(cm)',
    max_width       DECIMAL(10,2) COMMENT '最大宽度(cm)',
    max_height      DECIMAL(10,2) COMMENT '最大高度(cm)',
    max_weight      DECIMAL(10,2) COMMENT '最大承重(kg)',
    allowed_sku     VARCHAR(500) COMMENT '允许存放的SKU(逗号分隔)',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '1-空闲 2-占用 3-锁定 4-禁用',
    current_qty     INT NOT NULL DEFAULT 0 COMMENT '当前数量',
    last_check_time DATETIME COMMENT '最后盘点时间',
    create_user     BIGINT COMMENT '创建人ID',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user     BIGINT COMMENT '更新人ID',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    INDEX idx_zone_id (zone_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位表';

-- ============================================
-- 3. 商品分类表
-- ============================================
CREATE TABLE IF NOT EXISTS base_category (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(50) NOT NULL COMMENT '分类编码',
    name            VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id       BIGINT COMMENT '父分类ID',
    level           TINYINT NOT NULL COMMENT '层级：1-一级 2-二级 3-三级',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ============================================
-- 4. 商品表
-- ============================================
CREATE TABLE IF NOT EXISTS base_product (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku_code        VARCHAR(50) NOT NULL COMMENT 'SKU编码',
    barcode         VARCHAR(50) COMMENT '条码',
    name_cn         VARCHAR(200) NOT NULL COMMENT '中文名',
    name_en         VARCHAR(200) COMMENT '英文名',
    category_id     BIGINT COMMENT '分类ID',
    category_name   VARCHAR(200) COMMENT '分类名称',
    brand           VARCHAR(100) COMMENT '品牌',
    supplier_id     BIGINT COMMENT '供应商ID',
    supplier_name   VARCHAR(100) COMMENT '供应商名称',
    weight          DECIMAL(10,3) COMMENT '重量(kg)',
    length          DECIMAL(10,2) COMMENT '长度(cm)',
    width           DECIMAL(10,2) COMMENT '宽度(cm)',
    height          DECIMAL(10,2) COMMENT '高度(cm)',
    volume          DECIMAL(10,2) COMMENT '体积(cm³)',
    main_image      VARCHAR(500) COMMENT '主图URL',
    images          TEXT COMMENT '附图URL(JSON数组)',
    description     TEXT COMMENT '商品描述',
    -- 仓储属性
    storage_cond    TINYINT DEFAULT 1 COMMENT '存储条件：1-常温 2-冷藏 3-冷冻',
    humidity_min    DECIMAL(5,2) COMMENT '湿度下限(%)',
    humidity_max    DECIMAL(5,2) COMMENT '湿度上限(%)',
    shelf_life      INT COMMENT '保质期(天)',
    expiry_warning  INT DEFAULT 30 COMMENT '临期预警天数',
    is_dangerous    TINYINT DEFAULT 0 COMMENT '是否危险品：0-否 1-是',
    is_fragile      TINYINT DEFAULT 0 COMMENT '是否易碎：0-否 1-是',
    is_high_value   TINYINT DEFAULT 0 COMMENT '是否高价值：0-否 1-是',
    high_value_threshold DECIMAL(10,2) COMMENT '高价值阈值',
    security_level  TINYINT DEFAULT 1 COMMENT '安全级别：1-普通 2-高值 3-严格',
    need_expiry_mgmt TINYINT DEFAULT 0 COMMENT '需要效期管理：0-否 1-是',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_user     BIGINT COMMENT '创建人ID',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user     BIGINT COMMENT '更新人ID',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sku_code (sku_code),
    INDEX idx_barcode (barcode),
    INDEX idx_category_id (category_id),
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ============================================
-- 5. 供应商表
-- ============================================
CREATE TABLE IF NOT EXISTS base_supplier (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(50) NOT NULL COMMENT '供应商编码',
    name            VARCHAR(200) NOT NULL COMMENT '供应商名称',
    contact         VARCHAR(50) COMMENT '联系人',
    phone           VARCHAR(20) COMMENT '联系电话',
    email           VARCHAR(100) COMMENT '邮箱',
    address         VARCHAR(500) COMMENT '地址',
    bank_name       VARCHAR(100) COMMENT '开户银行',
    bank_account    VARCHAR(50) COMMENT '银行账号',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    create_user     BIGINT COMMENT '创建人ID',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user     BIGINT COMMENT '更新人ID',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    INDEX idx_name (name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- ============================================
-- 6. 客户表
-- ============================================
CREATE TABLE IF NOT EXISTS base_customer (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(50) NOT NULL COMMENT '客户编码',
    name            VARCHAR(200) NOT NULL COMMENT '客户名称',
    type            TINYINT NOT NULL DEFAULT 1 COMMENT '客户类型：1-个人 2-企业',
    contact         VARCHAR(50) COMMENT '联系人',
    phone           VARCHAR(20) COMMENT '联系电话',
    email           VARCHAR(100) COMMENT '邮箱',
    address         VARCHAR(500) COMMENT '地址',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    create_user     BIGINT COMMENT '创建人ID',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user     BIGINT COMMENT '更新人ID',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    INDEX idx_name (name),
    INDEX idx_type (type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- ============================================
-- 初始化商品分类数据
-- ============================================
INSERT INTO base_category (code, name, parent_id, level, sort_order) VALUES
-- 一级分类
('CAT001', '电子数码', NULL, 1, 1),
('CAT002', '服装服饰', NULL, 1, 2),
('CAT003', '鞋类箱包', NULL, 1, 3),
('CAT004', '美妆个护', NULL, 1, 4),
('CAT005', '家居生活', NULL, 1, 5),
('CAT006', '母婴用品', NULL, 1, 6),
('CAT007', '运动户外', NULL, 1, 7),
('CAT008', '食品保健', NULL, 1, 8),
('CAT009', '办公文具', NULL, 1, 9),
('CAT010', '其他', NULL, 1, 10);

-- 二级分类 - 电子数码
INSERT INTO base_category (code, name, parent_id, level, sort_order) VALUES
('CAT001001', '手机配件', 1, 2, 1),
('CAT001002', '电脑配件', 1, 2, 2),
('CAT001003', '数码相机', 1, 2, 3),
('CAT001004', '智能设备', 1, 2, 4);

-- 三级分类 - 手机配件
INSERT INTO base_category (code, name, parent_id, level, sort_order) VALUES
('CAT001001001', '手机壳', 11, 3, 1),
('CAT001001002', '手机膜', 11, 3, 2),
('CAT001001003', '充电器', 11, 3, 3),
('CAT001001004', '数据线', 11, 3, 4);

-- ============================================
-- 初始化供应商数据
-- ============================================
INSERT INTO base_supplier (code, name, contact, phone, email, address) VALUES
('SUP001', '深圳电子科技有限公司', '张经理', '13800138001', 'zhang@sz-electronics.com', '深圳市南山区科技园'),
('SUP002', '广州服装批发中心', '李经理', '13800138002', 'li@gz-clothing.com', '广州市白云区服装城'),
('SUP003', '义乌小商品城', '王经理', '13800138003', 'wang@yw-market.com', '义乌市国际商贸城');

-- ============================================
-- 初始化客户数据
-- ============================================
INSERT INTO base_customer (code, name, type, contact, phone, email, address) VALUES
('CUS001', '越南河内贸易公司', 2, '阮文强', '0901234567', 'nguyen@hanoi-trade.vn', '河内市还剑郡'),
('CUS002', '胡志明电商中心', 2, '陈氏梅', '0902345678', 'tran@hcm-ecom.vn', '胡志明市第一郡'),
('CUS003', '雅加达进口商', 2, 'Ahmad', '08123456789', 'ahmad@jakarta-import.id', '雅加达中区');

SELECT '基础数据模块表结构创建完成!' AS result;
