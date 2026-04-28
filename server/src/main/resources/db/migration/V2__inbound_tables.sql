-- 入库管理模块数据库表
-- 创建日期: 2026-04-21
-- 说明: 包含入库单、入库明细、不合格品记录、上架记录、库存、库存事务表

-- 1. 入库单主表
CREATE TABLE IF NOT EXISTS wms_inbound_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL COMMENT '入库单号 IN202604210001',
    delivery_batch_no VARCHAR(50) COMMENT '送货批次号 SZ-0421-AM',
    order_type TINYINT NOT NULL COMMENT '入库类型: 1采购 2退货 3调拨 4赠品 5其他',
    po_no VARCHAR(50) COMMENT '采购订单号(采购入库)',

    -- 供应商信息
    supplier_id BIGINT COMMENT '供应商ID',
    supplier_code VARCHAR(50) COMMENT '供应商编码',
    supplier_name VARCHAR(100) COMMENT '供应商名称',

    -- 仓库信息
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    warehouse_code VARCHAR(50) COMMENT '仓库编码',
    warehouse_name VARCHAR(100) COMMENT '仓库名称',

    -- 日期信息
    expected_date DATE COMMENT '预计到货日期',
    actual_arrival_date DATE COMMENT '实际到货日期',

    -- 状态与进度
    status TINYINT DEFAULT 0 COMMENT '状态: 0待收货 1收货中 2验收中 3待上架 4已完成 9已取消',
    progress_receive INT DEFAULT 0 COMMENT '收货进度百分比',
    progress_inspect INT DEFAULT 0 COMMENT '验收进度百分比',
    progress_putaway INT DEFAULT 0 COMMENT '上架进度百分比',

    -- 数量统计
    total_expected_qty INT DEFAULT 0 COMMENT '总预期数量',
    total_received_qty INT DEFAULT 0 COMMENT '总收货数量',
    total_qualified_qty INT DEFAULT 0 COMMENT '总合格数量',
    total_rejected_qty INT DEFAULT 0 COMMENT '总不合格数量',
    total_putaway_qty INT DEFAULT 0 COMMENT '总上架数量',
    total_return_qty INT DEFAULT 0 COMMENT '总退货数量',

    -- 其他
    remark VARCHAR(500) COMMENT '备注',
    create_user BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_user BIGINT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    complete_time DATETIME COMMENT '完成时间',

    UNIQUE KEY uk_order_no (order_no),
    KEY idx_supplier (supplier_id),
    KEY idx_delivery_batch (delivery_batch_no),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单主表';

