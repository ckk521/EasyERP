-- 添加上架操作人姓名字段
ALTER TABLE wms_putaway_record ADD COLUMN putaway_user_name VARCHAR(50) COMMENT '上架操作人姓名' AFTER putaway_user;
