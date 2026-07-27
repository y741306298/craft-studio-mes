# 旺店通快递配置接口文档

本文档描述工厂端旺店通（WDT）快递配置的新增、修改、删除、详情和分页查询接口。所有接口的基础路径为：

`/api/manufacturerSide/wdt`

## 通用数据结构

### 快递配置字段

| 字段 | 类型 | 说明 |
|---|---|---|
| id | string | MongoDB 配置 ID；新增时不传，更新时必传 |
| manufacturerMetaId | string | 工厂元数据 ID |
| warehouseId | string | 旺店通仓库 ID |
| logisticsId | string | 旺店通物流 ID |
| logisticsName | string | 物流名称 |
| presetType | string | MES 订单物流预设类型；预下单时与 `orderInfo.logisticsCarrierInfo.presetType` 匹配 |
| createTime | string | 创建时间，格式为 `yyyy-MM-dd HH:mm:ss` |
| updateTime | string | 更新时间，格式为 `yyyy-MM-dd HH:mm:ss` |

除 `id` 和时间字段外，请求中的所有配置字段均不能为空。参数校验失败时返回 HTTP/业务参数校验错误，消息为对应的“字段不能为空”。

### 通用响应

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1785000000000
}
```

## 1. 新增配置

- **URL**：`POST /api/manufacturerSide/wdt/add`
- **Content-Type**：`application/json`
- **返回类型**：`ApiResponse<WdtConfig>`

### 请求示例

```json
{
  "manufacturerMetaId": "MFR_10001",
  "warehouseId": "3",
  "logisticsId": "2848610592363643041",
  "logisticsName": "极兔速递",
  "presetType": "JITU"
}
```

成功后，`data` 返回包含数据库 `id` 和时间字段的完整配置。

## 2. 更新配置

- **URL**：`POST /api/manufacturerSide/wdt/update`
- **Content-Type**：`application/json`
- **返回类型**：`ApiResponse<String>`

### 请求示例

```json
{
  "id": "6884a3406f9e020001234567",
  "manufacturerMetaId": "MFR_10001",
  "warehouseId": "5",
  "logisticsId": "2848610592363643041",
  "logisticsName": "极兔速递",
  "presetType": "JITU"
}
```

更新时 `id` 必填；成功时 `data` 为 `"success"`。

## 3. 删除配置

- **URL**：`DELETE /api/manufacturerSide/wdt/{id}`
- **返回类型**：`ApiResponse<String>`

示例：

```http
DELETE /api/manufacturerSide/wdt/6884a3406f9e020001234567
```

配置存在时执行删除；配置不存在时接口仍按幂等删除返回成功。

## 4. 查询配置详情

- **URL**：`GET /api/manufacturerSide/wdt/{id}`
- **返回类型**：`ApiResponse<WdtConfig>`

示例：

```http
GET /api/manufacturerSide/wdt/6884a3406f9e020001234567
```

## 5. 分页查询配置

- **URL**：`POST /api/manufacturerSide/wdt/list`
- **Content-Type**：`application/json`
- **返回类型**：`ApiResponse<PagedResult<WdtConfig>>`

### 请求体参数

| 参数 | 类型 | 必填 | 默认值 | 校验规则 |
|---|---|---:|---:|---|
| current | integer | 否 | 1 | 大于等于 1 |
| size | integer | 否 | 20 | 1～100 |
| manufacturerMetaId | string | 否 | - | 工厂元数据 ID，精确匹配 |
| presetType | string | 否 | - | MES 订单物流预设类型，精确匹配 |

### 请求示例

```json
{
  "current": 1,
  "size": 20,
  "manufacturerMetaId": "MFR_10001",
  "presetType": "JITU"
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "6884a3406f9e020001234567",
        "manufacturerMetaId": "MFR_10001",
        "warehouseId": "3",
        "logisticsId": "2848610592363643041",
        "logisticsName": "极兔速递",
        "presetType": "JITU",
        "createTime": "2026-07-25 16:30:00",
        "updateTime": "2026-07-25 16:30:00"
      }
    ],
    "total": 1,
    "size": 20,
    "current": 1
  },
  "timestamp": 1785000000000
}
```

> `PagedResult` 的列表字段名以共享组件的实际序列化结果为准。

## 与预下单打印的关系

聚单平台订单执行旺店通预下单时，系统使用订单的 `manufacturerId` 和 `logisticsCarrierInfo.presetType` 精确查询一条配置。未匹配到配置时跳过本次打印；匹配成功后，系统使用配置中的仓库及物流信息执行换仓和面单打印。
