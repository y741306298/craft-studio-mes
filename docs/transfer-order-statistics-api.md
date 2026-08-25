# 转单统计接口与统计口径文档

## 1. 功能概述

转单统计用于查询指定时间范围内，某个源工厂转向某个目标工厂的：

- 转单目标订单项目明细；
- 转单订单数；
- 转单订单总金额。

统计数据保存在独立的 MongoDB 集合 `transferDailyStatistics` 中，不写入普通订单统计集合
`orderDailyStatistics`。

## 2. 查询接口

- **URL**：`POST /api/manufacturerSide/statistics/transfer/list`
- **Controller**：`StatisticsController#listTransferOrderStatistics`
- **返回类型**：`PagedApiResponse<TransferOrderItemVO>`
- **日期时区**：`Asia/Shanghai`

### 2.1 请求参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `current` | integer | 是 | 当前页，从 1 开始 |
| `size` | integer | 是 | 每页条数，范围为 1-100 |
| `sourceId` | string | 否 | 源工厂 `manufacturerMetaId`，对应转单记录 `sourceId` |
| `targetId` | string | 否 | 目标工厂 `manufacturerMetaId`，对应转单记录 `targetId` |
| `createDateStart` | string | 是 | 转单开始日期，格式 `yyyy-MM-dd`，按北京时间 `00:00:00` 查询 |
| `createDateEnd` | string | 是 | 转单结束日期，格式 `yyyy-MM-dd`，按北京时间 `23:59:59` 查询 |

请求示例：

```json
{
  "current": 1,
  "size": 20,
  "sourceId": "MFR_SOURCE_001",
  "targetId": "MFR_TARGET_001",
  "createDateStart": "2026-08-01",
  "createDateEnd": "2026-08-31"
}
```

工厂条件组合规则：

- 同时传 `sourceId`、`targetId`：查询指定的源工厂到目标工厂流向；
- 只传 `sourceId`：查询该源工厂在日期范围内转向所有目标工厂的转出数据；
- 只传 `targetId`：查询日期范围内所有源工厂转入该目标工厂的数据；
- 两者都不传：查询日期范围内全部工厂流向。

### 2.2 返回参数

返回分页结构与订单列表 `listOrders` 一致。

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | integer | 业务状态码，成功为 200 |
| `message` | string | 响应消息 |
| `data.items` | array | 当前页 `TransferOrderItemVO` 转单项目明细 |
| `data.items[].sourceId` | string | 源工厂 ID |
| `data.items[].sourceName` | string | 源工厂名称快照 |
| `data.items[].targetId` | string | 目标工厂 ID |
| `data.items[].targetName` | string | 目标工厂名称快照 |
| `data.current` | integer | 当前页 |
| `data.size` | integer | 每页条数 |
| `data.total` | integer | 符合条件的目标订单项目总数，不是转单订单数 |
| `data.totalOrderCount` | integer | 日期范围内源工厂转向目标工厂的转单次数/订单数 |
| `data.totalArea` | number | 转单统计不统计面积，固定返回 `0.00` |
| `data.totalAmount` | number | 日期范围内的转单订单金额总和，保留两位小数 |
| `timestamp` | integer | 响应时间戳 |

