# 平台价格配置与平台列表接口文档

本文档描述本次新增的 3 个接口：配置平台成品商品规格价格、配置平台工艺价格、获取工厂所属平台列表。

> 说明：当前 MES 接口层通过 `HttpProxy` 透传到 product-core 内部接口。为兼容 configSide 与 manufacturerSide，两侧均提供相同能力的入口。

---

## 1. 配置平台成品商品规格价格

- **说明**：为指定平台配置指定成品商品规格的价格。
- **Method**：`POST`
- **MES 入口 URL**：
  - `POST /api/configSide/mtsProductCfg/price/mtsProductSpec`
  - `POST /api/manufacturerSide/mtsProductCfg/price/mtsProductSpec`
- **转发目标 URL**：`POST /api/internal/mes/platform/cfg/price/mtsProductSpec`

### 请求字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| rmfId | string | 是 | 工厂/资源制造方 ID |
| platformId | string | 是 | 平台 ID |
| mtsProductSpecId | string | 是 | 成品商品规格 ID |
| priceConfig | object | 是 | 价格配置 |
| priceConfig.unitPrice | object | 是 | 单价配置 |
| priceConfig.unitPrice.price | number | 是 | 单价金额 |
| priceConfig.unitPrice.unitType | string | 是 | 单价单位类型，例如：`AREA` |
| priceConfig.floorPrice | object | 是 | 底价配置 |
| priceConfig.floorPrice.price | number | 是 | 底价金额 |

### 请求示例

```json
{
  "rmfId": "69f956c00ff1ad90a9611464",
  "platformId": "69f858ae0ff1ad90a9611340",
  "mtsProductSpecId": "6a01c1bff3e7bbb0ddd0dbe3",
  "priceConfig": {
    "unitPrice": {
      "price": 30,
      "unitType": "AREA"
    },
    "floorPrice": {
      "price": 40
    }
  }
}
```

### 响应示例

```json
{
  "code": 200,
  "data": "success",
  "message": "success",
  "timestamp": 1774675727782
}
```

---

## 2. 配置平台工艺价格

- **说明**：为指定平台配置指定工艺定义的价格。
- **Method**：`POST`
- **MES 入口 URL**：
  - `POST /api/configSide/processCfg/price/process`
  - `POST /api/manufacturerSide/processCfg/price/process`
- **转发目标 URL**：`POST /api/internal/mes/platform/cfg/price/process`

### 请求字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| rmfId | string | 是 | 工厂/资源制造方 ID |
| platformId | string | 是 | 平台 ID |
| processMetaId | string | 是 | 工艺定义 ID |
| priceConfig | object | 是 | 价格配置 |
| priceConfig.unitPrice | object | 是 | 单价配置 |
| priceConfig.unitPrice.price | number | 是 | 单价金额 |
| priceConfig.unitPrice.unitType | string | 是 | 单价单位类型，例如：`ONE` |
| priceConfig.floorPrice | object | 是 | 底价配置 |
| priceConfig.floorPrice.price | number | 是 | 底价金额 |

### 请求示例

```json
{
  "rmfId": "69f956c00ff1ad90a9611464",
  "platformId": "69f858ae0ff1ad90a9611340",
  "processMetaId": "69f090a709c2201320269754",
  "priceConfig": {
    "unitPrice": {
      "price": 1,
      "unitType": "ONE"
    },
    "floorPrice": {
      "price": 2
    }
  }
}
```

### 响应示例

```json
{
  "code": 200,
  "data": "success",
  "message": "success",
  "timestamp": 1784169297165
}
```

---

## 3. 获取工厂所属平台列表

- **说明**：按工厂 ID 分页查询该工厂所属平台列表。
- **Method**：`POST`
- **MES 入口 URL**：
  - `POST /api/configSide/platform/pageListPlatformsByMfId`
  - `POST /api/manufacturerSide/platform/pageListPlatformsByMfId`
- **转发目标 URL**：`POST /api/internal/mes/platform/pageListPlatformsByMfId`

### 请求字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| mfId | string | 是 | 工厂 ID |
| current | number | 是 | 当前页码，从 1 开始 |
| size | number | 是 | 每页条数 |

### 请求示例

```json
{
  "mfId": "69f95b080ff1ad90a9611468",
  "current": 1,
  "size": 10
}
```

### 响应 data 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| current | number | 当前页码 |
| size | number | 每页条数 |
| total | number | 总条数 |
| items | array[object] | 平台列表 |
| items[].id | string | 平台 ID |
| items[].code | string | 平台编码 |
| items[].name | string | 平台名称 |

### 响应示例

```json
{
  "code": 200,
  "data": {
    "current": 1,
    "size": 10,
    "total": 2,
    "items": [
      {
        "id": "69f839920ff1ad90a961133f",
        "code": "PF-1",
        "name": "常德睿图科技"
      },
      {
        "id": "69f858ae0ff1ad90a9611340",
        "code": "PF-2",
        "name": "艺联POD"
      }
    ]
  },
  "message": "success",
  "timestamp": 1784168776918
}
```
