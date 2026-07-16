# ConfigSide 登录与登录态校验接口文档

本文档描述配置端登录接口、token 查询接口，以及 `/api/configSide/**` 业务接口的登录态校验规则。配置端使用独立的 `configUser` 用户表，不复用工厂端 `manufacturerUser` 登录账号。

## 1. 基本信息

- Controller 路径前缀：`/api/configSide/auth`
- 登录接口：`POST /api/configSide/auth/login`
- token 查询接口：`GET /api/configSide/auth/token/configUserId`
- 受保护业务接口：除上述认证接口外，所有 `/api/configSide/**` 接口都需要在请求头携带登录 token。
- 请求头格式：`Authorization: Bearer <token>`
- 返回结构：`ApiResponse<T>`

## 2. 配置端登录

- **URL**：`POST /api/configSide/auth/login`
- **Content-Type**：`application/json`
- **描述**：使用配置端账号密码登录，登录成功后返回 token、配置端用户信息和 token 过期时间。配置端账号来源于独立的 `configUser` 表，工厂端 `manufacturerUser` 账号不能登录配置端。

### 请求字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| account | string | 是 | 登录账号 |
| password | string | 是 | 登录密码 |

### 请求示例

```http
POST /api/configSide/auth/login
Content-Type: application/json
```

```json
{
  "account": "13800001111",
  "password": "123456"
}
```

### 响应 data 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| token | string | 登录令牌，后续访问 `/api/configSide/**` 业务接口时放入 `Authorization` 请求头 |
| userName | string | 用户名称 |
| isAdmin | boolean | 是否管理员 |
| tokenExpireAt | string(datetime) | token 过期时间 |

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "c5f4f5f3d0...",
    "userName": "张三",
    "isAdmin": true,
    "tokenExpireAt": "2026-07-23T10:15:30.000+00:00"
  },
  "timestamp": 1784801730000
}
```

## 3. 配置端业务接口登录态校验

除 `/api/configSide/auth/login` 和 `/api/configSide/auth/token/configUserId` 外，访问其他 `/api/configSide/**` 接口时必须携带登录 token。

### 请求头

| Header | 必填 | 说明 |
|---|---|---|
| Authorization | 是 | 固定格式：`Bearer <登录接口返回的 token>` |

### 请求示例

```http
POST /api/configSide/device/add
Content-Type: application/json
Authorization: Bearer c5f4f5f3d0...
```

### 未登录或 token 缺失响应示例

```json
{
  "code": 401,
  "message": "请先登录",
  "data": null,
  "timestamp": 1784801730000
}
```

### token 失效响应示例

```json
{
  "code": 401,
  "message": "登录已失效，请重新登录",
  "data": null,
  "timestamp": 1784801730000
}
```

## 4. 根据 token 查询 configUserId

- **URL**：`GET /api/configSide/auth/token/configUserId`
- **描述**：根据登录后获取的 token 查询配置端用户 `configUserId`。

### 请求参数（Query）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| token | string | 是 | 登录令牌 |

### 请求示例

```http
GET /api/configSide/auth/token/configUserId?token=c5f4f5f3d0...
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": "CFG_USER_10001",
  "timestamp": 1784801730000
}
```

## 5. 常见错误码

| code | 含义 | 场景 |
|---|---|---|
| 400 | 参数错误 | `account`、`password` 或 `token` 为空等参数校验失败 |
| 401 | 未授权 | 账号或密码错误、未携带 `Authorization`、token 无效或已过期 |
| 500 | 服务异常 | 服务端未知异常 |
