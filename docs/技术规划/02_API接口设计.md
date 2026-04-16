# 系统管理模块 - API接口设计

> WMS仓储管理系统 - 系统管理模块 RESTful API 规范

---

## 一、接口规范

### 1.1 基础规范

- **Base URL**: `/api/v1/system`
- **数据格式**: JSON
- **字符编码**: UTF-8
- **认证方式**: Bearer Token (JWT)

### 1.2 通用响应格式

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1712800000000,
  "path": "/api/v1/system/users"
}
```

**错误响应格式**：

```json
{
  "success": false,
  "code": 400,
  "message": "参数校验失败",
  "errors": [
    {
      "field": "username",
      "message": "用户名不能为空"
    }
  ],
  "timestamp": 1712800000000,
  "path": "/api/v1/system/users"
}
```

### 1.3 分页响应格式

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 100,
      "totalPages": 5
    }
  }
}
```

---

## 二、用户管理接口

### 2.1 用户列表

```
GET /api/v1/system/users
```

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| limit | int | 否 | 每页条数，默认20 |
| keyword | string | 否 | 搜索关键词（用户名/姓名） |
| roleId | long | 否 | 角色ID |
| warehouseId | long | 否 | 仓库ID |
| status | int | 否 | 状态：0-禁用，1-启用 |

**Response**:

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 1,
        "username": "admin",
        "name": "系统管理员",
        "phone": "13800138000",
        "email": "admin@wms.com",
        "roleId": 1,
        "roleName": "超级管理员",
        "warehouseId": null,
        "warehouseName": "-",
        "status": 1,
        "statusText": "正常",
        "lastLoginTime": "2026-04-10 10:25:00",
        "createTime": "2026-01-01 00:00:00"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 1,
      "totalPages": 1
    }
  }
}
```

---

### 2.2 用户详情

```
GET /api/v1/system/users/{id}
```

**Response**:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin",
    "name": "系统管理员",
    "phone": "13800138000",
    "email": "admin@wms.com",
    "roleId": 1,
    "roleName": "超级管理员",
    "warehouseId": null,
    "warehouseName": "-",
    "status": 1,
    "statusText": "正常",
    "lastLoginTime": "2026-04-10 10:25:00",
    "lastLoginIp": "192.168.1.100",
    "createTime": "2026-01-01 00:00:00",
    "createUserName": "系统初始化"
  }
}
```

---

### 2.3 创建用户

```
POST /api/v1/system/users
```

**Request Body**:

```json
{
  "username": "test_user",
  "password": "Test@123456",
  "name": "测试用户",
  "phone": "13800138000",
  "email": "test@example.com",
  "roleId": 2,
  "warehouseId": 1
}
```

**Validation Rules**:

| 字段 | 规则 |
|------|------|
| username | 必填，4-20字符，唯一 |
| password | 必填，6-20字符 |
| name | 必填，2-100字符 |
| phone | 可选，手机号格式 |
| email | 可选，邮箱格式 |
| roleId | 必填，正整数 |
| warehouseId | 可选，正整数 |

**Response**:

```json
{
  "success": true,
  "message": "用户创建成功",
  "data": {
    "id": 10
  }
}
```

---

### 2.4 更新用户

```
PUT /api/v1/system/users/{id}
```

**Request Body**:

```json
{
  "name": "张三修改",
  "phone": "13900139000",
  "email": "zhangsan@example.com",
  "roleId": 3,
  "warehouseId": 2
}
```

---

### 2.5 删除用户

```
DELETE /api/v1/system/users/{id}
```

**Response**:

```json
{
  "success": true,
  "message": "用户已删除"
}
```

---

### 2.6 启用用户

```
PATCH /api/v1/system/users/{id}/enable
```

**Response**:

```json
{
  "success": true,
  "message": "用户已启用"
}
```

---

### 2.7 禁用用户

```
PATCH /api/v1/system/users/{id}/disable
```

**Response**:

```json
{
  "success": true,
  "message": "用户已禁用"
}
```

---

### 2.8 重置密码

```
POST /api/v1/system/users/{id}/reset-password
```

**Response**:

```json
{
  "success": true,
  "message": "重置密码链接已发送到邮箱"
}
```

---

### 2.9 修改密码

```
POST /api/v1/system/users/{id}/change-password
```

**Request Body**:

```json
{
  "oldPassword": "Old@123456",
  "newPassword": "New@123456"
}
```

---

## 三、角色权限接口

### 3.1 角色列表

```
GET /api/v1/system/roles
```

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| limit | int | 否 | 每页条数 |
| keyword | string | 否 | 搜索关键词 |
| type | int | 否 | 类型：1-预置，2-自定义 |
| status | int | 否 | 状态 |

