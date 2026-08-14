# StatisticsController 订单统计接口文档（V2）

本文档描述厂商端订单统计的两个接口：订单统计分页查询和统计维度筛选项查询。

## 1. 通用约定

- Base Path：`/api/manufacturerSide/statistics`
- 请求格式：`application/json`
- 日期格式：`yyyy-MM-dd`
- 日期边界按北京时间（`Asia/Shanghai`）解释。
- 成功响应通用字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | integer | 成功时为 `200` |
| `message` | string | 成功时为 `success` |
| `data` | object | 业务数据 |
| `timestamp` | integer | 服务端响应时间戳，单位毫秒 |

## 2. 统计维度定义

`orderDailyStatistics` 使用以下三个维度保存统计数据，每条维度同时保存 `indexId` 和用于展示的 `indexName`：

| 维度 | type | indexId 来源 | indexName 来源 |
| --- | --- | --- | --- |
| 企业 | `ENTERPRISE` | `orderItem.orgInfo.name` | `orderItem.orgInfo.name` |
| 材料 | `MATERIAL` | `orderItem.material.materialId` | `orderItem.material.materialSnapshot.name` |
| 路线 | `ROUTE` | `orderItem.routeId` | `deliveryRoute.routeName` |

## 3. 获取统计筛选项

获取指定工厂、指定日期范围内出现过的企业、材料和路线，分别按维度 ID 去重后返回 `id/name` 集合。

### 3.1 请求

```http
POST /api/manufacturerSide/statistics/order/filters
Content-Type: application/json
```

```json
{
  "manufacturerId": "69f956c00ff1ad90a9611464",
  "createDateStart": "2026-06-01",
  "createDateEnd": "2026-06-30"
}
```

### 3.2 请求字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `manufacturerId` | string | 是 | 工厂 `manufacturerMetaId` |
| `createDateStart` | string | 是 | 统计开始日期，格式 `yyyy-MM-dd`，包含当天 |
| `createDateEnd` | string | 是 | 统计结束日期，格式 `yyyy-MM-dd`，包含当天 |

筛选项接口使用独立的非分页请求 DTO，不接收 `current`、`size` 或订单明细筛选字段；服务端会一次性查询并返回日期范围内的全部去重维度。

### 3.3 响应字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `data.enterprises` | array | 企业筛选项 |
| `data.enterprises[].id` | string | 企业维度 ID，即企业名称 |
| `data.enterprises[].name` | string | 企业展示名称 |
| `data.materials` | array | 材料筛选项 |
| `data.materials[].id` | string | 材料 ID |
| `data.materials[].name` | string/null | 材料快照名称；历史数据可能为空 |
| `data.routes` | array | 路线筛选项 |
| `data.routes[].id` | string | 路线 ID |
| `data.routes[].name` | string/null | 路线名称；未匹配到路线快照时可能为空 |

