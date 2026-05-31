import pymysql

conn = pymysql.connect(host='localhost', user='root', password='root', database='easy_erp', charset='utf8mb4')
cursor = conn.cursor()

# 创建供应商商品关系表
cursor.execute("""
CREATE TABLE IF NOT EXISTS base_supplier_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    supplier_code VARCHAR(50) COMMENT '供应商编码',
    supplier_name VARCHAR(100) COMMENT '供应商名称',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(50) COMMENT 'SKU编码',
    product_name VARCHAR(200) COMMENT '商品名称',
    supplier_sku_code VARCHAR(100) COMMENT '供应商商品编码',
    purchase_price DECIMAL(12,2) COMMENT '采购价',
    min_order_qty DECIMAL(12,2) COMMENT '最小起订量',
    lead_time DECIMAL(10,0) COMMENT '交货周期(天)',
    status INT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    create_user BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_user BIGINT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_supplier_product (supplier_id, product_id),
    KEY idx_supplier_id (supplier_id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商商品关系表'
""")
print("Created base_supplier_product table")

# 移除商品表的供应商字段
try:
    cursor.execute("ALTER TABLE base_product DROP COLUMN supplier_id")
    cursor.execute("ALTER TABLE base_product DROP COLUMN supplier_name")
    print("Removed supplier columns from base_product")
except Exception as e:
    print(f"Columns may not exist: {e}")

conn.commit()
cursor.close()
conn.close()
print("Done!")
