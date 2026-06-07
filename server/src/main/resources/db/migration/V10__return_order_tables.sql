-- 客户退货功能数据表
-- 创建时间: 2026-06-06

-- 先给入库单表添加来源退货单字段
ALTER TABLE wms_inbound_order
ADD COLUMN source_return_id BIGINT COMMENT '来源退货单ID(客户退货入库)',
ADD COLUMN source_return_no VARCHAR(32) COMMENT '来源退货单号';

-- 退货单主表
CREATE TABLE IF NOT EXISTS wms_return_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    return_no VARCHAR(32) NOT NULL COMMENT '退货单号 RT202606060001',

    -- 关联原出库单
    original_outbound_id BIGINT COMMENT '原出库单ID',
    original_outbound_no VARCHAR(32) COMMENT '原出库单号',

    -- 客户信息（从出库单带出）
    customer_id BIGINT COMMENT '客户ID',
    customer_name VARCHAR(64) COMMENT '客户名称',

    -- 退货原因
    return_reason TINYINT COMMENT '退货原因: 1质量问题 2发错货 3数量不符 4不满意 5无理由 6其他',
    return_reason_text VARCHAR(200) COMMENT '退货原因详细说明',

    -- 状态
    status TINYINT DEFAULT 0 COMMENT '状态: 0待收货 1已收货 2已完成 9已取消',

    -- 数量汇总
    total_expected_qty INT DEFAULT 0 COMMENT '预计退货数量',
    total_received_qty INT DEFAULT 0 COMMENT '实际收货数量',

    -- 关联入库单
    inbound_order_id BIGINT COMMENT '生成的入库单ID',
    inbound_order_no VARCHAR(32) COMMENT '生成的入库单号',

    -- 仓库
    warehouse_id BIGINT COMMENT '仓库ID',
    warehouse_code VARCHAR(32) COMMENT '仓库编码',
    warehouse_name VARCHAR(64) COMMENT '仓库名称',

    -- 取消信息
    cancel_reason VARCHAR(200) COMMENT '取消原因',

    -- 备注
    remark VARCHAR(500) COMMENT '备注',

    -- 审计字段
    create_user_id BIGINT COMMENT '创建人ID',
    create_user_name VARCHAR(32) COMMENT '创建人姓名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    receive_time DATETIME COMMENT '收货时间',
    receive_user_id BIGINT COMMENT '收货人ID',
    receive_user_name VARCHAR(32) COMMENT '收货人姓名',
    complete_time DATETIME COMMENT '完成时间',

    UNIQUE KEY uk_return_no (return_no),
    KEY idx_status (status),
    KEY idx_outbound (original_outbound_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退货单主表';

-- 退货单明细表
CREATE TABLE IF NOT EXISTS wms_return_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    return_order_id BIGINT NOT NULL COMMENT '退货单ID',
    return_order_no VARCHAR(32) NOT NULL COMMENT '退货单号',

    -- 商品信息
    product_id BIGINT COMMENT '商品ID',
    sku_code VARCHAR(32) COMMENT 'SKU编码',
    product_name VARCHAR(128) COMMENT '商品名称',
    barcode VARCHAR(64) COMMENT '条码',

    -- 数量
    original_qty INT DEFAULT 0 COMMENT '原出库数量',
    expected_qty INT DEFAULT 0 COMMENT '预计退货数量',
    received_qty INT DEFAULT 0 COMMENT '实际收货数量',

    -- 备注
    remark VARCHAR(200) COMMENT '备注',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    KEY idx_return_order (return_order_id),
    KEY idx_sku (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退货单明细表';
