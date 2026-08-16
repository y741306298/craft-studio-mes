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
| `routeId` | string | 否 | 精确匹配订单项 `routeId`，并选择路线统计维度 |
| `createDateStart` | string | 汇总时是 | 开始日期；明细从北京时间当天 `00:00:00.000` 开始 |
| `createDateEnd` | string | 汇总时是 | 结束日期；当前实现的明细上界为北京时间当天 `23:59:59.000`（含），该秒内毫秒部分大于 `000` 的记录不会命中，见 4.8 |
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
| `data.items` | array | 当前页匹配的完整订单项；包含 `OrderItem` 的全部基础字段及统计接口扩展字段，同一订单有多个匹配订单项时，`orderId` 可能重复 |
| `data.items[].id` | string/null | 订单项数据库 ID |
| `data.items[].orderItemId` | string/null | 订单项业务 ID |
| `data.items[].orderId` | string | 订单号 |
| `data.items[].manufacturerId` | string/null | 订单项所属工厂 ID |
| `data.items[].mtoProduct` | object/null | 定制产品规格 |
| `data.items[].logisticsCarrierInfo` | object/null | 物流承运信息 |
| `data.items[].material` | object/null | 材料配置 |
| `data.items[].procedureFlow` | object/null | 工艺流程 |
| `data.items[].quantity` | integer/null | 数量 |
| `data.items[].status` | string/null | 订单项状态 |
| `data.items[].isUrgent` | boolean/null | 是否加急 |
| `data.items[].processingFlow` | string/null | 当前处理流程 |
| `data.items[].productionImgFile` | object/null | 生产图文件 |
| `data.items[].maskImgFile` | object/null | 蒙版图文件 |
| `data.items[].failureReason` | string/null | 失败原因 |
| `data.items[].preprocessRequestId` | string/null | 当前预处理请求 ID |
| `data.items[].kuaidiWay` | string/null | 快递方式 |
| `data.items[].kuaidiNum` | string/null | 快递单号 |
| `data.items[].channel` | object/null | 订单渠道信息 |
| `data.items[].routeId` | string/null | 配送路线 ID |
| `data.items[].routeName` | string/null | 当前订单项 `routeId` 对应的路线名称；服务端按当前页路线 ID 批量查询，路线不存在或已删除时为空 |
| `data.items[].routeNodeId` | string/null | 配送路线节点 ID |
| `data.items[].productionPieces` | array/null | 生产件集合 |
| `data.items[].price` | object/null | 订单项价格信息 |
| `data.items[].orgInfo` | object/null | 下单企业信息 |
| `data.items[].paymentPrice` | number/null | 当前订单项的 `price.actualPrice`；字段名为兼容既有响应而保留 |
| `data.items[].orderItemPrice` | number | 该订单项所属订单实际参与日统计的价格：优先使用完整底价清单之和，否则使用快照支付价 |
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
        "id": "68a000000000000000000001",
        "orderItemId": "OI_2070082974454358018_1",
        "orderId": "2070082974454358018",
        "manufacturerId": "69f956c00ff1ad90a9611464",
        "quantity": 1,
        "status": "IN_PRODUCTION",
        "isUrgent": false,
        "routeId": "ROUTE_001",
        "routeName": "常德城区路线",
        "paymentPrice": 1.08,
        "orderItemPrice": 1.00,
        "createTime": "2026-06-25 17:53:36",
        "updateTime": "2026-06-25 17:53:36"
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
| `/order/filters` 缺少 `manufacturerId`、开始日期或结束日期中的任一字段 | 请求 DTO 校验失败，返回“工厂、开始日期和结束日期不能为空” |

### 4.8 当前实现需要注意的口径差异

- `items/total` 的日期条件作用于订单项 `createTime`；`totalOrderCount/totalArea/totalAmount` 的日期条件作用于 `orderDailyStatistics.statisticsDate`。日统计写入时使用北京时间的处理当日，而不是订单项 `createTime`，因此补录、延迟处理等场景下两组数据可能不一致。
- 两个统计查询的订单项明细固定只包含 `IN_PRODUCTION`（生产中）和 `PACKAGED`（已打包），不再使用请求中的 `status` 过滤。`orderId`、`materialName` 和 `materialType` 只过滤明细，不过滤日统计汇总。`materialId`、`routeId`、`orgName` 会过滤明细并按 4.3 的优先级选择一个汇总维度。
- 当前结束日期被转换为当天 `23:59:59.000` 并使用“包含上界”查询；创建时间处于 `23:59:59.001` 至 `23:59:59.999` 的订单项会被遗漏。这是当前代码行为，不应理解为完整覆盖结束日期当天。
- `paymentPrice` 在订单项 `price` 对象不存在时由服务端返回 `0`；只有 `price` 存在但 `actualPrice` 为空时才返回 `null`。
- 未选择维度时，汇总全部 `ENTERPRISE` 统计记录。如果一张订单被写入多个不同的企业维度，该订单及其面积、金额会在无维度汇总中重复累计。

## 5. 推荐调用流程

1. 页面初始化时调用 `/order/filters`，加载日期范围内可选企业、材料和路线。
2. 用户选择一个统计维度后调用 `/order/list`。
3. 使用 `/order/list` 的 `items/total` 展示分页明细。
4. 使用同一响应的 `totalOrderCount/totalArea/totalAmount` 展示日统计聚合值。

## 6. 全量查询订单统计

该接口与 `/order/list` 使用完全相同的明细筛选、排序、路线名称补充和日统计汇总口径，区别仅在于订单项会一次性全量查询，不应用分页。

### 6.1 请求

```http
POST /api/manufacturerSide/statistics/order/listAll
Content-Type: application/json
```

```json
{
  "manufacturerId": "69f956c00ff1ad90a9611464",
  "createDateStart": "2026-06-01",
  "createDateEnd": "2026-06-30",
  "materialId": "6a06ae72722cf613cc8b409f"
}
```

### 6.2 请求字段

请求字段与 4.2 的 `/order/list` 一致，但不接收、也不需要 `current`、`size` 和 `status`。支持 `manufacturerId`、`orderId`、`routeId`、`createDateStart`、`createDateEnd`、`materialId`、`materialName`、`materialType` 和 `orgName`；订单项固定只查询“生产中”和“已打包”状态。

### 6.3 响应

响应的 `data.items`、`data.total`、`data.totalOrderCount`、`data.totalArea`、`data.totalAmount`、`data.materialList`、`data.statusList` 和 `data.orgNameList` 含义与 4.4 一致。其中：

- `data.items` 包含符合条件的全部订单项，仍按“加急优先、更新时间倒序”排列。
- `data.total` 等于本次返回的订单项总数。
- 响应不包含分页接口的 `data.current` 和 `data.size` 字段。

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "orderItemId": "OI_2070082974454358018_1",
        "orderId": "2070082974454358018",
        "manufacturerId": "69f956c00ff1ad90a9611464",
        "routeId": "ROUTE_001",
        "routeName": "常德城区路线"
      }
    ],
    "total": 1,
    "totalOrderCount": 1,
    "totalArea": 0.54,
    "totalAmount": 1.00,
    "materialList": [],
    "statusList": [],
    "orgNameList": []
  },
  "timestamp": 1786629600000
}
```
