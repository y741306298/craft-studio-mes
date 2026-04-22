# AuthController 接口文档

本文档描述 `AuthController` 提供的两个接口：登录与新增用户。

- Controller 路径前缀：`/api/auth`
- 返回结构：统一为 `ApiResponse<T>`

---

## 1. 登录

- **URL**：`POST /api/auth/login`
- **描述**：使用账号密码登录，返回 token、所属工厂 `manufacturerMetaId` 和 token 过期时间（默认 3 天）。

### 请求参数

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

### 响应 data 字段

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

### 请求参数

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

### 响应说明

- 成功时 `data` 固定为 `"success"`

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

## 错误码说明（常见）

| code | 含义 |
|---|---|
| 400 | 参数错误 |
| 401 | 未授权（账号或密码错误） |
| 500 | 服务异常 |

