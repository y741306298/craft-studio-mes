# listDeviceCfgs / listOrders 查询参数说明

本文档说明以下两个接口新增查询能力及请求示例：

- `listDeviceCfgs`：在原有 `manufacturerMetaId` 基础上，支持 `deviceName`、`deviceCode` 查询。
- `listOrders`：在原有查询条件基础上，支持 `customerName`、`customerPhone` 查询。

---

## 1. listDeviceCfgs 接口

### 1.1 接口地址

该能力在两个端口控制器都已支持：

- 配置端：`POST /api/configSide/deviceCfg/list`
- 制造商端：`POST /api/manufacturerSide/deviceCfg/list`

### 1.2 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `current` | number | 否 | 页码，默认按系统分页规则处理 |
| `size` | number | 否 | 每页条数，默认按系统分页规则处理 |
| `manufacturerMetaId` | string | 是 | 制造商 ID |
| `deviceName` | string | 否 | 设备名称（与 `manufacturerMetaId` 组合过滤） |
| `deviceCode` | string | 否 | 设备编号（与 `manufacturerMetaId` 组合过滤） |

> 说明：`deviceName` 为模糊匹配（包含关系，忽略大小写）；`manufacturerMetaId`、`deviceCode` 为等值匹配。

### 1.3 请求示例

#### 示例 A：仅按 manufacturerMetaId 查询

```json
{
  "current": 1,
  "size": 20,
  "manufacturerMetaId": "MFG_10001"
}
```

#### 示例 B：按 manufacturerMetaId + deviceName 查询

```json
{
  "current": 1,
  "size": 20,
  "manufacturerMetaId": "MFG_10001",
  "deviceName": "激光切割机A"
}
```

#### 示例 C：按 manufacturerMetaId + deviceCode 查询

```json
{
  "current": 1,
  "size": 20,
  "manufacturerMetaId": "MFG_10001",
  "deviceCode": "DEV-CODE-0001"
}
```

#### 示例 D：按 manufacturerMetaId + deviceName + deviceCode 组合查询

```json
{
  "current": 1,
  "size": 20,
  "manufacturerMetaId": "MFG_10001",
  "deviceName": "激光切割机A",
  "deviceCode": "DEV-CODE-0001"
}
```

---

## 2. listOrders 接口

### 2.1 接口地址

- 制造商端：`POST /api/manufacturerSide/order/list`

### 2.2 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `current` | number | 否 | 页码，默认按系统分页规则处理 |
| `size` | number | 否 | 每页条数，默认按系统分页规则处理 |
| `manufacturerId` | string | 建议必填 | 制造商 ID |
| `orderId` | string | 否 | 订单号 |
| `status` | string | 否 | 订单状态枚举值 |
| `customerName` | string | 否 | 客户姓名 |
| `customerPhone` | string | 否 | 客户手机号 |
| `createDateStart` | string | 否 | 起始日期，格式 `yyyy-MM-dd` |
| `createDateEnd` | string | 否 | 结束日期，格式 `yyyy-MM-dd` |

> 说明：`customerName` 为模糊匹配（包含关系，忽略大小写）；`customerPhone` 为等值匹配。

### 2.3 请求示例

#### 示例 A：按客户姓名 + 手机号查询

```json
{
  "current": 1,
  "size": 20,
  "manufacturerId": "MFG_10001",
  "customerName": "张三",
  "customerPhone": "13800000000"
}
```

#### 示例 B：按订单状态 + 日期区间 + 客户姓名查询

```json
{
  "current": 1,
  "size": 20,
  "manufacturerId": "MFG_10001",
  "status": "PENDING",
  "customerName": "李四",
  "createDateStart": "2026-05-01",
  "createDateEnd": "2026-05-05"
}
```

---

## 3. 调用建议

1. 当需要唯一定位设备配置时，优先组合 `manufacturerMetaId + deviceCode`。
2. 当客户重名较多时，建议组合 `customerName + customerPhone` 共同过滤。
3. 若后续需要“包含匹配/前缀匹配”，建议新增独立的模糊查询参数或开关，避免影响现有等值查询语义。