**Response**:

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 1,
        "code": "SUPER_ADMIN",
        "name": "超级管理员",
        "type": 1,
        "typeText": "预置",
        "description": "拥有全部系统权限",
        "userCount": 1,
        "permissionCount": 50,
        "status": 1,
        "statusText": "启用",
        "createTime": "2026-01-01 00:00:00"
      }
    ],
    "pagination": {}
  }
}
```

---

### 3.2 角色详情

```
GET /api/v1/system/roles/{id}
```

**Response**:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "SUPER_ADMIN",
    "name": "超级管理员",
    "type": 1,
    "typeText": "预置",
    "description": "拥有全部系统权限",
    "status": 1,
    "isSystem": true,
    "permissions": [
      {
        "id": 1,
        "code": "system:user:view",
        "name": "用户查看",
        "module": "系统管理"
      }
    ],
    "createTime": "2026-01-01 00:00:00"
  }
}
```

---

### 3.3 创建角色

```
POST /api/v1/system/roles
```

**Request Body**:

```json
{
  "code": "CUSTOM_ROLE_01",
  "name": "自定义角色",
  "description": "测试用自定义角色",
  "permissionIds": [1, 2, 3, 10, 11]
}
```

---

### 3.4 更新角色

```
PUT /api/v1/system/roles/{id}
```

**Request Body**:

```json
{
  "name": "自定义角色v2",
  "description": "修改后的描述",
  "permissionIds": [1, 2, 3, 10, 11, 15]
}
```

---

### 3.5 删除角色

```
DELETE /api/v1/system/roles/{id}
```

---

### 3.6 启用角色

```
PATCH /api/v1/system/roles/{id}/enable
```

---

### 3.7 禁用角色

```
PATCH /api/v1/system/roles/{id}/disable
```

---

### 3.8 获取权限树

```
GET /api/v1/system/permissions/tree
```

**Response**:

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "system",
      "name": "系统管理",
      "children": [
        {
          "id": 2,
          "code": "system:user",
          "name": "用户管理",
          "children": [
            {
              "id": 3,
              "code": "system:user:view",
              "name": "用户查看"
            },
            {
              "id": 4,
              "code": "system:user:create",
              "name": "用户创建"
            }
          ]
        }
      ]
    },
    {
      "id": 10,
      "code": "inbound",
      "name": "入库管理",
      "children": []
    }
  ]
}
```

---

### 3.9 批量启用角色

```
PATCH /api/v1/system/roles/batch-enable
```

**Request Body**:

```json
{
  "ids": [1, 2, 3]
}
```

---

### 3.10 批量禁用角色

```
PATCH /api/v1/system/roles/batch-disable
```

**Request Body**:

```json
{
  "ids": [1, 2, 3]
}
```

---

### 3.11 批量删除角色

```
DELETE /api/v1/system/roles/batch-delete
```

**Request Body**:

```json
{
  "ids": [1, 2, 3]
}
```

---

## 四、日志审计接口

### 4.1 操作日志列表

```
GET /api/v1/system/logs/operations
```

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| limit | int | 否 | 每页条数 |
| startTime | string | 否 | 开始时间 |
| endTime | string | 否 | 结束时间 |
| userId | long | 否 | 操作人ID |
| module | string | 否 | 模块 |
| action | string | 否 | 操作类型 |
| result | string | 否 | 结果 |

**Response**:

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 1,
        "time": "2026-04-10 10:25:12",
        "userId": 2,
        "username": "张三",
        "module": "入库管理",
        "action": "创建入库单",
        "object": "RK-20260410-0012",
        "ip": "192.168.1.100",
        "result": "成功",
        "details": {
          "入库单号": "RK-20260410-0012",
          "入库类型": "采购入库",
          "总数量": 100
        }
      }
    ],
    "pagination": {}
  }
}
```

---

### 4.2 登录日志列表

```
GET /api/v1/system/logs/login
```

**Query Parameters**:

同操作日志

**Response**:

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 1,
        "time": "2026-04-10 10:00:00",
        "userId": 1,
        "username": "admin",
        "action": "登录",
        "ip": "192.168.1.100",
        "device": "Windows 10",
        "browser": "Chrome 120",
        "result": "成功",
        "failReason": null
      }
    ],
    "pagination": {}
  }
}
```

---

### 4.3 库存变动日志列表

```
GET /api/v1/system/logs/inventory
```

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startTime | string | 否 | 开始时间 |
| endTime | string | 否 | 结束时间 |
| skuCode | string | 否 | SKU编码 |
| warehouseId | long | 否 | 仓库ID |
| changeType | string | 否 | 变动类型 |

**Response**:

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 1,
        "time": "2026-04-10 10:30:00",
        "userId": 2,
        "username": "张三",
        "skuCode": "SKU001",
        "skuName": "iPhone15手机壳",
        "warehouseCode": "WH-VN-001",
        "locationCode": "A-01-02-03",
        "changeType": "入库",
        "changeQuantity": "+100",
        "quantityBefore": 50,
        "quantityAfter": 150,
        "referenceNo": "RK-20260410-0012"
      }
    ],
    "pagination": {}
  }
}
```