返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "orderId": "ORD_10001",
        "orderItemId": "OI_TARGET_10001",
        "manufacturerId": "MFR_TARGET_001",
        "sourceId": "MFR_SOURCE_001",
        "sourceName": "源工厂",
        "targetId": "MFR_TARGET_001",
        "targetName": "目标工厂",
        "quantity": 2,
        "paymentPrice": 188.50
      }
    ],
    "current": 1,
    "size": 20,
    "total": 1,
    "totalOrderCount": 1,
    "totalArea": 0.00,
    "totalAmount": 188.50
  },
  "timestamp": 1788172800000
}
```

## 3. 统计保存规则

调用订单转单接口并完成转单记录保存时，系统按下列维度累加统计：

```text
statisticsDate + sourceId + targetId
```

每次转单的增量为：

```text
totalOrderCount += 1
totalAmount += orderInfo.price.paymentPrice
```

当 `orderInfo.price` 或 `paymentPrice` 为空时，本次金额按 `0.00` 处理。统计日期使用转单发生时的
北京时间日期，而不是原订单创建日期。

例如，同一天发生三次从工厂 A 到工厂 B 的转单，订单实付金额分别为 100、200 和 300，则统计结果为：

```json
{
  "sourceId": "A",
  "targetId": "B",
  "statisticsDate": "2026-08-25",
  "totalOrderCount": 3,
  "totalAmount": 600.00
}
```

## 4. 数据模型

### 4.1 `transferDailyStatistics`

| 字段 | 类型 | 说明 |
|---|---|---|
| `sourceId` | string | 源工厂 ID |
| `targetId` | string | 目标工厂 ID |
| `targetName` | string | 目标工厂名称快照 |
| `statisticsDate` | date | 统计日期 |
| `totalOrderCount` | long | 当日该流向的转单订单数 |
| `totalAmount` | decimal | 当日该流向的转单订单总金额 |
| `createTime` | date | 统计记录创建时间 |
| `updateTime` | date | 最后更新时间 |

集合具有唯一联合索引：

```text
sourceId + targetId + statisticsDate
```

写入使用 MongoDB 原子 `$inc` 和 `upsert`，同一天同一工厂流向的多次转单会累加到同一条记录。

### 4.2 `orderTransferRecord.targetOrderItemId`

转单记录同时保存：

- `orderItemId`：源工厂原订单项目 ID；
- `targetOrderItemId`：目标工厂新生成的订单项目 ID。

查询明细时优先使用 `targetOrderItemId` 精确查询目标项目。历史转单记录没有该字段时，兼容使用
`orderId + targetId` 查询目标工厂订单项目。

## 5. 查询与汇总口径

### 5.1 项目明细

1. 按 `sourceId`、`targetId` 和转单记录 `createTime` 查询 `orderTransferRecord`。
2. 新记录按 `targetOrderItemId` 定位目标订单项目。
3. 历史记录缺少 `targetOrderItemId` 时，按转单记录的 `orderId` 查询目标工厂项目。
4. 项目明细按请求的 `current`、`size` 分页。
5. `data.total` 为目标订单项目数量，因此一次转单包含多个项目时，可能大于 `totalOrderCount`。

### 5.2 订单数与金额

统计汇总直接读取 `transferDailyStatistics`。日期始终必传，工厂条件按请求中实际提供的字段动态加入：

```text
statisticsDate between createDateStart and createDateEnd
[sourceId = 请求 sourceId]
[targetId = 请求 targetId]
```

其中：

- `totalOrderCount = sum(totalOrderCount)`；
- `totalAmount = sum(totalAmount)`；
- 没有统计记录时，两项均返回 0；
- 汇总值覆盖完整查询日期范围，不受明细分页影响。

## 6. 参数错误

| 场景 | 错误说明 |
|---|---|
| `createDateStart` 为空 | `开始日期不能为空` |
| `createDateEnd` 为空 | `结束日期不能为空` |
| 日期格式不是 `yyyy-MM-dd` | `开始日期格式错误，应为 yyyy-MM-dd` 或 `结束日期格式错误，应为 yyyy-MM-dd` |
| 开始日期晚于结束日期 | `开始日期不能晚于结束日期` |
| `current <= 0` | 分页参数错误 |
| `size <= 0` 或 `size > 100` | `每页大小必须在 1-100 之间` |

## 7. 历史数据说明

- 新统计只会在本功能上线后调用 `transferOrder` 时写入。
- 上线前已经存在的 `orderTransferRecord` 不会自动生成对应的 `transferDailyStatistics`。
- 因此，查询历史日期时可能可以查到项目明细，但 `totalOrderCount`、`totalAmount` 为 0 或小于历史实际值。
- 若要求历史统计完整，需要另行执行一次历史统计回填；回填时应按转单事件去重，避免同一订单的多条项目记录被重复计算。
