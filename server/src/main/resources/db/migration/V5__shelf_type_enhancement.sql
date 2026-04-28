-- ============================================
-- V5: 支持三种货架类型的业务逻辑
-- ============================================

-- 1. 库区表添加默认货架类型
ALTER TABLE base_zone ADD COLUMN default_shelf_type TINYINT DEFAULT 1 COMMENT '默认货架类型：1-立体货架 2-贯通式货架 3-自动化仓库' AFTER temp_require;

-- 2. 库位表添加货架类型和相关字段
ALTER TABLE base_location ADD COLUMN shelf_type TINYINT DEFAULT 1 COMMENT '货架类型：1-立体货架 2-贯通式货架 3-自动化仓库' AFTER type;
ALTER TABLE base_location ADD COLUMN aisle_num INT COMMENT '通道号(贯通式货架用)' AFTER row_num;
ALTER TABLE base_location ADD COLUMN depth_num INT COMMENT '深度号(贯通式货架用，从外到里递增)' AFTER col_num;
ALTER TABLE base_location ADD COLUMN is_blocked TINYINT DEFAULT 0 COMMENT '是否被挡住(贯通式货架用)：0-否 1-是' AFTER status;

-- 3. 添加索引
ALTER TABLE base_location ADD INDEX idx_shelf_type (shelf_type);
ALTER TABLE base_location ADD INDEX idx_aisle_depth (aisle_num, depth_num);

-- 4. 更新货架配置表，添加通道数和深度数字段
ALTER TABLE base_shelf_config ADD COLUMN aisle_count INT DEFAULT 1 COMMENT '通道数(贯通式货架用)' AFTER column_count;
ALTER TABLE base_shelf_config ADD COLUMN depth_count INT DEFAULT 1 COMMENT '深度数(贯通式货架用，每个通道有几个深度位置)' AFTER aisle_count;
