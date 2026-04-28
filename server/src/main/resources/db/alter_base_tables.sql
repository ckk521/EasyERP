-- 添加供应商表缺失字段
ALTER TABLE base_supplier ADD COLUMN tax_no VARCHAR(50) COMMENT '税号';
ALTER TABLE base_supplier ADD COLUMN remark VARCHAR(500) COMMENT '备注';

-- 添加客户表缺失字段
ALTER TABLE base_customer ADD COLUMN credit_limit DECIMAL(12,2) COMMENT '信用额度';
ALTER TABLE base_customer ADD COLUMN level TINYINT DEFAULT 1 COMMENT '等级：1-普通 2-银卡 3-金卡 4-VIP';
ALTER TABLE base_customer ADD COLUMN remark VARCHAR(500) COMMENT '备注';
