-- 盘点单表扩展字段（循环盘配置）
-- V6__stocktake_cycle_fields.sql

ALTER TABLE wms_stocktake_order
ADD COLUMN cycle_type VARCHAR(20) NULL COMMENT '周期类型: daily每日/weekly每周/monthly每月' AFTER remark,
ADD COLUMN cycle_day INT NULL COMMENT '盘点日: 周几(1-7) 或 每月第几天(1-31)' AFTER cycle_type,
ADD COLUMN cycle_strategy VARCHAR(30) NULL COMMENT '轮转策略: zone_rotation库区轮转/sku_rotation按SKU轮转/fixed固定范围' AFTER cycle_day,
ADD COLUMN cycle_config TEXT NULL COMMENT '轮转配置JSON: 如库区ID列表、SKU比例等' AFTER cycle_strategy,
ADD COLUMN cycle_index INT DEFAULT 0 COMMENT '当前轮转索引' AFTER cycle_config,
ADD COLUMN last_cycle_date DATE NULL COMMENT '上次执行日期' AFTER cycle_index,
ADD COLUMN next_cycle_date DATE NULL COMMENT '下次执行日期' AFTER last_cycle_date,
ADD COLUMN parent_strategy_id BIGINT NULL COMMENT '父策略ID（循环盘生成的盘点单关联到策略）' AFTER next_cycle_date;

-- 创建索引
CREATE INDEX idx_stocktake_cycle_type ON wms_stocktake_order(cycle_type);
CREATE INDEX idx_stocktake_next_cycle ON wms_stocktake_order(next_cycle_date);
CREATE INDEX idx_stocktake_parent ON wms_stocktake_order(parent_strategy_id);