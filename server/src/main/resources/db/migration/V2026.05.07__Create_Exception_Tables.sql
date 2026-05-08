-- 异常管理模块数据库表
-- 版本: V2026.05.07
-- 说明: 异常处理单主表和明细表

-- ============================================
-- 异常处理单主表
-- ============================================
CREATE TABLE IF NOT EXISTS wms_exception_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    order_no VARCHAR(32) NOT NULL COMMENT '异常处理单号，格式：EX+年月日+序号',
    inbound_order_id BIGINT COMMENT '关联入库单ID',
    inbound_order_no VARCHAR(32) COMMENT '入库单号',
    purchase_order_id BIGINT COMMENT '关联采购订单ID',
    purchase_order_no VARCHAR(32) COMMENT '采购订单号',
    supplier_id BIGINT COMMENT '供应商ID',
    supplier_code VARCHAR(50) COMMENT '供应商编码',
    supplier_name VARCHAR(100) COMMENT '供应商名称',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    warehouse_code VARCHAR(20) NOT NULL COMMENT '仓库编码',
    zone_id BIGINT NOT NULL COMMENT '隔离库区ID',
    zone_code VARCHAR(20) NOT NULL COMMENT '隔离库区编码',
    exception_type TINYINT NOT NULL COMMENT '异常类型：1-破损 2-短缺 3-质量不合格 4-错货 5-其他',
    total_qty INT NOT NULL COMMENT '异常总数量',
    exception_reason VARCHAR(500) COMMENT '异常原因说明',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理 1-处理中 2-已完成 3-已取消',
    handle_type TINYINT COMMENT '处理方式：1-退货 2-换货 3-报废 4-降价销售',
    handle_result VARCHAR(500) COMMENT '处理结果说明',
    handle_time DATETIME COMMENT '处理完成时间',
    handle_user_id BIGINT COMMENT '处理人ID',
    handle_user_name VARCHAR(50) COMMENT '处理人姓名',
    source_type TINYINT NOT NULL DEFAULT 1 COMMENT '来源类型：1-收货异常 2-验收异常',
    remark VARCHAR(500) COMMENT '备注',
    create_user_id BIGINT COMMENT '创建人ID',
    create_user_name VARCHAR(50) COMMENT '创建人姓名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_inbound_order (inbound_order_id),
    KEY idx_supplier (supplier_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常处理单主表';

-- ============================================
-- 异常处理明细表
-- ============================================
CREATE TABLE IF NOT EXISTS wms_exception_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    order_id BIGINT NOT NULL COMMENT '异常处理单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '异常处理单号',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(50) NOT NULL COMMENT 'SKU编码',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    barcode VARCHAR(50) COMMENT '条码',
    batch_no VARCHAR(50) COMMENT '批次号',
    exception_qty INT NOT NULL COMMENT '异常数量',
    exception_type TINYINT NOT NULL COMMENT '异常类型',
    exception_reason VARCHAR(500) COMMENT '异常原因',
    location_id BIGINT COMMENT '隔离库位ID',
    location_code VARCHAR(50) COMMENT '隔离库位编码',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理 1-已隔离 2-已处理',
    handle_type TINYINT COMMENT '处理方式',
    handle_qty INT COMMENT '处理数量',
    handle_result VARCHAR(500) COMMENT '处理结果',
    inbound_item_id BIGINT COMMENT '关联入库明细ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_order_id (order_id),
    KEY idx_product (product_id),
    KEY idx_status (status),
    KEY idx_inbound_item (inbound_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常处理明细表';
