-- 入库作业记录表
-- 创建日期: 2026-04-21
-- 说明: 记录式存储收货、验收、上架操作，支持追溯

-- 1. 收货记录表
CREATE TABLE IF NOT EXISTS wms_receive_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',

    -- 数据来源(关联入库单)
    inbound_order_id BIGINT NOT NULL COMMENT '来源入库单ID，关联 wms_inbound_order.id',
    inbound_order_no VARCHAR(50) COMMENT '来源入库单号',
    inbound_item_id BIGINT NOT NULL COMMENT '来源入库明细ID，关联 wms_inbound_order_item.id',

    -- 商品信息
    product_id BIGINT NOT NULL COMMENT '商品ID，关联 base_product.id',
    sku_code VARCHAR(50) COMMENT 'SKU编码',
    product_name VARCHAR(200) COMMENT '商品名称',

    -- 收货信息
    receive_qty INT NOT NULL COMMENT '本次收货数量',
    diff_qty INT DEFAULT 0 COMMENT '本次差异数量(与待收货数量的差异)',
    diff_reason VARCHAR(200) COMMENT '差异原因',

    -- 异常信息
    has_exception TINYINT DEFAULT 0 COMMENT '是否有异常: 0否 1是',
    exception_type VARCHAR(50) COMMENT '异常类型: 包装破损/商品损坏/错货/串货',
    exception_desc VARCHAR(500) COMMENT '异常描述',
    exception_images VARCHAR(1000) COMMENT '异常照片URL(逗号分隔)',

    -- 操作信息
    receive_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收货时间',
    receive_user BIGINT COMMENT '收货人ID',
    receive_user_name VARCHAR(50) COMMENT '收货人姓名',

    -- 索引
    KEY idx_inbound_order (inbound_order_id) COMMENT '入库单索引',
    KEY idx_inbound_item (inbound_item_id) COMMENT '入库明细索引',
    KEY idx_product (product_id) COMMENT '商品索引',
    KEY idx_receive_time (receive_time) COMMENT '收货时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货记录表 - 记录每次收货操作';

-- 2. 验收记录表
CREATE TABLE IF NOT EXISTS wms_inspect_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',

    -- 数据来源(关联入库单)
    inbound_order_id BIGINT NOT NULL COMMENT '来源入库单ID，关联 wms_inbound_order.id',
    inbound_order_no VARCHAR(50) COMMENT '来源入库单号',
    inbound_item_id BIGINT NOT NULL COMMENT '来源入库明细ID，关联 wms_inbound_order_item.id',

    -- 商品信息
    product_id BIGINT NOT NULL COMMENT '商品ID，关联 base_product.id',
    sku_code VARCHAR(50) COMMENT 'SKU编码',
    product_name VARCHAR(200) COMMENT '商品名称',

    -- 验收信息
    inspect_qty INT NOT NULL COMMENT '本次验收数量',
    qualified_qty INT DEFAULT 0 COMMENT '本次合格数量',
    rejected_qty INT DEFAULT 0 COMMENT '本次不合格数量',

    -- 批次效期
    batch_no VARCHAR(50) COMMENT '批次号(验收时生成)',
    production_date DATE COMMENT '生产日期',
    expiry_date DATE COMMENT '过期日期',

    -- 操作信息
    inspect_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '验收时间',
    inspect_user BIGINT COMMENT '验收人ID',
    inspect_user_name VARCHAR(50) COMMENT '验收人姓名',

    -- 索引
    KEY idx_inbound_order (inbound_order_id) COMMENT '入库单索引',
    KEY idx_inbound_item (inbound_item_id) COMMENT '入库明细索引',
    KEY idx_product (product_id) COMMENT '商品索引',
    KEY idx_batch (batch_no) COMMENT '批次号索引',
    KEY idx_inspect_time (inspect_time) COMMENT '验收时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验收记录表 - 记录每次验收操作';

-- 3. 上架记录表 (已存在，确认结构)
-- 如果已存在则跳过，这里作为参考
CREATE TABLE IF NOT EXISTS wms_putaway_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',

    -- 数据来源(关联入库)
    inbound_order_id BIGINT NOT NULL COMMENT '来源入库单ID，关联 wms_inbound_order.id',
    inbound_order_no VARCHAR(50) COMMENT '来源入库单号',
    inbound_item_id BIGINT NOT NULL COMMENT '来源入库明细ID，关联 wms_inbound_order_item.id',

    -- 商品信息
    product_id BIGINT NOT NULL COMMENT '商品ID，关联 base_product.id',
    sku_code VARCHAR(50) COMMENT 'SKU编码',
    product_name VARCHAR(200) COMMENT '商品名称',

    -- 批次效期
    batch_no VARCHAR(50) COMMENT '批次号',
    production_date DATE COMMENT '生产日期',
    expiry_date DATE COMMENT '过期日期',

    -- 上架信息
    warehouse_id BIGINT COMMENT '仓库ID，关联 sys_warehouse.id',
    location_id BIGINT NOT NULL COMMENT '库位ID，关联 sys_location.id',
    location_code VARCHAR(50) COMMENT '库位编码',
    putaway_qty INT NOT NULL COMMENT '本次上架数量',

    -- 推荐信息
    is_recommended TINYINT DEFAULT 0 COMMENT '是否使用推荐库位: 0否 1是',
    recommended_location_id BIGINT COMMENT '推荐的库位ID',

    -- 操作信息
    putaway_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
    putaway_user BIGINT COMMENT '上架人ID',
    putaway_user_name VARCHAR(50) COMMENT '上架人姓名',

    -- 索引
    KEY idx_inbound_order (inbound_order_id) COMMENT '入库单索引',
    KEY idx_inbound_item (inbound_item_id) COMMENT '入库明细索引',
    KEY idx_location (location_id) COMMENT '库位索引',
    KEY idx_product (product_id) COMMENT '商品索引',
    KEY idx_putaway_time (putaway_time) COMMENT '上架时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上架记录表 - 记录每次上架操作';

-- 4. 修改入库明细表，添加汇总字段（如果不存在）
-- 注意：这些字段用于快速查询，实际数据以记录表为准
ALTER TABLE wms_inbound_order_item
    ADD COLUMN IF NOT EXISTS total_receive_count INT DEFAULT 0 COMMENT '收货次数',
    ADD COLUMN IF NOT EXISTS total_inspect_count INT DEFAULT 0 COMMENT '验收次数',
    ADD COLUMN IF NOT EXISTS total_putaway_count INT DEFAULT 0 COMMENT '上架次数';
