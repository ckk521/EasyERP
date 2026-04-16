-- ============================================
-- 迁移脚本: 添加操作日志新字段
-- 日期: 2026-04-11
-- ============================================

USE wms_system;

-- 添加 request_method 字段
ALTER TABLE sys_operation_log
ADD COLUMN request_method VARCHAR(20) COMMENT '请求方法' AFTER ip;

-- 添加 request_url 字段
ALTER TABLE sys_operation_log
ADD COLUMN request_url VARCHAR(500) COMMENT '请求URL' AFTER request_method;

-- 添加 request_params 字段
ALTER TABLE sys_operation_log
ADD COLUMN request_params TEXT COMMENT '请求参数' AFTER request_url;

-- 添加 real_name 字段
ALTER TABLE sys_operation_log
ADD COLUMN real_name VARCHAR(100) COMMENT '用户真实姓名' AFTER username;

-- 添加 error_message 字段
ALTER TABLE sys_operation_log
ADD COLUMN error_message TEXT COMMENT '错误信息' AFTER result;

-- 添加 response_time 字段
ALTER TABLE sys_operation_log
ADD COLUMN response_time BIGINT COMMENT '响应时间(毫秒)' AFTER error_message;
