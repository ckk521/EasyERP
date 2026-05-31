-- 修复 base_location.code 字段长度不足的问题
-- 库位编码格式: 库区编码-RXX-LXX-CXX，库区编码可能很长
ALTER TABLE base_location MODIFY COLUMN code VARCHAR(50) NOT NULL COMMENT '库位编码';