-- 2. 入库单明细表
CREATE TABLE IF NOT EXISTS wms_inbound_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '入库单ID',
    order_no VARCHAR(50) COMMENT '入库单号',

    -- 商品信息
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(50) NOT NULL COMMENT 'SKU编码',
    product_name VARCHAR(200) COMMENT '商品名称',
    barcode VARCHAR(50) COMMENT '商品条码',

    -- 数量
    expected_qty INT DEFAULT 0 COMMENT '预期数量',
    received_qty INT DEFAULT 0 COMMENT '收货数量',
    qualified_qty INT DEFAULT 0 COMMENT '合格数量',
    rejected_qty INT DEFAULT 0 COMMENT '不合格数量',
    putaway_qty INT DEFAULT 0 COMMENT '上架数量',
    return_qty INT DEFAULT 0 COMMENT '退货数量',

    -- 批次效期
    batch_no VARCHAR(50) COMMENT '批次号',
    production_date DATE COMMENT '生产日期',
    expiry_date DATE COMMENT '过期日期',

    -- 状态
    status TINYINT DEFAULT 0 COMMENT '状态: 0待收货 1已收货 2已验收 3已上架 4部分退货 5全部退货',

    -- 差异原因
    diff_reason VARCHAR(200) COMMENT '收货差异原因',

    -- 时间
    receive_time DATETIME COMMENT '收货时间',
    receive_user BIGINT COMMENT '收货人',
    inspect_time DATETIME COMMENT '验收时间',
    inspect_user BIGINT COMMENT '验收人',
    putaway_time DATETIME COMMENT '上架时间',
    putaway_user BIGINT COMMENT '上架人',

    KEY idx_order (order_id),
    KEY idx_product (product_id),
    KEY idx_batch (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单明细表';

-- 3. 不合格品记录表
CREATE TABLE IF NOT EXISTS wms_reject_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 数据来源(关联入库)
    inbound_order_id BIGINT NOT NULL COMMENT '来源入库单ID',
    inbound_order_no VARCHAR(50) COMMENT '来源入库单号',
    inbound_item_id BIGINT NOT NULL COMMENT '来源入库明细ID',
    delivery_batch_no VARCHAR(50) COMMENT '送货批次号',

    -- 商品信息
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(50) COMMENT 'SKU编码',
    product_name VARCHAR(200) COMMENT '商品名称',

    -- 供应商信息(从入库单继承)
    supplier_id BIGINT COMMENT '供应商ID',
    supplier_code VARCHAR(50) COMMENT '供应商编码',
    supplier_name VARCHAR(100) COMMENT '供应商名称',

    -- 不合格信息
    reject_qty INT NOT NULL COMMENT '不合格数量',
    reject_type TINYINT NOT NULL COMMENT '不合格类型: 1包装破损 2商品损坏 3错货 4规格不符 5效期问题 6其他',
    reject_reason VARCHAR(500) COMMENT '不合格原因描述',
    reject_images VARCHAR(1000) COMMENT '不合格照片URL(逗号分隔)',

    -- 发现环节
    discover_stage TINYINT COMMENT '发现环节: 1收货 2验收',

    -- 处理信息
    handle_status TINYINT DEFAULT 0 COMMENT '处理状态: 0待处理 1已处理',
    handle_type TINYINT COMMENT '处理方式: 1退货供应商 2降价销售 3报损 4内部使用 5销毁',
    handle_qty INT COMMENT '处理数量',
    handle_remark VARCHAR(500) COMMENT '处理备注',
    handle_time DATETIME COMMENT '处理时间',
    handle_user BIGINT COMMENT '处理人',

    -- 退货信息(处理方式为退货时填写)
    return_order_no VARCHAR(50) COMMENT '退货单号',
    return_time DATETIME COMMENT '退货时间',
    return_remark VARCHAR(500) COMMENT '退货备注',

    -- 时间
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    KEY idx_inbound_order (inbound_order_id),
    KEY idx_inbound_item (inbound_item_id),
    KEY idx_supplier (supplier_id),
    KEY idx_handle_status (handle_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不合格品记录表';

-- 4. 上架记录表
CREATE TABLE IF NOT EXISTS wms_putaway_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 数据来源(关联入库)
    inbound_order_id BIGINT NOT NULL COMMENT '来源入库单ID',
    inbound_order_no VARCHAR(50) COMMENT '来源入库单号',
    inbound_item_id BIGINT NOT NULL COMMENT '来源入库明细ID',

    -- 商品信息
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(50) COMMENT 'SKU编码',
    product_name VARCHAR(200) COMMENT '商品名称',

    -- 批次效期
    batch_no VARCHAR(50) COMMENT '批次号',
    production_date DATE COMMENT '生产日期',
    expiry_date DATE COMMENT '过期日期',

    -- 上架信息
    warehouse_id BIGINT COMMENT '仓库ID',
    location_id BIGINT NOT NULL COMMENT '库位ID',
    location_code VARCHAR(50) COMMENT '库位编码',
    putaway_qty INT NOT NULL COMMENT '上架数量',

    -- 推荐信息
    is_recommended TINYINT DEFAULT 0 COMMENT '是否使用推荐库位: 0否 1是',
    recommended_location_id BIGINT COMMENT '推荐的库位ID',

    -- 时间
    putaway_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
    putaway_user BIGINT COMMENT '上架人',

    KEY idx_inbound_order (inbound_order_id),
    KEY idx_inbound_item (inbound_item_id),
    KEY idx_location (location_id),
    KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上架记录表';

-- 5. 库存表
CREATE TABLE IF NOT EXISTS wms_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 位置信息
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    warehouse_code VARCHAR(50) COMMENT '仓库编码',
    location_id BIGINT NOT NULL COMMENT '库位ID',
    location_code VARCHAR(50) COMMENT '库位编码',

    -- 商品信息
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(50) NOT NULL COMMENT 'SKU编码',
    product_name VARCHAR(200) COMMENT '商品名称',

    -- 批次效期
    batch_no VARCHAR(50) COMMENT '批次号',
    production_date DATE COMMENT '生产日期',
    expiry_date DATE COMMENT '过期日期',

    -- 数量
    qty INT DEFAULT 0 COMMENT '总数量',
    available_qty INT DEFAULT 0 COMMENT '可用数量',
    locked_qty INT DEFAULT 0 COMMENT '锁定数量',

    -- 数据来源
    inbound_order_id BIGINT COMMENT '来源入库单ID',
    inbound_order_no VARCHAR(50) COMMENT '来源入库单号',
    inbound_time DATETIME COMMENT '入库时间',

    -- 效期状态
    expiry_status TINYINT DEFAULT 0 COMMENT '效期状态: 0正常 1预警 2临期 3已过期',

    -- 时间
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_location_product_batch (location_id, product_id, batch_no),
    KEY idx_warehouse (warehouse_id),
    KEY idx_product (product_id),
    KEY idx_batch (batch_no),
    KEY idx_expiry (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- 6. 库存事务表
CREATE TABLE IF NOT EXISTS wms_inventory_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_no VARCHAR(50) NOT NULL COMMENT '事务单号',

    -- 事务类型
    transaction_type TINYINT NOT NULL COMMENT '类型: 1入库 2出库 3调拨入 4调拨出 5盘点调整 6退货',

    -- 位置
    warehouse_id BIGINT COMMENT '仓库ID',
    location_id BIGINT COMMENT '库位ID',
    location_code VARCHAR(50) COMMENT '库位编码',

    -- 商品
    product_id BIGINT COMMENT '商品ID',
    sku_code VARCHAR(50) COMMENT 'SKU编码',
    batch_no VARCHAR(50) COMMENT '批次号',

    -- 数量变化
    qty_change INT NOT NULL COMMENT '变动数量(正为增,负为减)',
    qty_before INT COMMENT '变动前数量',
    qty_after INT COMMENT '变动后数量',

    -- 关联单据(数据源)
    ref_order_type VARCHAR(20) COMMENT '关联单据类型: INBOUND/OUTBOUND/TRANSFER/STOCKTAKE/RETURN',
    ref_order_id BIGINT COMMENT '关联单据ID',
    ref_order_no VARCHAR(50) COMMENT '关联单据号',

    -- 备注
    remark VARCHAR(200) COMMENT '备注',

    -- 操作人
    create_user BIGINT COMMENT '操作人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    KEY idx_transaction_no (transaction_no),
    KEY idx_location (location_id),
    KEY idx_product (product_id),
    KEY idx_ref_order (ref_order_type, ref_order_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存事务表';
