-- 盘点审核记录表
-- V7__stocktake_approve_record.sql

CREATE TABLE wms_stocktake_approve_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '盘点单ID',
    order_no VARCHAR(20) NOT NULL COMMENT '盘点单号',
    action VARCHAR(20) NOT NULL COMMENT '操作类型: approve通过/reject驳回',
    reason TEXT NULL COMMENT '驳回原因（驳回时填写）',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) NOT NULL COMMENT '操作人姓名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    INDEX idx_order_id (order_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点审核记录表';