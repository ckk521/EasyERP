import requests
import json

BASE_URL = "http://localhost:8081/api/v1"

# 登录获取token
login_resp = requests.post(f"{BASE_URL}/auth/login", json={"username": "admin", "password": "admin123"})
token = login_resp.json()["data"]["token"]
headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json; charset=utf-8"}

products = [
    {"skuCode": "SKU-001", "barcode": "6901234000001", "nameCn": "iPhone 15手机壳透明款", "nameEn": "iPhone 15 Clear Case", "categoryId": 25, "brand": "Apple", "supplierId": 1, "weight": 0.03, "length": 16, "width": 8, "height": 1, "storageCond": 1, "shelfLife": 1095, "expiryWarning": 90},
    {"skuCode": "SKU-002", "barcode": "6901234000002", "nameCn": "iPhone 15钢化膜", "nameEn": "iPhone 15 Tempered Glass", "categoryId": 26, "brand": "Apple", "supplierId": 1, "weight": 0.05, "length": 15, "width": 7, "height": 0.5, "storageCond": 1, "shelfLife": 1095, "isFragile": 1},
    {"skuCode": "SKU-003", "barcode": "6901234000003", "nameCn": "20W快充充电器", "nameEn": "20W Fast Charger", "categoryId": 27, "brand": "Anker", "supplierId": 1, "weight": 0.12, "length": 5, "width": 5, "height": 3, "storageCond": 1, "shelfLife": 730},
    {"skuCode": "SKU-004", "barcode": "6901234000004", "nameCn": "Type-C数据线1米", "nameEn": "Type-C Cable 1m", "categoryId": 28, "brand": "Anker", "supplierId": 1, "weight": 0.04, "length": 100, "width": 2, "height": 2, "storageCond": 1, "shelfLife": 730},
    {"skuCode": "SKU-005", "barcode": "6901234000005", "nameCn": "男士纯棉T恤白色L码", "nameEn": "Mens Cotton T-Shirt White L", "categoryId": 12, "brand": "优衣库", "supplierId": 2, "weight": 0.2, "length": 30, "width": 25, "height": 2, "storageCond": 1, "shelfLife": 1825},
    {"skuCode": "SKU-006", "barcode": "6901234000006", "nameCn": "女士牛仔裤蓝色M码", "nameEn": "Womens Jeans Blue M", "categoryId": 12, "brand": "Levis", "supplierId": 2, "weight": 0.45, "length": 35, "width": 28, "height": 3, "storageCond": 1, "shelfLife": 1825},
    {"skuCode": "SKU-007", "barcode": "6901234000007", "nameCn": "运动跑鞋黑色42码", "nameEn": "Running Shoes Black 42", "categoryId": 13, "brand": "Nike", "supplierId": 3, "weight": 0.35, "length": 28, "width": 12, "height": 10, "storageCond": 1, "shelfLife": 1095},
    {"skuCode": "SKU-008", "barcode": "6901234000008", "nameCn": "商务公文包黑色", "nameEn": "Business Briefcase Black", "categoryId": 13, "brand": "新秀丽", "supplierId": 3, "weight": 0.8, "length": 40, "width": 28, "height": 8, "storageCond": 1, "shelfLife": 1825, "isHighValue": 1},
    {"skuCode": "SKU-009", "barcode": "6901234000009", "nameCn": "海飞丝去屑洗发水500ml", "nameEn": "Head Shoulders 500ml", "categoryId": 14, "brand": "海飞丝", "supplierId": 4, "weight": 0.55, "length": 8, "width": 5, "height": 20, "storageCond": 1, "shelfLife": 730, "needExpiryMgmt": 1},
    {"skuCode": "SKU-010", "barcode": "6901234000010", "nameCn": "玉兰油面霜50g", "nameEn": "Olay Cream 50g", "categoryId": 14, "brand": "玉兰油", "supplierId": 4, "weight": 0.08, "length": 8, "width": 8, "height": 5, "storageCond": 1, "shelfLife": 1095, "isHighValue": 1, "needExpiryMgmt": 1},
    {"skuCode": "SKU-011", "barcode": "6901234000011", "nameCn": "婴幼儿奶粉800g", "nameEn": "Baby Formula 800g", "categoryId": 16, "brand": "美赞臣", "supplierId": 5, "weight": 0.9, "length": 15, "width": 12, "height": 20, "storageCond": 1, "shelfLife": 365, "isHighValue": 1, "needExpiryMgmt": 1},
    {"skuCode": "SKU-012", "barcode": "6901234000012", "nameCn": "婴儿纸尿裤M码100片", "nameEn": "Baby Diapers M 100pcs", "categoryId": 16, "brand": "帮宝适", "supplierId": 5, "weight": 1.5, "length": 45, "width": 35, "height": 15, "storageCond": 1, "shelfLife": 1095},
    {"skuCode": "SKU-013", "barcode": "6901234000013", "nameCn": "鲜牛奶1L装", "nameEn": "Fresh Milk 1L", "categoryId": 18, "brand": "蒙牛", "supplierId": 6, "weight": 1.05, "length": 10, "width": 7, "height": 20, "storageCond": 2, "shelfLife": 7, "needExpiryMgmt": 1},
    {"skuCode": "SKU-014", "barcode": "6901234000014", "nameCn": "冷冻水饺猪肉白菜500g", "nameEn": "Frozen Dumplings 500g", "categoryId": 18, "brand": "思念", "supplierId": 6, "weight": 0.55, "length": 20, "width": 15, "height": 5, "storageCond": 3, "shelfLife": 180, "needExpiryMgmt": 1},
    {"skuCode": "SKU-015", "barcode": "6901234000015", "nameCn": "进口牛排冷冻装200g", "nameEn": "Frozen Steak 200g", "categoryId": 18, "brand": "澳洲牧场", "supplierId": 6, "weight": 0.22, "length": 15, "width": 10, "height": 2, "storageCond": 3, "shelfLife": 365, "isHighValue": 1, "needExpiryMgmt": 1},
    {"skuCode": "SKU-016", "barcode": "6901234000016", "nameCn": "维生素C片100片", "nameEn": "Vitamin C 100 Tablets", "categoryId": 18, "brand": "汤臣倍健", "supplierId": 4, "weight": 0.1, "length": 10, "width": 5, "height": 5, "storageCond": 1, "shelfLife": 730, "needExpiryMgmt": 1},
    {"skuCode": "SKU-017", "barcode": "6901234000017", "nameCn": "瑜伽垫加厚10mm", "nameEn": "Yoga Mat 10mm", "categoryId": 17, "brand": "Keep", "supplierId": 3, "weight": 1.2, "length": 183, "width": 61, "height": 1, "storageCond": 1, "shelfLife": 1825},
    {"skuCode": "SKU-018", "barcode": "6901234000018", "nameCn": "羽毛球拍双支装", "nameEn": "Badminton Racket Set", "categoryId": 17, "brand": "尤尼克斯", "supplierId": 3, "weight": 0.4, "length": 68, "width": 25, "height": 5, "storageCond": 1, "shelfLife": 1825},
    {"skuCode": "SKU-019", "barcode": "6901234000019", "nameCn": "A4打印纸500张", "nameEn": "A4 Paper 500 Sheets", "categoryId": 19, "brand": "得力", "supplierId": 3, "weight": 2.5, "length": 30, "width": 21, "height": 5, "storageCond": 1, "shelfLife": 3650},
    {"skuCode": "SKU-020", "barcode": "6901234000020", "nameCn": "中性笔黑色0.5mm 12支", "nameEn": "Gel Pen Black 12pcs", "categoryId": 19, "brand": "晨光", "supplierId": 3, "weight": 0.15, "length": 18, "width": 8, "height": 2, "storageCond": 1, "shelfLife": 1825},
]

success_count = 0
for p in products:
    resp = requests.post(f"{BASE_URL}/base/products", headers=headers, json=p)
    if resp.json().get("success"):
        success_count += 1
        print(f"OK {p['skuCode']} - {p['nameCn']}")
    else:
        print(f"FAIL {p['skuCode']}: {resp.json().get('message')}")

print(f"\n成功创建 {success_count}/{len(products)} 条商品数据")