### 3.4 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "enterprises": [
      { "id": "测试企业", "name": "测试企业" }
    ],
    "materials": [
      { "id": "6a06ae72722cf613cc8b409f", "name": "户外PP背胶" },
      { "id": "6a119c66639151d29cf218c6", "name": "5.2m黑白布" }
    ],
    "routes": [
      { "id": "ROUTE_001", "name": "常德城区路线" }
    ]
  },
  "timestamp": 1786629600000
}
```

### 3.5 去重规则

- 服务端读取日期范围内该工厂的全部 `orderDailyStatistics`。
- 先按 `type` 分成 `enterprises`、`materials`、`routes` 三组。
- 每组按 `indexId` 去重，并保留该 ID 首次出现记录的 `indexName`。
- `type` 或 `indexId` 为空的历史统计记录不会进入筛选项结果。

## 4. 分页查询订单统计

接口先从 `orderDailyStatistics` 查询日期范围及选定维度的汇总数据，再分页查询符合明细条件的订单项，并组合成响应。

### 4.1 请求

```http
POST /api/manufacturerSide/statistics/order/list
Content-Type: application/json
```

```json
{
  "current": 1,
  "size": 20,
  "manufacturerId": "69f956c00ff1ad90a9611464",
  "createDateStart": "2026-06-01",
  "createDateEnd": "2026-06-30",
  "materialId": "6a06ae72722cf613cc8b409f"
}
```

### 4.2 请求字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | integer | 是 | 当前页，从 `1` 开始 |
| `size` | integer | 是 | 每页数量，范围 `1-100` |
| `manufacturerId` | string | 汇总时是 | 工厂标识；匹配订单项 `manufacturerId`，并用于查询日统计 |
| `orderId` | string | 否 | 订单号模糊匹配 |
| `status` | string | 否 | 订单状态码或 `OrderStatus` 枚举名 |
| `routeId` | string | 否 | 精确匹配订单项 `routeId`，并选择路线统计维度 |
| `createDateStart` | string | 汇总时是 | 开始日期；明细从北京时间当天 `00:00:00` 开始 |
| `createDateEnd` | string | 汇总时是 | 结束日期；明细截至北京时间当天 `23:59:59` |
| `materialId` | string | 否 | 精确匹配 `material.materialId`，并选择材料统计维度 |
| `materialName` | string | 否 | 模糊匹配 `material.materialSnapshot.name`；仅作用于明细分页 |
| `materialType` | string | 否 | 精确匹配 `material.materialType`；仅作用于明细分页 |
| `orgName` | string | 否 | 精确匹配订单项 `orgInfo.name`，并选择企业统计维度 |

### 4.3 汇总维度选择规则

一次请求只会使用一个维度查询 `orderDailyStatistics`，优先级如下：

1. 传入 `materialId`：按 `MATERIAL + materialId` 汇总。
2. 否则传入 `routeId`：按 `ROUTE + routeId` 汇总。
3. 否则传入 `orgName`：按 `ENTERPRISE + orgName` 汇总。
4. 三者均未传：汇总日期范围内全部 `ENTERPRISE` 数据。

> 建议一次只传 `materialId`、`routeId`、`orgName` 中的一个。若同时传入，订单项明细会同时应用所有条件，但汇总数据只采用上述最高优先级维度，可能造成明细和汇总口径不一致。

### 4.4 响应字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `data.items` | array[`OrderItemVO`] | 当前页完整订单项数据；同一订单有多个匹配订单项时，`orderId` 可能重复 |
| `data.items[].id` | string | 订单项 Mongo ID |
| `data.items[].orderItemId` | string | 订单项业务 ID |
| `data.items[].orderId` | string | 订单号 |
| `data.items[].manufacturerId` | string | 当前订单项所属工厂 |
| `data.items[].routeId` | string/null | 配送路线 ID |
| `data.items[].routeName` | string/null | `routeId` 对应的配送路线名称 |
| `data.items[].mtoProduct` | object/null | 完整定制产品规格 |
| `data.items[].material` | object/null | 材料配置和材料快照 |
| `data.items[].procedureFlow` | object/null | 生产工序流 |
| `data.items[].quantity` | integer/null | 数量 |
| `data.items[].status` | string/null | 订单项状态 |
| `data.items[].productionImgFile` | object/null | 生产图文件 |
| `data.items[].productionPieces` | array/null | 生产工件列表 |
| `data.items[].price` | object/null | 订单项原始价、实际价 |
| `data.items[].customer` | object/null | 所属订单的客户信息 |
| `data.items[].remark` | string/null | 所属订单备注 |
| `data.items[].orgInfo` | object/null | 下单企业信息 |
| `data.items[].paymentPrice` | number | 所属订单的 `price.paymentPrice`，保留两位小数 |
| `data.items[].orderItemPrice` | number | 实际参与日统计的价格：优先使用完整底价清单之和，否则使用快照支付价 |
| `data.items[].createTime` | string/null | 订单项创建时间 |
| `data.items[].updateTime` | string/null | 订单项更新时间 |
| `data.current` | integer | 当前页 |
| `data.size` | integer | 每页数量 |
| `data.total` | integer | 符合明细条件的订单项总数 |
| `data.totalOrderCount` | integer | `orderDailyStatistics` 聚合后的订单数；没有统计记录时为 `0` |
| `data.totalArea` | number | `orderDailyStatistics` 聚合后的面积，保留两位小数 |
| `data.totalAmount` | number | `orderDailyStatistics` 聚合后的金额，保留两位小数 |
| `data.statusList` | array | 全部订单状态筛选项 |
| `data.statusList[].code` | string | 状态码 |
| `data.statusList[].description` | string | 状态说明 |
| `data.materialList` | array | 当前版本固定为空；材料筛选项请调用 `/order/filters` |
| `data.orgNameList` | array | 当前版本固定为空；企业筛选项请调用 `/order/filters` |

### 4.5 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "mongo-order-item-id",
        "orderItemId": "2070082974475329537",
        "orderId": "2070082974454358018",
        "manufacturerId": "69f956c00ff1ad90a9611464",
        "routeId": "ROUTE_001",
        "routeName": "常德城区路线",
        "quantity": 1,
        "status": "PENDING",
        "material": {
          "materialId": "6a06ae72722cf613cc8b409f",
          "materialSnapshot": { "name": "户外PP背胶" }
        },
        "price": { "originalPrice": 1.08, "actualPrice": 1.08 },
        "orgInfo": { "name": "测试企业" },
        "paymentPrice": 9.08,
        "orderItemPrice": 1.00,
        "createTime": "2026-06-25T09:53:36.000+00:00"
      }
    ],
    "current": 1,
    "size": 20,
    "total": 1,
    "totalOrderCount": 1,
    "totalArea": 0.54,
    "totalAmount": 1.00,
    "materialList": [],
    "statusList": [
      { "code": "PENDING", "description": "待处理" },
      { "code": "RETURNED", "description": "已退单" }
    ],
    "orgNameList": []
  },
  "timestamp": 1786629600000
}
```

### 4.6 汇总金额口径

日统计金额由订单创建、转单和取消订单流程维护：

- 列表页通过订单号集合一次性批量查询订单快照，不会为每条订单项逐个查询订单，避免 N+1 查询。
- `floorPriceEffectManifest.floorPriceEffectItems` 非空，且每一项均存在 `floorPrice` 时，金额为全部 `floorPrice` 之和。
- 清单不存在、为空，或任意一项缺少 `floorPrice` 时，使用工厂快照 `manufacturerInfo.price.paymentPrice`。
- 历史订单没有工厂价格快照时，回退到订单 `price.paymentPrice`。

### 4.7 错误说明

| 场景 | 结果 |
| --- | --- |
| `current <= 0` | 请求校验失败 |
| `size <= 0` 或 `size > 100` | 请求校验失败，分页大小必须在 `1-100` 之间 |
| 日期不是 `yyyy-MM-dd` | 返回日期格式参数错误 |
| `/order/filters` 缺少开始或结束日期 | 返回“开始日期不能为空”或“结束日期不能为空” |
| `/order/filters` 缺少 `manufacturerId` | 返回“工厂和统计日期不能为空” |
| `status` 无法解析为订单状态 | 返回状态参数错误 |

## 5. 推荐调用流程

1. 页面初始化时调用 `/order/filters`，加载日期范围内可选企业、材料和路线。
2. 用户选择一个统计维度后调用 `/order/list`。
3. 使用 `/order/list` 的 `items/total` 展示分页明细。
4. 使用同一响应的 `totalOrderCount/totalArea/totalAmount` 展示日统计聚合值。
