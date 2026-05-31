-- ============================================
-- 盘点管理模块 - 数据库表结构
-- 数据库: easy_erp
-- 创建时间: 2026-05-29
-- ============================================

USE easy_erp;

-- ============================================
-- 1. 盘点单主表
-- ============================================
CREATE TABLE IF NOT EXISTS wms_stocktake_order (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(20) NOT NULL COMMENT '盘点单号 ST+年月日+4位序号',
    warehouse_id    BIGINT NOT NULL COMMENT '仓库ID',
    warehouse_code  VARCHAR(20) COMMENT '仓库编码',
    warehouse_name  VARCHAR(100) COMMENT '仓库名称',
    stocktake_type  TINYINT NOT NULL COMMENT '盘点类型: 1全盘 2抽盘 3循环盘',
    blind_mode      TINYINT NOT NULL DEFAULT 0 COMMENT '盲盘模式: 0明盘 1盲盘',
    scope_type      VARCHAR(20) COMMENT '筛选方式: all/zone/category/abc/sku/random',
    scope_config    TEXT COMMENT '筛选条件JSON',
    status          TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待盘点 1盘点中 2待审核 3已完成 4已取消',
    total_items     INT NOT NULL DEFAULT 0 COMMENT '总SKU数',
    counted_items   INT NOT NULL DEFAULT 0 COMMENT '已盘点SKU数',
    diff_items      INT NOT NULL DEFAULT 0 COMMENT '差异SKU数',
    accuracy_rate   DECIMAL(5,2) COMMENT '准确率(%)',
    plan_date       DATE COMMENT '计划盘点日期',
    start_time      DATETIME COMMENT '实际开始时间',
    finish_time     DATETIME COMMENT '完成时间',
    approve_user_id BIGINT COMMENT '审批人ID',
    approve_user_name VARCHAR(50) COMMENT '审批人姓名',
    approve_time    DATETIME COMMENT '审批时间',
    remark          VARCHAR(500) COMMENT '备注',
    create_user_id  BIGINT COMMENT '创建人ID',
    create_user_name VARCHAR(50) COMMENT '创建人姓名',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status),
    INDEX idx_plan_date (plan_date),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单主表';

-- ============================================
-- 2. 盘点任务分配表
-- ============================================
CREATE TABLE IF NOT EXISTS wms_stocktake_assign (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    stocktake_id    BIGINT NOT NULL COMMENT '盘点单ID',
    user_id         BIGINT NOT NULL COMMENT '盘点人ID',
    user_name       VARCHAR(50) COMMENT '盘点人姓名',
    zone_id         BIGINT COMMENT '分配库区ID（按库区分配时）',
    zone_code       VARCHAR(20) COMMENT '库区编码',
    sku_count       INT NOT NULL DEFAULT 0 COMMENT '分配SKU数量',
    completed_count INT NOT NULL DEFAULT 0 COMMENT '已完成数量',
    status          TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0未开始 1进行中 2已完成',
    assign_time     DATETIME COMMENT '分配时间',
    complete_time   DATETIME COMMENT '完成时间',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_stocktake_id (stocktake_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点任务分配表';

-- ============================================
-- 3. 盘点明细表
-- ============================================
CREATE TABLE IF NOT EXISTS wms_stocktake_item (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT NOT NULL COMMENT '盘点单ID',
    order_no        VARCHAR(20) COMMENT '盘点单号',
    product_id      BIGINT COMMENT '商品ID',
    sku_code        VARCHAR(50) NOT NULL COMMENT 'SKU编码',
    product_name    VARCHAR(200) COMMENT '商品名称',
    barcode         VARCHAR(50) COMMENT '条码',
    location_id     BIGINT COMMENT '库位ID',
    location_code   VARCHAR(30) COMMENT '库位编码',
    batch_no        VARCHAR(100) COMMENT '批次号',
    system_qty      INT NOT NULL COMMENT '系统数量',
    counted_qty     INT COMMENT '盘点数量',
    diff_qty        INT COMMENT '差异数量',
    diff_reason     VARCHAR(20) COMMENT '差异原因: profit/loss/wrong/missed/other',
    diff_remark     VARCHAR(500) COMMENT '差异说明',
    status          TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待盘点 1已盘点 2已确认',
    round_no        TINYINT NOT NULL DEFAULT 1 COMMENT '盘点轮次: 1初盘 2复盘',
    count_user_id   BIGINT COMMENT '盘点人ID',
    count_user_name VARCHAR(50) COMMENT '盘点人姓名',
    count_time      DATETIME COMMENT '盘点时间',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_sku_code (sku_code),
    INDEX idx_location_code (location_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点明细表';

-- ============================================
-- 4. 盘点差异报告表（可选，用于记录差异处理结果）
-- ============================================
CREATE TABLE IF NOT EXISTS wms_stocktake_diff (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    stocktake_id    BIGINT NOT NULL COMMENT '盘点单ID',
    item_id         BIGINT NOT NULL COMMENT '盘点明细ID',
    sku_code        VARCHAR(50) NOT NULL COMMENT 'SKU编码',
    product_name    VARCHAR(200) COMMENT '商品名称',
    location_code   VARCHAR(30) COMMENT '库位编码',
    system_qty      INT NOT NULL COMMENT '系统数量',
    counted_qty     INT NOT NULL COMMENT '实盘数量',
    diff_qty        INT NOT NULL COMMENT '差异数量',
    diff_reason     VARCHAR(20) COMMENT '差异原因',
    diff_remark     VARCHAR(500) COMMENT '差异说明',
    process_type    TINYINT COMMENT '处理方式: 1生成调整单 2标记待处理 3驳回',
    process_status  TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态: 0待处理 1已处理',
    adjust_order_id BIGINT COMMENT '库存调整单ID',
    adjust_order_no VARCHAR(20) COMMENT '库存调整单号',
    process_user_id BIGINT COMMENT '处理人ID',
    process_user_name VARCHAR(50) COMMENT '处理人姓名',
    process_time    DATETIME COMMENT '处理时间',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_stocktake_id (stocktake_id),
    INDEX idx_item_id (item_id),
    INDEX idx_process_status (process_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点差异报告表';

SELECT '盘点管理模块表结构创建完成!' AS result;
