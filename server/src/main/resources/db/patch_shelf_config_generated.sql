-- 修复 base_shelf_config 表字段名问题
-- 如果字段名是 is_generated，改为 generated

-- 检查并修改字段名
ALTER TABLE base_shelf_config CHANGE COLUMN is_generated generated TINYINT NOT NULL DEFAULT 0 COMMENT '是否已生成库位：0-否 1-是';