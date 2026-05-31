import requests

BASE_URL = "http://localhost:8081/api/v1"

# 登录
login_resp = requests.post(f"{BASE_URL}/auth/login", json={"username": "admin", "password": "admin123"})
token = login_resp.json()["data"]["token"]
headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json; charset=utf-8"}

# 获取供应商和商品
suppliers = requests.get(f"{BASE_URL}/base/suppliers?page=1&limit=10", headers=headers).json()["data"]["list"]
products = requests.get(f"{BASE_URL}/base/products?page=1&limit=30", headers=headers).json()["data"]["list"]

print(f"供应商: {len(suppliers)}个, 商品: {len(products)}个")

# 创建关联：每个供应商关联部分商品
relations = [
    # 深圳电子 - 电子产品
    {"supplierId": 1, "productId": 4, "supplierSkuCode": "SZ-IPHONE15-FILM", "purchasePrice": 15.00, "minOrderQty": 100, "leadTime": 3},
    {"supplierId": 1, "productId": 5, "supplierSkuCode": "SZ-CHARGER-20W", "purchasePrice": 25.00, "minOrderQty": 50, "leadTime": 5},
    {"supplierId": 1, "productId": 6, "supplierSkuCode": "SZ-TYPEC-1M", "purchasePrice": 8.00, "minOrderQty": 200, "leadTime": 3},

    # 广州服装 - 服装类
    {"supplierId": 2, "productId": 7, "supplierSkuCode": "GZ-TSHIRT-WL", "purchasePrice": 35.00, "minOrderQty": 50, "leadTime": 7},
    {"supplierId": 2, "productId": 8, "supplierSkuCode": "GZ-JEANS-BM", "purchasePrice": 85.00, "minOrderQty": 30, "leadTime": 10},

    # 义乌小商品 - 日用品
    {"supplierId": 3, "productId": 21, "supplierSkuCode": "YW-YOGA-10MM", "purchasePrice": 45.00, "minOrderQty": 20, "leadTime": 5},
    {"supplierId": 3, "productId": 22, "supplierSkuCode": "YW-BATMINTON", "purchasePrice": 65.00, "minOrderQty": 10, "leadTime": 3},
    {"supplierId": 3, "productId": 23, "supplierSkuCode": "YW-PAPER-A4", "purchasePrice": 22.00, "minOrderQty": 100, "leadTime": 2},
    {"supplierId": 3, "productId": 24, "supplierSkuCode": "YW-PEN-BK12", "purchasePrice": 8.50, "minOrderQty": 500, "leadTime": 3},

    # 湖南电子 - 数码配件
    {"supplierId": 5, "productId": 4, "supplierSkuCode": "HN-FILM-IP15", "purchasePrice": 12.00, "minOrderQty": 150, "leadTime": 4},
    {"supplierId": 5, "productId": 5, "supplierSkuCode": "HN-CHARGER-20W", "purchasePrice": 22.00, "minOrderQty": 80, "leadTime": 5},
]

success = 0
for r in relations:
    resp = requests.post(f"{BASE_URL}/base/supplier-products", headers=headers, json=r)
    if resp.json().get("success"):
        success += 1
        # 找到供应商和商品名称
        supplier = next((s for s in suppliers if s["id"] == r["supplierId"]), None)
        product = next((p for p in products if p["id"] == r["productId"]), None)
        print(f"OK: {supplier['name'] if supplier else r['supplierId']} -> {product['skuCode'] if product else r['productId']}")
    else:
        print(f"FAIL: {resp.json().get('message')}")

print(f"\n成功创建 {success}/{len(relations)} 条关联")
