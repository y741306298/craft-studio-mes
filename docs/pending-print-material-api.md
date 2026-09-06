# 打印列表材料筛选与可打印材料查询接口文档

## 1. 变更说明

本次打印模块接口变更包含以下内容：

1. 修改现有待打印印版分页接口 `POST /api/manufacturerSide/print/pending/list`：新增 `materialId` 精确筛选条件。
2. 新增可打印材料查询接口 `POST /api/manufacturerSide/print/pending/material/list`：按制造商、设备和创建时间查询“待打印”或“打印中”印版使用的材料，返回去重后的材料 ID 与材料名称。

---

## 2. 待打印印版分页查询（已修改）

### 2.1 基本信息

- **URL**：`POST /api/manufacturerSide/print/pending/list`
- **Content-Type**：`application/json`
- **返回类型**：`ApiResponse<PagedResult<PendingPrintTypesettingVO>>`
- **是否分页**：是

### 2.2 变更内容

请求体新增可选字段 `materialId`。传入该字段时，后端按
`materialConfig.materialId` 进行精确匹配；该条件是在现有查询条件基础上增加的，
可以与设备、印版业务 ID、创建时间、状态及分页条件组合使用。

### 2.3 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `manufacturerMetaId` | string | 是 | 制造商元数据 ID；不允许为空 |
| `id` | string | 否 | 设备配置 ID。后端根据该 ID 获取 `deviceCode` 并筛选印版 |
| `typesettingId` | string | 否 | 印版业务 ID，忽略大小写并支持包含匹配 |
| `materialId` | string | 否 | **新增字段**；材料 ID，去除首尾空白后精确匹配 `materialConfig.materialId` |
| `startTime` | string | 否 | 印版创建时间起点，包含边界；建议使用 ISO-8601 格式 |
| `endTime` | string | 否 | 印版创建时间终点，包含边界；建议使用 ISO-8601 格式 |
| `status` | string | 否 | 可传 `printing`、`printing_in_progress` 或 `completed`；不传时合并查询 `printing` 与 `printing_in_progress` |
| `current` | integer | 是 | 页码，从 `1` 开始；小于 `1` 时后端按 `1` 处理 |
| `size` | integer | 是 | 每页条数，有效范围为 `1`～`100`；无效时后端按 `20` 处理 |

### 2.4 状态编码

| 状态编码 | 中文含义 |
|---|---|
| `printing` | 待打印 |
| `printing_in_progress` | 打印中 |
| `completed` | 已完成 |

### 2.5 请求示例

```json
{
  "manufacturerMetaId": "MFR_10001",
  "id": "DEVICE_CFG_10001",
  "typesettingId": "TS_202609",
  "materialId": "MATERIAL_10001",
  "startTime": "2026-09-01T00:00:00Z",
  "endTime": "2026-09-05T23:59:59Z",
  "status": "printing",
  "current": 1,
  "size": 20
}
```

### 2.6 成功响应字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | number | 业务状态码，成功时为 `200` |
| `message` | string | 响应消息 |
| `data.items` | array | 当前页印版列表，元素结构为 `PendingPrintTypesettingVO` |
| `data.total` | number | 所有符合条件的印版数量 |
| `data.size` | number | 当前页实际返回条数 |
| `data.current` | number | 当前页码 |
| `data.items[].id` | string | 印版记录主键 |
| `data.items[].typesettingId` | string | 印版业务 ID |
| `data.items[].status` | string | 印版状态编码 |
| `data.items[].materialConfig.materialId` | string | 材料 ID |
| `data.items[].materialConfig.materialSnapshot.name` | string | 材料名称快照 |
| `data.items[].deviceCode` | string | 设备编码 |
| `data.items[].deviceName` | string | 设备名称 |
| `data.items[].jsonfile` | string/null | JSON 文件名 |
| `timestamp` | number | 响应时间戳（以实际公共响应结构为准） |

除上述关键字段外，`data.items[]` 继续返回现有 `PendingPrintTypesettingVO` 中的其他印版字段。

