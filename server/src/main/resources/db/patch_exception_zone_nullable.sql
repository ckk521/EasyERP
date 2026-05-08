-- 修改 zone_id 允许 NULL
ALTER TABLE wms_exception_order MODIFY COLUMN zone_id BIGINT NULL COMMENT '隔离库区ID';