---

### 4.4 日志详情

```
GET /api/v1/system/logs/{type}/{id}
```

**Path Parameters**:

| 参数 | 类型 | 说明 |
|------|------|------|
| type | string | 日志类型：operation/login/inventory |
| id | long | 日志ID |

---

### 4.5 导出日志

```
GET /api/v1/system/logs/export
```

**Query Parameters**:

同列表查询参数

**Response**: Excel文件下载

---

## 五、系统配置接口

### 5.1 配置列表

```
GET /api/v1/system/configs
```

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category | string | 否 | 配置分类 |
| keyword | string | 否 | 搜索关键词 |

**Response**:

```json
{
  "success": true,
  "data": {
    "基础参数": [
      {
        "id": 1,
        "code": "system.name",
        "name": "系统名称",
        "value": "WMS仓库管理系统",
        "type": "string",
        "defaultValue": "WMS仓库管理系统",
        "description": "系统显示名称",
        "isSystem": true,
        "updateTime": "2026-01-01 00:00:00"
      }
    ],
    "业务参数": [],
    "编码规则": [],
    "会话管理": [],
    "安全配置": [],
    "作业配置": [],
    "通知配置": []
  }
}
```

---

### 5.2 配置详情

```
GET /api/v1/system/configs/{code}
```

---

### 5.3 更新配置

```
PUT /api/v1/system/configs/{code}
```

**Request Body**:

```json
{
  "value": "新的配置值",
  "reason": "业务需要修改"
}
```

---

### 5.4 批量更新配置

```
PUT /api/v1/system/configs/batch-update
```

**Request Body**:

```json
{
  "configs": [
    {
      "code": "code.inbound_prefix",
      "value": "IN",
      "reason": "统一编码规范"
    },
    {
      "code": "code.outbound_prefix",
      "value": "OUT",
      "reason": "统一编码规范"
    }
  ]
}
```

---

### 5.5 获取配置历史

```
GET /api/v1/system/configs/{code}/history
```

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| limit | int | 否 | 每页条数 |

**Response**:

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 1,
        "oldValue": "RK",
        "newValue": "IN",
        "operatorId": 1,
        "operatorName": "系统管理员",
        "reason": "统一编码规范",
        "createdAt": "2026-04-10 10:00:00"
      }
    ],
    "pagination": {}
  }
}
```

---

### 5.6 回滚配置

```
POST /api/v1/system/configs/{code}/rollback
```

**Request Body**:

```json
{
  "historyId": 1,
  "reason": "恢复之前的配置"
}
```

---

### 5.7 恢复默认配置

```
POST /api/v1/system/configs/{code}/reset
```

---

### 5.8 批量恢复默认配置

```
POST /api/v1/system/configs/batch-reset
```

**Request Body**:

```json
{
  "codes": ["code.inbound_prefix", "code.outbound_prefix"]
}
```

---

### 5.9 导出配置

```
GET /api/v1/system/configs/export
```

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category | string | 否 | 分类，为空则导出全部 |

**Response**: JSON文件下载

---

### 5.10 导入配置

```
POST /api/v1/system/configs/import
```

**Request**: multipart/form-data

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 导入文件 |
| strategy | string | 否 | 策略：merge覆盖/ignore跳过 |

---

## 六、仓库管理接口（系统管理用到）

### 6.1 仓库列表

```
GET /api/v1/system/warehouses
```

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词 |
| status | int | 否 | 状态 |

---

## 七、错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如用户名重复） |
| 500 | 服务器内部错误 |

### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 1001 | 用户不存在 |
| 1002 | 用户名已存在 |
| 1003 | 不能停用当前登录账号 |
| 1004 | 不能停用超级管理员 |
| 1005 | 用户存在业务数据关联，无法删除 |
| 1006 | 该用户未设置邮箱，无法发送重置邮件 |
| 2001 | 角色不存在 |
| 2002 | 角色编码已存在 |
| 2003 | 预置角色不可编辑 |
| 2004 | 预置角色不可删除 |
| 2005 | 该角色下有用户，无法删除 |
| 3001 | 配置不存在 |
| 3002 | 配置编码已存在 |
| 3003 | 预置配置不可删除 |
| 3004 | 配置值不能为空 |
| 3005 | 单号前缀格式错误（只能为字母） |
| 3006 | 单号前缀长度不能超过5个字符 |
| 3007 | 预警天数必须为正整数 |
| 3008 | 预警天数不能超过365天 |
| 3009 | 入库单号前缀不能与出库单号前缀相同 |

---

*文档完成于 2026-04-11*
