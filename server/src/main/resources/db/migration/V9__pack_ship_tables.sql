-- 打包和发货相关表
-- 创建日期: 2026-05-31
-- 说明: 包含打包记录表、发货记录表

-- 1. 打包记录表
CREATE TABLE IF NOT EXISTS wms_pack_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 关联信息
    outbound_order_id BIGINT NOT NULL COMMENT '出库单ID',
    outbound_order_no VARCHAR(50) COMMENT '出库单号',

    -- 包裹信息
    package_no VARCHAR(50) COMMENT '包裹号 PK20260531001',
    box_type VARCHAR(50) COMMENT '包装箱型（小箱/中箱/大箱/超大箱）',
    box_type_code VARCHAR(20) COMMENT '箱型编码',
    weight DECIMAL(10,2) COMMENT '包裹重量(kg)',
    volume DECIMAL(10,2) COMMENT '包裹体积(m³)',

    -- 商品统计
    total_qty INT DEFAULT 0 COMMENT '包裹内商品总数量',
    total_sku INT DEFAULT 0 COMMENT '包裹内SKU种类数',

    -- 物流信息
    logistics_company VARCHAR(100) COMMENT '物流公司',
    tracking_no VARCHAR(100) COMMENT '物流单号',

    -- 打包人
    pack_user_id BIGINT COMMENT '打包人ID',
    pack_user_name VARCHAR(100) COMMENT '打包人姓名',

    -- 状态
    status TINYINT DEFAULT 0 COMMENT '状态: 0待打包 1打包中 2已打包 3已发货 9已取消',

    -- 时间
    claim_time DATETIME COMMENT '领取时间',
    pack_time DATETIME COMMENT '打包完成时间',
    ship_time DATETIME COMMENT '发货时间',

    -- 备注
    remark VARCHAR(500) COMMENT '备注',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    KEY idx_outbound_order (outbound_order_id),
    KEY idx_package_no (package_no),
    KEY idx_pack_user (pack_user_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打包记录表';

-- 2. 打包明细表（包裹内的商品明细）
CREATE TABLE IF NOT EXISTS wms_pack_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 关联打包记录
    pack_record_id BIGINT NOT NULL COMMENT '打包记录ID',
    package_no VARCHAR(50) COMMENT '包裹号',

    -- 商品信息
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(50) COMMENT 'SKU编码',
    product_name VARCHAR(200) COMMENT '商品名称',
    barcode VARCHAR(50) COMMENT '商品条码',

    -- 数量
    qty INT DEFAULT 0 COMMENT '打包数量',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    KEY idx_pack_record (pack_record_id),
    KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打包明细表';

-- 3. 发货记录表
CREATE TABLE IF NOT EXISTS wms_ship_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 关联信息
    outbound_order_id BIGINT NOT NULL COMMENT '出库单ID',
    outbound_order_no VARCHAR(50) COMMENT '出库单号',
    package_no VARCHAR(50) COMMENT '包裹号',

    -- 物流信息
    logistics_company VARCHAR(100) COMMENT '物流公司',
    logistics_company_code VARCHAR(50) COMMENT '物流公司编码',
    tracking_no VARCHAR(100) COMMENT '物流单号',

    -- 发货人
    ship_user_id BIGINT COMMENT '发货人ID',
    ship_user_name VARCHAR(100) COMMENT '发货人姓名',

    -- 状态
    status TINYINT DEFAULT 0 COMMENT '状态: 0待发货 1已发货 9已取消',

    -- 发货时间
    ship_time DATETIME COMMENT '发货时间',

    -- 备注
    remark VARCHAR(500) COMMENT '备注',

    -- 通知状态
    erp_notified TINYINT DEFAULT 0 COMMENT '是否已通知ERP: 0未通知 1已通知',
    erp_notify_time DATETIME COMMENT 'ERP通知时间',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    KEY idx_outbound_order (outbound_order_id),
    KEY idx_package_no (package_no),
    KEY idx_tracking_no (tracking_no),
    KEY idx_ship_user (ship_user_id),
    KEY idx_status (status),
    KEY idx_ship_time (ship_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发货记录表';

-- 4. 包装箱型配置表（推荐包装用）
CREATE TABLE IF NOT EXISTS wms_box_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    code VARCHAR(20) NOT NULL COMMENT '箱型编码',
    name VARCHAR(50) NOT NULL COMMENT '箱型名称',
    length DECIMAL(10,2) COMMENT '长度(cm)',
    width DECIMAL(10,2) COMMENT '宽度(cm)',
    height DECIMAL(10,2) COMMENT '高度(cm)',
    volume DECIMAL(10,2) COMMENT '体积(m³)',
    max_weight DECIMAL(10,2) COMMENT '最大承重(kg)',

    status TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    sort_order INT DEFAULT 0 COMMENT '排序',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包装箱型配置表';

-- 插入默认箱型数据
INSERT INTO wms_box_type (code, name, length, width, height, volume, max_weight, sort_order) VALUES
('S', '小箱', 30, 20, 15, 0.009, 5, 1),
('M', '中箱', 40, 30, 25, 0.030, 15, 2),
('L', '大箱', 50, 40, 35, 0.070, 30, 3),
('XL', '超大箱', 60, 50, 45, 0.135, 50, 4);
