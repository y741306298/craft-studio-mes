# 转单来源/目标工厂查询接口文档

## 1. 功能说明

本文档描述 `StatisticsController` 提供的两个转单工厂筛选接口：

- 根据目标工厂和转单时间查询来源工厂；
- 根据来源工厂和转单时间查询目标工厂。

两个接口均查询 MongoDB 集合 `orderTransferRecord`，返回日期范围内出现过的全部工厂，结果不分页。

## 2. 通用约定

- Base Path：`/api/manufacturerSide/statistics`
- 请求方式：`POST`
- Content-Type：`application/json`
- 日期格式：`yyyy-MM-dd`
- 日期时区：`Asia/Shanghai`
- 开始日期转换为当天 `00:00:00`，结束日期转换为当天 `23:59:59`

成功响应结构：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | integer | 业务状态码，成功时为 `200` |
| `message` | string | 响应消息，成功时为 `success` |
| `data` | array | 去重后的工厂列表；无匹配记录时为空数组 |
| `data[].manufacturerMetaId` | string | 工厂 `manufacturerMetaId` |
| `data[].name` | string/null | 转单发生时保存的工厂名称快照 |
| `timestamp` | integer | 服务端响应时间戳，单位为毫秒 |

## 3. 查询转单来源工厂

根据目标工厂 `targetId` 和日期范围查询转单记录，提取并去重其中的 `sourceId`、`sourceName`。

### 3.1 请求地址

```http
POST /api/manufacturerSide/statistics/transfer/sourceFactories
Content-Type: application/json
```

### 3.2 请求字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `targetId` | string | 是 | 目标工厂 `manufacturerMetaId`，精确匹配 `orderTransferRecord.targetId` |
| `createDateStart` | string | 是 | 转单开始日期，格式为 `yyyy-MM-dd`，包含当天 |
| `createDateEnd` | string | 是 | 转单结束日期，格式为 `yyyy-MM-dd`，包含当天 |

### 3.3 请求示例

```json
{
  "targetId": "MFR_TARGET_001",
  "createDateStart": "2026-08-01",
  "createDateEnd": "2026-08-31"
}
```

### 3.4 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "manufacturerMetaId": "MFR_SOURCE_001",
      "name": "来源工厂一"
    },
    {
      "manufacturerMetaId": "MFR_SOURCE_002",
      "name": "来源工厂二"
    }
  ],
  "timestamp": 1788172800000
}
```

## 4. 查询转单目标工厂

根据来源工厂 `sourceId` 和日期范围查询转单记录，提取并去重其中的 `targetId`、`targetName`。

### 4.1 请求地址

```http
POST /api/manufacturerSide/statistics/transfer/targetFactories
Content-Type: application/json
```

### 4.2 请求字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sourceId` | string | 是 | 来源工厂 `manufacturerMetaId`，精确匹配 `orderTransferRecord.sourceId` |
| `createDateStart` | string | 是 | 转单开始日期，格式为 `yyyy-MM-dd`，包含当天 |
| `createDateEnd` | string | 是 | 转单结束日期，格式为 `yyyy-MM-dd`，包含当天 |

### 4.3 请求示例

```json
{
  "sourceId": "MFR_SOURCE_001",
  "createDateStart": "2026-08-01",
  "createDateEnd": "2026-08-31"
}
```

### 4.4 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "manufacturerMetaId": "MFR_TARGET_001",
      "name": "目标工厂一"
    },
    {
      "manufacturerMetaId": "MFR_TARGET_002",
      "name": "目标工厂二"
    }
  ],
  "timestamp": 1788172800000
}
```

## 5. 查询及去重规则

### 5.1 来源工厂接口

1. 使用 `targetId`、`createTime >= createDateStart 00:00:00` 和
   `createTime <= createDateEnd 23:59:59` 查询全部 `orderTransferRecord`。
2. 忽略 `sourceId` 为空的历史记录。
3. 按 `sourceId` 去重，并将 `sourceId`、`sourceName` 分别映射为响应中的
   `manufacturerMetaId`、`name`。

### 5.2 目标工厂接口

1. 使用 `sourceId`、`createTime >= createDateStart 00:00:00` 和
   `createTime <= createDateEnd 23:59:59` 查询全部 `orderTransferRecord`。
2. 忽略 `targetId` 为空的历史记录。
3. 按 `targetId` 去重，并将 `targetId`、`targetName` 分别映射为响应中的
   `manufacturerMetaId`、`name`。

重复工厂保留查询结果中首次出现记录的名称快照；接口不额外查询制造商主数据，因此历史名称可能为空或与当前名称不同。

## 6. 参数校验与错误场景

| 场景 | 错误说明 |
| --- | --- |
| 来源工厂接口未传 `targetId` | 目标工厂不能为空 |
| 目标工厂接口未传 `sourceId` | 来源工厂不能为空 |
| 未传开始日期或结束日期 | 开始日期和结束日期不能为空 |
| 日期格式不是 `yyyy-MM-dd` | 开始日期或结束日期格式错误 |
| 开始日期晚于结束日期 | 开始日期不能晚于结束日期 |

错误响应遵循项目统一的 `ApiResponse` 异常响应格式，具体 `code` 和 `message` 由全局异常处理器生成。
