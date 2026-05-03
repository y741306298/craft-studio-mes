# AuthController 接口文档

本文档描述 `AuthController` 的认证与用户管理接口，包括登录、新增用户、按工厂分页查询用户（支持手机号模糊）、删除用户、更新用户密码、根据 token 查询 `manufacturerMetaId`。

- Controller 路径前缀：`/api/auth`
- 返回结构：
  - 普通接口：`ApiResponse<T>`
  - 分页接口：`PagedApiResponse<T>`（`data` 内含 `items/current/size/total`）

---

## 1. 登录

- **URL**：`POST /api/auth/login`
- **描述**：使用账号密码登录，返回 token、所属工厂 `manufacturerMetaId` 和 token 过期时间（默认 3 天）。

### 请求字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| account | string | 是 | 登录账号 |
| password | string | 是 | 登录密码 |

### 请求示例

```json
{
  "account": "13800001111",
  "password": "123456"
}
```

### 响应 data 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| token | string | 登录令牌 |
| manufacturerMetaId | string | 用户所属工厂ID |
| tokenExpireAt | string(datetime) | 令牌过期时间 |

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "c5f4f5f3d0...",
    "manufacturerMetaId": "RMF_10001",
    "tokenExpireAt": "2026-04-25T10:15:30.000+00:00"
  },
  "timestamp": 1713760000000
}
```

---

## 2. 新增用户

- **URL**：`POST /api/auth/user/add`
- **描述**：新增一个工厂用户。

### 请求字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| account | string | 是 | 用户账号 |
| password | string | 是 | 用户密码 |
| manufacturerMetaId | string | 是 | 所属工厂ID |
| name | string | 否 | 用户姓名 |
| phone | string | 否 | 用户手机号 |

### 请求示例

```json
{
  "account": "13800002222",
  "password": "123456",
  "manufacturerMetaId": "RMF_10001",
  "name": "张三",
  "phone": "13800002222"
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": "success",
  "timestamp": 1713760000000
}
```

---

## 3. 分页查询用户列表（支持手机号模糊）

- **URL**：`POST /api/auth/user/list`
- **描述**：按 `manufacturerMetaId` 查询用户列表；当 `phone` 非空时进行手机号模糊匹配（`regex`）。

### 请求字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| manufacturerMetaId | string | 是 | 工厂ID |
| phone | string | 否 | 手机号模糊关键词，例如 `138` |
| current | number | 是 | 页码，从 1 开始 |
| size | number | 是 | 每页条数，建议 1-100 |

### 请求示例（按工厂查全部）

```json
{
  "manufacturerMetaId": "RMF_10001",
  "current": 1,
  "size": 10
}
```

### 请求示例（按手机号模糊查）

```json
{
  "manufacturerMetaId": "RMF_10001",
  "phone": "138",
  "current": 1,
  "size": 10
}
```

### 响应 data 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| items | array | 当前页用户列表 |
| current | number | 当前页码 |
| size | number | 每页条数 |
| total | number | 总记录数 |

`items` 内单个用户字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | string | 用户ID |
| account | string | 用户账号 |
| manufacturerMetaId | string | 工厂ID |
| name | string | 用户姓名 |
| phone | string | 用户手机号 |

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "u_001",
        "account": "13800002222",
        "manufacturerMetaId": "RMF_10001",
        "name": "张三",
        "phone": "13800002222"
      }
    ],
    "current": 1,
    "size": 10,
    "total": 1
  },
  "timestamp": 1713760000000
}
```

---

## 4. 删除用户

- **URL**：`POST /api/auth/user/delete`
- **描述**：根据用户 ID 删除用户（软删除）。

### 请求字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | string | 是 | 用户ID |

### 请求示例

```json
{
  "id": "u_001"
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": "success",
  "timestamp": 1713760000000
}
```

---

## 5. 更新用户密码

- **URL**：`POST /api/auth/user/password/update`
- **描述**：根据用户 ID 更新密码。

### 请求字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | string | 是 | 用户ID |
| password | string | 是 | 新密码 |

### 请求示例

```json
{
  "id": "u_001",
  "password": "new_password_123"
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": "success",
  "timestamp": 1713760000000
}
```

---

## 6. 根据 token 查询 manufacturerMetaId

- **URL**：`GET /api/auth/token/manufacturerMetaId`
- **描述**：根据登录后获取的 token 查询用户所属工厂 `manufacturerMetaId`。

### 请求参数（Query）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| token | string | 是 | 登录令牌 |

### 请求示例

```http
GET /api/auth/token/manufacturerMetaId?token=c5f4f5f3d0...
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": "RMF_10001",
  "timestamp": 1713760000000
}
```

---

## 常见错误码

| code | 含义 |
|---|---|
| 400 | 参数错误 |
| 401 | 未授权（账号或密码错误 / token 无效） |
| 500 | 服务异常 |
