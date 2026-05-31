import requests

BASE_URL = "http://localhost:8081/api/v1"

# 登录
login_resp = requests.post(f"{BASE_URL}/auth/login", json={"username": "admin", "password": "admin123"})
token = login_resp.json()["data"]["token"]
headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json; charset=utf-8"}

# 获取已关联的商品ID
existing = requests.get(f"{BASE_URL}/base/supplier-products?page=1&limit=100", headers=headers).json()["data"]["list"]
linked_product_ids = set(sp["productId"] for sp in existing)
print(f"已关联商品数: {len(linked_product_ids)}")

# 获取所有商品
products = requests.get(f"{BASE_URL}/base/products?page=1&limit=50", headers=headers).json()["data"]["list"]
print(f"总商品数: {len(products)}")

# 为未关联的商品创建关联
# 供应商: 1=深圳电子(数码), 2=广州服装(服装鞋包), 3=义乌小商品(日用品运动), 5=湖南电子(数码)
# 供应商6=Test Supplier 用于食品类

new_relations = [
    # 数码产品 -> 深圳电子
    {"supplierId": 1, "productId": 3, "supplierSkuCode": "SZ-IP15-CASE", "purchasePrice": 18.00, "minOrderQty": 100, "leadTime": 3},  # SKU-001 手机壳

    # 鞋包 -> 广州服装
    {"supplierId": 2, "productId": 9, "supplierSkuCode": "GZ-SHOES-B42", "purchasePrice": 180.00, "minOrderQty": 20, "leadTime": 7},  # SKU-007 跑鞋
    {"supplierId": 2, "productId": 10, "supplierSkuCode": "GZ-BAG-BK", "purchasePrice": 220.00, "minOrderQty": 15, "leadTime": 10},  # SKU-008 公文包

    # 美妆个护 -> 义乌小商品
    {"supplierId": 3, "productId": 11, "supplierSkuCode": "YW-SHAMPOO-500", "purchasePrice": 28.00, "minOrderQty": 50, "leadTime": 5},  # SKU-009 洗发水
    {"supplierId": 3, "productId": 12, "supplierSkuCode": "YW-CREAM-50G", "purchasePrice": 68.00, "minOrderQty": 30, "leadTime": 5},  # SKU-010 面霜

    # 母婴用品 -> 义乌小商品
    {"supplierId": 3, "productId": 13, "supplierSkuCode": "YW-FORMULA-800", "purchasePrice": 185.00, "minOrderQty": 20, "leadTime": 7},  # SKU-011 奶粉
    {"supplierId": 3, "productId": 14, "supplierSkuCode": "YW-DIAPER-M100", "purchasePrice": 95.00, "minOrderQty": 30, "leadTime": 5},  # SKU-012 纸尿裤

    # 食品 - 冷藏/冷冻 -> Test Supplier (假设有冷链能力)
    {"supplierId": 6, "productId": 15, "supplierSkuCode": "TS-MILK-1L", "purchasePrice": 8.50, "minOrderQty": 200, "leadTime": 1},  # SKU-013 鲜牛奶
    {"supplierId": 6, "productId": 16, "supplierSkuCode": "TS-DUMPLING-500", "purchasePrice": 15.00, "minOrderQty": 100, "leadTime": 2},  # SKU-014 水饺
    {"supplierId": 6, "productId": 17, "supplierSkuCode": "TS-STEAK-200", "purchasePrice": 45.00, "minOrderQty": 50, "leadTime": 3},  # SKU-015 牛排

    # 保健品 -> 义乌小商品
    {"supplierId": 3, "productId": 18, "supplierSkuCode": "YW-VITC-100", "purchasePrice": 35.00, "minOrderQty": 50, "leadTime": 5},  # SKU-016 维生素C

    # 运动户外 -> 义乌小商品
    {"supplierId": 3, "productId": 19, "supplierSkuCode": "YW-YOGAMAT-10", "purchasePrice": 48.00, "minOrderQty": 20, "leadTime": 5},  # SKU-017 瑜伽垫
    {"supplierId": 3, "productId": 20, "supplierSkuCode": "YW-RACKET-SET", "purchasePrice": 72.00, "minOrderQty": 15, "leadTime": 5},  # SKU-018 羽毛球拍

    # 测试商品
    {"supplierId": 1, "productId": 1, "supplierSkuCode": "SZ-NEW01", "purchasePrice": 10.00, "minOrderQty": 100, "leadTime": 5},  # SKU-NEW01
    {"supplierId": 1, "productId": 2, "supplierSkuCode": "SZ-NEW02", "purchasePrice": 15.00, "minOrderQty": 50, "leadTime": 5},  # SKU-NEW02
]

success = 0
for r in new_relations:
    if r["productId"] in linked_product_ids:
        # 检查是否已关联该供应商
        existing_for_product = [sp for sp in existing if sp["productId"] == r["productId"]]
        if any(sp["supplierId"] == r["supplierId"] for sp in existing_for_product):
            print(f"SKIP: productId={r['productId']} 已关联该供应商")
            continue

    resp = requests.post(f"{BASE_URL}/base/supplier-products", headers=headers, json=r)
    if resp.json().get("success"):
        success += 1
        product = next((p for p in products if p["id"] == r["productId"]), None)
        print(f"OK: productId={r['productId']} ({product['skuCode'] if product else '?'}) -> supplierId={r['supplierId']}")
    else:
        print(f"FAIL: productId={r['productId']} - {resp.json().get('message')}")

print(f"\n成功创建 {success} 条新关联")

# 验证
final = requests.get(f"{BASE_URL}/base/supplier-products?page=1&limit=100", headers=headers).json()["data"]["list"]
print(f"当前总关联数: {len(final)}")
