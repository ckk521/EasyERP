-- 出库管理模块数据库表
-- 创建日期: 2026-05-31
-- 说明: 包含出库单、出库明细、波次、库存分配、拣货记录表

-- 1. 出库单主表
CREATE TABLE IF NOT EXISTS wms_outbound_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL COMMENT '出库单号 OB20260531001',
    order_type TINYINT NOT NULL COMMENT '出库类型: 1销售 2调拨 3退货 4报废 5样品',
    source_type TINYINT COMMENT '来源类型: 1ERP推送 2手工创建 3调拨申请',
    so_no VARCHAR(50) COMMENT '销售订单号(销售出库时填写)',

    -- 客户信息
    customer_id BIGINT COMMENT '客户ID',
    customer_code VARCHAR(50) COMMENT '客户编码',
    customer_name VARCHAR(100) COMMENT '客户名称',

    -- 仓库信息
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    warehouse_code VARCHAR(50) COMMENT '仓库编码',
    warehouse_name VARCHAR(100) COMMENT '仓库名称',

    -- 优先级与物流
    priority TINYINT DEFAULT 3 COMMENT '优先级: 1紧急 2高 3中 4低',
    logistics_company VARCHAR(100) COMMENT '物流公司',

    -- 收货信息
    receiver_name VARCHAR(100) COMMENT '收货人姓名',
    receiver_phone VARCHAR(50) COMMENT '收货人电话',
    receiver_address VARCHAR(500) COMMENT '收货地址',

    -- 状态
    status TINYINT DEFAULT 0 COMMENT '状态: 0待分配 1已分配 2拣货中 3待打包 4待发货 5已发货 9已取消',

    -- 数量统计
    total_qty INT DEFAULT 0 COMMENT '总出库数量',
    total_picked_qty INT DEFAULT 0 COMMENT '总拣货数量',
    total_packed_qty INT DEFAULT 0 COMMENT '总打包数量',
    total_shipped_qty INT DEFAULT 0 COMMENT '总发货数量',

    -- 波次信息
    wave_id BIGINT COMMENT '波次ID',
    wave_no VARCHAR(50) COMMENT '波次号',

    -- 拣货人
    pick_user_id BIGINT COMMENT '拣货人ID',
    pick_user_name VARCHAR(100) COMMENT '拣货人姓名',
    pick_start_time DATETIME COMMENT '拣货开始时间',
    pick_complete_time DATETIME COMMENT '拣货完成时间',

    -- 打包人
    pack_user_id BIGINT COMMENT '打包人ID',
    pack_user_name VARCHAR(100) COMMENT '打包人姓名',
    pack_time DATETIME COMMENT '打包时间',

    -- 发货人
    ship_user_id BIGINT COMMENT '发货人ID',
    ship_user_name VARCHAR(100) COMMENT '发货人姓名',
    ship_time DATETIME COMMENT '发货时间',

    -- 物流信息
    tracking_no VARCHAR(100) COMMENT '物流单号',
    shipped_time DATETIME COMMENT '发货时间',

    -- 其他
    remark VARCHAR(500) COMMENT '备注',
    cancel_reason VARCHAR(500) COMMENT '取消原因',

    -- 时间
    create_user BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_user BIGINT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    complete_time DATETIME COMMENT '完成时间',

    UNIQUE KEY uk_order_no (order_no),
    KEY idx_customer (customer_id),
    KEY idx_warehouse (warehouse_id),
    KEY idx_status (status),
    KEY idx_wave (wave_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单主表';

-- 2. 出库单明细表
CREATE TABLE IF NOT EXISTS wms_outbound_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '出库单ID',
    order_no VARCHAR(50) COMMENT '出库单号',

    -- 商品信息
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(50) NOT NULL COMMENT 'SKU编码',
    product_name VARCHAR(200) COMMENT '商品名称',
    barcode VARCHAR(50) COMMENT '商品条码',

    -- 数量
    qty INT DEFAULT 0 COMMENT '应出数量',
    picked_qty INT DEFAULT 0 COMMENT '已拣数量',
    packed_qty INT DEFAULT 0 COMMENT '已打包数量',
    shipped_qty INT DEFAULT 0 COMMENT '已发货数量',

    -- 库位信息（分配后填写）
    location_id BIGINT COMMENT '库位ID',
    location_code VARCHAR(50) COMMENT '库位编码',

    -- 批次号
    batch_no VARCHAR(50) COMMENT '批次号',

    -- 状态
    status TINYINT DEFAULT 0 COMMENT '状态: 0待拣货 1拣货中 2已拣货 3已打包 4已发货 9已取消',

    -- 差异原因
    diff_reason VARCHAR(200) COMMENT '拣货差异原因',
    diff_qty INT COMMENT '差异数量',

    -- 异常标记
    is_exception TINYINT DEFAULT 0 COMMENT '是否异常: 0正常 1异常',
    exception_type TINYINT COMMENT '异常类型: 1缺货 2破损 3错货 4其他',
    exception_qty INT COMMENT '异常数量',
    exception_remark VARCHAR(500) COMMENT '异常备注',

    -- 时间
    pick_time DATETIME COMMENT '拣货时间',
    pack_time DATETIME COMMENT '打包时间',
    ship_time DATETIME COMMENT '发货时间',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    KEY idx_order (order_id),
    KEY idx_product (product_id),
    KEY idx_location (location_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单明细表';

-- 3. 波次表
CREATE TABLE IF NOT EXISTS wms_wave (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wave_no VARCHAR(50) NOT NULL COMMENT '波次号 W20260531001',

    -- 策略信息
    strategy_type TINYINT NOT NULL COMMENT '策略类型: 1按时间 2按物流 3按区域 4按商品 5按客户',
    strategy_name VARCHAR(100) COMMENT '策略名称',

    -- 仓库信息
    warehouse_id BIGINT COMMENT '仓库ID',
    warehouse_code VARCHAR(50) COMMENT '仓库编码',
    warehouse_name VARCHAR(100) COMMENT '仓库名称',

    -- 状态
    status TINYINT DEFAULT 0 COMMENT '状态: 0待释放 1拣货中 2已完成 9已取消',

    -- 统计信息
    total_orders INT DEFAULT 0 COMMENT '总订单数',
    total_sku INT DEFAULT 0 COMMENT '总SKU数',
    total_qty INT DEFAULT 0 COMMENT '总件数',

    -- 进度
    picked_qty INT DEFAULT 0 COMMENT '已拣件数',
    packed_qty INT DEFAULT 0 COMMENT '已打包件数',
    shipped_qty INT DEFAULT 0 COMMENT '已发货件数',

    -- 分配信息
    assigned_user_id BIGINT COMMENT '分配人员ID',
    assigned_user_name VARCHAR(100) COMMENT '分配人员姓名',

    -- 时间
    start_time DATETIME COMMENT '开始时间',
    complete_time DATETIME COMMENT '完成时间',

    -- 其他
    remark VARCHAR(500) COMMENT '备注',

    create_user BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_user BIGINT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_wave_no (wave_no),
    KEY idx_warehouse (warehouse_id),
    KEY idx_status (status),
    KEY idx_strategy (strategy_type),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='波次表';

-- 4. 库存分配表（库存锁定记录）
CREATE TABLE IF NOT EXISTS wms_inventory_allocation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 关联出库单
    outbound_order_id BIGINT NOT NULL COMMENT '出库单ID',
    outbound_order_no VARCHAR(50) COMMENT '出库单号',
    outbound_item_id BIGINT COMMENT '出库明细ID',

    -- 商品信息
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(50) COMMENT 'SKU编码',

    -- 库位信息
    warehouse_id BIGINT COMMENT '仓库ID',
    location_id BIGINT COMMENT '库位ID',
    location_code VARCHAR(50) COMMENT '库位编码',
    batch_no VARCHAR(50) COMMENT '批次号',

    -- 分配数量
    allocated_qty INT NOT NULL COMMENT '分配数量（锁定数量）',
    picked_qty INT DEFAULT 0 COMMENT '已拣数量',
    shipped_qty INT DEFAULT 0 COMMENT '已发货数量',

    -- 状态
    status TINYINT DEFAULT 0 COMMENT '状态: 0已锁定 1已拣货 2已发货 3已释放',

    -- 波次信息
    wave_id BIGINT COMMENT '波次ID',
    wave_no VARCHAR(50) COMMENT '波次号',

    -- 时间
    allocate_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
    pick_time DATETIME COMMENT '拣货时间',
    ship_time DATETIME COMMENT '发货时间',
    release_time DATETIME COMMENT '释放时间',

    KEY idx_outbound_order (outbound_order_id),
    KEY idx_product (product_id),
    KEY idx_location (location_id),
    KEY idx_wave (wave_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存分配表';

-- 5. 拣货记录表
CREATE TABLE IF NOT EXISTS wms_pick_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 关联信息
    outbound_order_id BIGINT NOT NULL COMMENT '出库单ID',
    outbound_order_no VARCHAR(50) COMMENT '出库单号',
    outbound_item_id BIGINT COMMENT '出库明细ID',
    wave_id BIGINT COMMENT '波次ID',
    wave_no VARCHAR(50) COMMENT '波次号',

    -- 商品信息
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(50) COMMENT 'SKU编码',
    product_name VARCHAR(200) COMMENT '商品名称',
    barcode VARCHAR(50) COMMENT '商品条码',

    -- 库位信息
    warehouse_id BIGINT COMMENT '仓库ID',
    location_id BIGINT NOT NULL COMMENT '库位ID',
    location_code VARCHAR(50) COMMENT '库位编码',

    -- 批次号
    batch_no VARCHAR(50) COMMENT '批次号',

    -- 数量
    plan_qty INT NOT NULL COMMENT '计划拣货数量',
    actual_qty INT DEFAULT 0 COMMENT '实际拣货数量',
    diff_qty INT DEFAULT 0 COMMENT '差异数量',

    -- 扫码确认
    location_scanned TINYINT DEFAULT 0 COMMENT '库位扫码确认: 0未扫码 1已扫码',
    product_scanned TINYINT DEFAULT 0 COMMENT '商品扫码确认: 0未扫码 1已扫码',

    -- 状态
    status TINYINT DEFAULT 0 COMMENT '状态: 0待拣货 1拣货中 2已完成 3异常 9已取消',

    -- 异常信息
    is_exception TINYINT DEFAULT 0 COMMENT '是否异常: 0正常 1异常',
    exception_type TINYINT COMMENT '异常类型: 1缺货 2破损 3错货 4其他',
    exception_qty INT COMMENT '异常数量',
    exception_remark VARCHAR(500) COMMENT '异常备注',

    -- 差异原因
    diff_reason VARCHAR(500) COMMENT '差异原因',

    -- 拣货人
    pick_user_id BIGINT COMMENT '拣货人ID',
    pick_user_name VARCHAR(100) COMMENT '拣货人姓名',

    -- 时间
    claim_time DATETIME COMMENT '领取时间',
    start_time DATETIME COMMENT '开始拣货时间',
    complete_time DATETIME COMMENT '完成时间',

    -- 排序序号（拣货路径顺序）
    sort_order INT DEFAULT 0 COMMENT '拣货顺序序号',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    KEY idx_outbound_order (outbound_order_id),
    KEY idx_wave (wave_id),
    KEY idx_product (product_id),
    KEY idx_location (location_id),
    KEY idx_pick_user (pick_user_id),
    KEY idx_status (status),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拣货记录表';

-- 6. 拣货任务表（按波次汇总）
CREATE TABLE IF NOT EXISTS wms_pick_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 波次信息
    wave_id BIGINT NOT NULL COMMENT '波次ID',
    wave_no VARCHAR(50) COMMENT '波次号',

    -- 任务信息
    task_no VARCHAR(50) NOT NULL COMMENT '任务号 PT20260531001',

    -- 拣货人
    pick_user_id BIGINT COMMENT '拣货人ID',
    pick_user_name VARCHAR(100) COMMENT '拣货人姓名',

    -- 状态
    status TINYINT DEFAULT 0 COMMENT '状态: 0待领取 1进行中 2已完成 9已取消',

    -- 统计
    total_items INT DEFAULT 0 COMMENT '总拣货项数',
    completed_items INT DEFAULT 0 COMMENT '已完成项数',
    total_qty INT DEFAULT 0 COMMENT '总拣货数量',
    picked_qty INT DEFAULT 0 COMMENT '已拣数量',

    -- 时间
    claim_time DATETIME COMMENT '领取时间',
    start_time DATETIME COMMENT '开始时间',
    complete_time DATETIME COMMENT '完成时间',

    -- 备注
    remark VARCHAR(500) COMMENT '备注',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_task_no (task_no),
    KEY idx_wave (wave_id),
    KEY idx_pick_user (pick_user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拣货任务表';