### 2.7 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "68ba00000000000000000001",
        "typesettingId": "TS_20260905001",
        "status": "printing",
        "materialConfig": {
          "materialId": "MATERIAL_10001",
          "materialSnapshot": {
            "name": "铜版纸"
          }
        },
        "deviceCode": "PRINTER_01",
        "deviceName": "一号打印机",
        "jsonfile": "TS_20260905001.json"
      }
    ],
    "total": 1,
    "size": 1,
    "current": 1
  },
  "timestamp": 1788595200000
}
```

### 2.8 常见错误

| 场景 | 响应/异常说明 |
|---|---|
| `manufacturerMetaId` 为空 | 参数校验失败，提示 `manufacturerMetaId不能为空` 或 `manufacturerMetaId 不能为空` |
| `status` 不在允许范围内 | 返回参数错误，提示 `status 仅支持 printing / printing_in_progress / completed` |

---

## 3. 查询可打印材料列表（新增）

### 3.1 基本信息

- **URL**：`POST /api/manufacturerSide/print/pending/material/list`
- **Content-Type**：`application/json`
- **返回类型**：`ApiResponse<List<PendingPrintMaterialVO>>`
- **是否分页**：否

### 3.2 业务规则

1. 仅查询当前状态为以下两种之一的印版：
   - `printing`（待打印）；
   - `printing_in_progress`（打印中）。
2. 使用 `manufacturerMetaId` 限定制造商范围。
3. `id` 有值时，先根据设备配置 ID 获取设备编码，再使用 `deviceCode` 筛选印版。
4. `startTime`、`endTime` 按印版 `createTime` 筛选，且均包含边界。
5. 数据库只读取以下业务字段：
   - `materialConfig.materialId`；
   - `materialConfig.materialSnapshot.name`。
6. 响应将上述字段分别映射为 `materialId`、`materialName`。
7. `materialId` 为空的记录不会返回；相同 `materialId` 只返回一次，并保留首次查询到的材料名称。
8. 接口返回全部符合条件的材料，不接收分页参数。

### 3.3 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `manufacturerMetaId` | string | 是 | 制造商元数据 ID；不允许为空 |
| `id` | string | 否 | 设备配置 ID，用于换取 `deviceCode` 并筛选印版 |
| `startTime` | string | 否 | 印版创建时间起点，包含边界；建议使用 ISO-8601 格式 |
| `endTime` | string | 否 | 印版创建时间终点，包含边界；建议使用 ISO-8601 格式 |

> 本接口的状态由服务端固定为“待打印”和“打印中”，请求体不接收 `status` 字段。

### 3.4 请求示例

```json
{
  "manufacturerMetaId": "MFR_10001",
  "id": "DEVICE_CFG_10001",
  "startTime": "2026-09-01T00:00:00Z",
  "endTime": "2026-09-05T23:59:59Z"
}
```

### 3.5 成功响应字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | number | 业务状态码，成功时为 `200` |
| `message` | string | 响应消息 |
| `data` | array | 去重后的材料列表；没有符合条件的数据时返回空数组 |
| `data[].materialId` | string | `materialConfig.materialId` |
| `data[].materialName` | string/null | `materialConfig.materialSnapshot.name`；印版没有名称快照时可能为空 |
| `timestamp` | number | 响应时间戳（以实际公共响应结构为准） |

### 3.6 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "materialId": "MATERIAL_10001",
      "materialName": "铜版纸"
    },
    {
      "materialId": "MATERIAL_10002",
      "materialName": "牛皮纸"
    }
  ],
  "timestamp": 1788595200000
}
```

### 3.7 空结果示例

```json
{
  "code": 200,
  "message": "success",
  "data": [],
  "timestamp": 1788595200000
}
```

### 3.8 常见错误

| 场景 | 响应/异常说明 |
|---|---|
| `manufacturerMetaId` 为空 | 参数校验失败，提示 `manufacturerMetaId不能为空` 或 `manufacturerMetaId 不能为空` |

---

## 4. 联动调用建议

打印页面可按以下顺序调用接口：

1. 页面选择设备或时间范围后，调用 `/pending/material/list` 获取当前范围内可选材料。
2. 用户选择材料后，将返回的 `materialId` 传给 `/pending/list`。
3. `/pending/list` 返回同时符合设备、时间、状态、印版 ID 和材料 ID 条件的分页印版数据。

如果用户未选择材料，则调用 `/pending/list` 时不传 `materialId`，接口行为与本次修改前保持一致。
