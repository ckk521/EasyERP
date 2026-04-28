-- ============================================
-- 货架配置表 - 支持批量生成库位
-- ============================================
CREATE TABLE IF NOT EXISTS base_shelf_config (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(30) NOT NULL COMMENT '货架编码',
    name            VARCHAR(100) COMMENT '货架名称',
    zone_id         BIGINT NOT NULL COMMENT '所属库区ID',
    zone_code       VARCHAR(20) COMMENT '库区编码',
    warehouse_id    BIGINT NOT NULL COMMENT '所属仓库ID',
    warehouse_code  VARCHAR(20) COMMENT '仓库编码',
    row_num         INT NOT NULL COMMENT '排号',
    shelf_type      TINYINT NOT NULL DEFAULT 1 COMMENT '货架类型：1-立体货架 2-贯通式货架 3-自动化仓库',
    start_layer     INT NOT NULL DEFAULT 1 COMMENT '起始层',
    end_layer       INT NOT NULL DEFAULT 1 COMMENT '结束层',
    column_count    INT NOT NULL DEFAULT 1 COMMENT '每层位置数',
    storage_type    TINYINT COMMENT '存储类型：1-常温 2-冷藏 3-冷冻 4-恒温',
    max_length      DECIMAL(10,2) COMMENT '单个库位最大长度(cm)',
    max_width       DECIMAL(10,2) COMMENT '单个库位最大宽度(cm)',
    max_height      DECIMAL(10,2) COMMENT '单个库位最大高度(cm)',
    max_weight      DECIMAL(10,2) COMMENT '单个库位最大承重(kg)',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    generated       TINYINT NOT NULL DEFAULT 0 COMMENT '是否已生成库位：0-否 1-是',
    location_count  INT NOT NULL DEFAULT 0 COMMENT '生成的库位数量',
    create_user     BIGINT COMMENT '创建人ID',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user     BIGINT COMMENT '更新人ID',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    INDEX idx_zone_id (zone_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_row_num (row_num)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货架配置表';
