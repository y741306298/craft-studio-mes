# 新增全量范围查询接口文档

本文档覆盖本次新增的 4 个“按 ID 范围全量查询”接口：

1. 按订单 ID 全量查询订单项：`POST /api/manufacturerSide/order/listByOrderId`
2. 按 `typesettingId` / `orderId` / `orderItemId` 全量查询待排版对象：`POST /api/manufacturerSide/typesetting/listById`
3. 按 `typesettingId` 全量查询待确认排版：`GET /api/manufacturerSide/typesetting/confirming/listByTypesettingId`
4. 按 `orderId` / `orderItemId` 全量查询待打包零件：`POST /api/manufacturerSide/deliveryPkg/listById`

> 说明：本文档中的“全量”表示接口不接收分页参数，也不返回分页结构；返回集合由后端按对应业务条件完整筛选后一次性返回。

---

## 1. 按订单 ID 全量查询订单项

### 基本信息

- **URL**: `POST /api/manufacturerSide/order/listByOrderId`
- **Content-Type**: `application/json`
- **返回类型**: `ApiResponse<List<OrderItemVO>>`
- **是否分页**: 否

### 业务规则

- 根据 `orderId` 查询该订单下全部订单项。
- 仅返回 `quantity != 0` 的订单项。
- 返回 item 结构与 `POST /api/manufacturerSide/order/list` 中的 `OrderItemVO` item 一致。
- 支持通过 `manufacturerId` 限定工厂范围。
- 返回顺序沿用订单列表逻辑：加急优先，再按更新时间倒序（由订单项仓储查询逻辑决定）。

### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| orderId | string | 是 | 订单业务 ID |
| manufacturerId | string | 否 | 工厂/厂商 ID，用于限定订单项所属工厂 |

### 请求示例

```json
{
  "orderId": "ORD_3001",
  "manufacturerId": "MFR_10001"
}
```

### 成功响应字段

| 字段 | 类型 | 说明 |
|---|---|---|
| code | number | 业务状态码，成功为 `200` |
| message | string | 响应消息，成功为 `success` |
| data | array | `OrderItemVO` 列表 |
| data[].orderItemId | string | 订单项业务 ID |
| data[].orderId | string | 订单业务 ID |
| data[].manufacturerId | string | 工厂/厂商 ID |
| data[].quantity | number | 订单项数量；本接口只返回不为 0 的记录 |
| data[].status | string/object | 订单项状态，按现有 `OrderItemVO` 序列化结果返回 |
| data[].isUrgent | boolean | 是否加急 |
| data[].customer | object | 订单客户信息，来自订单主表 |
| data[].remark | string | 订单备注，来自订单主表 |
| data[].mtoProduct | object | 产品规格信息 |
| data[].material | object | 材料配置 |
| data[].procedureFlow | object | 工序流信息 |
| timestamp | number | 响应时间戳 |

### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "665f00000000000000000001",
      "orderItemId": "OI_2001",
      "orderId": "ORD_3001",
      "manufacturerId": "MFR_10001",
      "quantity": 2,
      "status": "PENDING_PREPROCESS",
      "isUrgent": true,
      "customer": {
        "customerName": "张三",
        "customerPhone": "13800000000"
      },
      "remark": "整单备注",
      "material": {
        "materialSnapshot": {
          "name": "白卡纸"
        }
      }
    }
  ],
  "timestamp": 1781000000000
}
```

### 常见错误

| 场景 | 响应/异常 |
|---|---|
| `orderId` 为空 | 后端抛出 `订单 ID 不能为空` |

---

## 2. 按 ID 全量查询待排版对象

### 基本信息

- **URL**: `POST /api/manufacturerSide/typesetting/listById`
- **Content-Type**: `application/json`
- **返回类型**: `ApiResponse<TypesettingAndProductionPiecesResponse>`
- **是否分页**: 否

### 业务规则

- `typesettingId`、`orderId`、`orderItemId` 三个范围字段**必须且只能传一个**。
- 不改动原分页接口 `POST /api/manufacturerSide/typesetting/list` 的行为；本接口是新增的全量范围查询入口。
- 当传 `typesettingId`：
  - 查询排版数据。
  - 返回 `typesettingId` 与入参匹配的数据，也包含同一基础 ID 的 `-Mirror` 数据。
  - 只返回满足待排版条件的数据：`status = PENDING` 且 `leaveQuantity > 0`。
- 当传 `orderId`：
  - 先查询该订单下的订单项。
  - 再按每个 `orderItemId` 查询生产工件。
  - 只返回“待排版”节点数量大于 0 的生产工件。
- 当传 `orderItemId`：
  - 直接查询该订单项下生产工件。
  - 只返回“待排版”节点数量大于 0 的生产工件。
- 返回结构与原 `listTypesettingAndProductionPieces` 一致：`list`、`total`、`current`、`processingFlowList`、`materialList`、`sourceType`。
- 因为本接口不分页，`total = list.size()`，`current = 1`。
- 返回前会和原排版列表接口一样清理工序流中的预处理类节点（如“预处理”“待排版”“排版中”“待打包”“已打包”）。

### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| manufacturerMetaId | string | 是 | 工厂元数据 ID |
| typesettingId | string | 条件必填之一 | 排版业务 ID；传入基础 ID 时会同时匹配 `基础ID-Mirror` |
| orderId | string | 条件必填之一 | 订单业务 ID；后端先展开为订单项再查生产工件 |
| orderItemId | string | 条件必填之一 | 订单项业务 ID；直接查生产工件 |
| materialName | string | 否 | 材料名称，模糊匹配 `materialConfig.materialSnapshot.name` |
| processingName | string | 否 | 工序名称，匹配 `procedureFlow.nodes.nodeName` |
| startTime | string | 否 | 创建时间起，ISO-8601 时间 |
| endTime | string | 否 | 创建时间止，ISO-8601 时间 |
| eCommerceMmodel | boolean | 否 | 电商模式；查询生产工件时为 `true` 则按订单 ID 设置 `groupId` |

> 注意：`typesettingId` / `orderId` / `orderItemId` 不允许同时出现，也不允许全部为空。

### 请求示例：按 typesettingId 查询

```json
{
  "manufacturerMetaId": "MFR_10001",
  "typesettingId": "TS_20260609001"
}
```

### 请求示例：按 orderId 查询

```json
{
  "manufacturerMetaId": "MFR_10001",
  "orderId": "ORD_3001",
  "materialName": "白卡纸",
  "processingName": "覆膜",
  "eCommerceMmodel": true
}
```

### 请求示例：按 orderItemId 查询

```json
{
  "manufacturerMetaId": "MFR_10001",
  "orderItemId": "OI_2001"
}
```

### 成功响应字段

| 字段 | 类型 | 说明 |
|---|---|---|
| code | number | 业务状态码，成功为 `200` |
| message | string | 响应消息，成功为 `success` |
| data.list | array | `TypesettingProductionPieceVO` 列表 |
| data.total | number | 返回条数，即 `data.list.length` |
| data.current | number | 固定为 `1` |
| data.processingFlowList | array[string] | 根据返回结果去重得到的工序名称列表 |
| data.materialList | array[string] | 根据返回结果去重得到的材料名称列表 |
| data.sourceType | array[object] | 来源类型选项列表 |
| timestamp | number | 响应时间戳 |

### `TypesettingProductionPieceVO` 关键字段

| 字段 | 类型 | 说明 |
|---|---|---|
| id | string | 来源数据主键 |
| sourceType | string | 来源类型：生产工件或排版数据，按现有枚举编码返回 |
| sourceId | string | 来源 ID |
| groupId | string | 分组 ID；生产工件通常为 `orderItemId`，电商模式可为 `orderId`；排版数据使用完整 `typesettingId`，`-Mirror` 印版会作为独立分组返回 |
| orderItemId | string | 订单项 ID；生产工件来源时有值 |
| quantity | number | 数量 |
| leaveQuantity | number | 剩余数量；排版来源取排版剩余量，生产工件来源取待排版节点数量 |
| materialConfig | object | 材料配置 |
| procedureFlow | object | 工序流，返回前已过滤预处理类节点 |
| previewUrl | string | 预览 URL |
| status | string | 状态 |
| isUrgent | boolean | 是否加急 |
| createTime | string | 创建时间 |

### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "665f00000000000000000002",
        "sourceType": "TYPESETTING",
        "sourceId": "665f00000000000000000002",
        "groupId": "TS_20260609001",
        "quantity": 10,
        "leaveQuantity": 5,
        "status": "PENDING",
        "isUrgent": false,
        "previewUrl": "https://oss.example.com/forme.svg",
        "materialConfig": {
          "materialSnapshot": {
            "name": "白卡纸"
          }
        },
        "procedureFlow": {
          "nodes": [
            { "nodeName": "覆膜", "pieceQuantity": 5 }
          ]
        }
      }
    ],
    "total": 1,
    "current": 1,
    "processingFlowList": ["覆膜"],
    "materialList": ["白卡纸"],
    "sourceType": [
      { "code": "PART", "description": "零件" },
      { "code": "TYPESETTING", "description": "排版" }
    ]
  },
  "timestamp": 1781000000000
}
```

### 常见错误

| 场景 | 响应/异常 |
|---|---|
| `manufacturerMetaId` 为空 | 后端抛出 `manufacturerMetaId 不能为空` |
| `typesettingId` / `orderId` / `orderItemId` 全为空 | 后端抛出 `typesettingId、orderId、orderItemId 必须且只能传一个` |
| `typesettingId` / `orderId` / `orderItemId` 同时传多个 | 后端抛出 `typesettingId、orderId、orderItemId 必须且只能传一个` |

---

## 3. 按 typesettingId 全量查询待确认排版

### 基本信息

- **URL**: `GET /api/manufacturerSide/typesetting/confirming/listByTypesettingId`
- **返回类型**: `ApiResponse<List<TypesettingInfo>>`
- **是否分页**: 否

### 业务规则

- 只查询 `status = CONFIRMING` 的排版记录。
- 根据 `typesettingId` 匹配同一基础 ID 的排版记录，同时包含 `-Mirror` 记录。
- 返回前会对 `element.width`、`element.height` 做向上取整处理，与待确认列表的尺寸处理保持一致。
- 返回顺序：加急优先，再按创建时间倒序。

### Query 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| manufacturerMetaId | string | 是 | 工厂元数据 ID |
| typesettingId | string | 是 | 排版业务 ID；传基础 ID 时会匹配 `基础ID` 和 `基础ID-Mirror` |

### 请求示例

```http
GET /api/manufacturerSide/typesetting/confirming/listByTypesettingId?manufacturerMetaId=MFR_10001&typesettingId=TS_20260609001
```

### 成功响应字段

| 字段 | 类型 | 说明 |
|---|---|---|
| code | number | 业务状态码，成功为 `200` |
| message | string | 响应消息，成功为 `success` |
| data | array | `TypesettingInfo` 列表 |
| data[].typesettingId | string | 排版业务 ID，可能为基础 ID 或 `基础ID-Mirror` |
| data[].status | string | 本接口只返回 `CONFIRMING` |
| data[].element | object | 排版元素信息；`width`、`height` 已向上取整 |
| data[].isUrgent | boolean | 是否加急 |
| data[].createTime | string | 创建时间 |
| timestamp | number | 响应时间戳 |

### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "665f00000000000000000003",
      "manufacturerMetaId": "MFR_10001",
      "typesettingId": "TS_20260609001",
      "status": "CONFIRMING",
      "quantity": 10,
      "leaveQuantity": 10,
      "isUrgent": true,
      "element": {
        "width": 120,
        "height": 240,
        "formeSvg": "https://oss.example.com/forme.svg"
      }
    },
    {
      "id": "665f00000000000000000004",
      "manufacturerMetaId": "MFR_10001",
      "typesettingId": "TS_20260609001-Mirror",
      "status": "CONFIRMING",
      "quantity": 10,
      "leaveQuantity": 10,
      "isUrgent": true,
      "element": {
        "width": 120,
        "height": 240,
        "formeSvg": "https://oss.example.com/forme-mirror.svg"
      }
    }
  ],
  "timestamp": 1781000000000
}
```

### 常见错误

| 场景 | 响应/异常 |
|---|---|
| `manufacturerMetaId` 为空 | 后端抛出 `manufacturerMetaId 不能为空` |
| `typesettingId` 为空 | 后端抛出 `typesettingId 不能为空` |

---

## 4. 按 orderId / orderItemId 全量查询待打包零件

### 基本信息

- **URL**: `POST /api/manufacturerSide/deliveryPkg/listById`
- **Content-Type**: `application/json`
- **返回类型**: `ApiResponse<DeliveryPkgPiecesResponse>`
- **是否分页**: 否

### 业务规则

- 使用独立请求体 `DeliveryPkgScopedRequest`，不扩展原 `DeliveryPkgRequest`。
- `orderId`、`orderItemId` 必须且只能传一个。
- 当传 `orderId`：
  - 先查询该订单下的订单项。
  - 再按订单项查询生产工件。
- 当传 `orderItemId`：
  - 直接查询该订单项下生产工件。
- 只返回满足“待打包”节点条件的数据：`procedureFlow.nodes` 中待打包节点数量大于 0。
- 组装返回时会补充订单、客户、地址、物流承运商、材料等信息。
- 保留和原待打包列表一致的后置过滤：`customerName`、`customerPhone`、`carrierName`、`startTime`、`endTime`、`width`。
- 返回结构与原 `POST /api/manufacturerSide/deliveryPkg/list` 一致：`items`、`materialList`、`sizeList`、`processList`。

### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| manufacturerMetaId | string | 是 | 工厂元数据 ID |
| orderId | string | 条件必填之一 | 订单业务 ID；后端先展开为订单项再查生产工件 |
| orderItemId | string | 条件必填之一 | 订单项业务 ID；直接查询生产工件 |
| customerName | string | 否 | 客户姓名，模糊匹配 |
| customerPhone | string | 否 | 客户手机号，模糊匹配 |
| carrierName | string | 否 | 物流承运商名称，模糊匹配 |
| startTime | string | 否 | 创建时间起，ISO-8601 时间 |
| endTime | string | 否 | 创建时间止，ISO-8601 时间 |
| materialName | string | 否 | 材料名称，模糊匹配 `materialConfig.materialSnapshot.name` |
| processName | string | 否 | 工序名称，匹配 `procedureFlow.nodes.nodeName` |
| width | number | 否 | 零件宽度 |

> 注意：`orderId` / `orderItemId` 不允许同时出现，也不允许全部为空。

### 请求示例：按 orderId 查询

```json
{
  "manufacturerMetaId": "MFR_10001",
  "orderId": "ORD_3001",
  "customerPhone": "1380000",
  "carrierName": "顺丰"
}
```

### 请求示例：按 orderItemId 查询

```json
{
  "manufacturerMetaId": "MFR_10001",
  "orderItemId": "OI_2001",
  "materialName": "白卡纸",
  "processName": "覆膜",
  "width": 70.0
}
```

### 成功响应字段

| 字段 | 类型 | 说明 |
|---|---|---|
| code | number | 业务状态码，成功为 `200` |
| message | string | 响应消息，成功为 `success` |
| data.items | array | `DeliveryPkgPieceVO` 列表 |
| data.materialList | array[string] | 从返回 items 去重得到的材料名称列表 |
| data.sizeList | array[number] | 从返回 items 去重得到的宽度列表 |
| data.processList | array[string] | 从返回 items 去重得到的工序名称列表 |
| timestamp | number | 响应时间戳 |

### `DeliveryPkgPieceVO` 关键字段

| 字段 | 类型 | 说明 |
|---|---|---|
| productionPieceId | string | 生产工件业务 ID |
| orderItemId | string | 订单项业务 ID |
| orderId | string | 订单业务 ID |
| quantity | number | 生产工件数量 |
| pendingPkgQuantity | number | 待打包数量 |
| packedQuantity | number | 已打包数量 |
| status | string | 打包状态展示值，如“待打包”“部分打包” |
| address | string | 收货地址 |
| isUrgent | boolean | 是否加急 |
| previewUrl | string | 预览图 URL |
| createTime | string | 创建时间 |
| width | number | 宽度 |
| height | number | 高度 |
| materialConfig | object | 材料配置 |
| procedureFlow | object | 工序流 |
| logisticsCarrierInfo | object | 物流承运商信息 |
| orderCustomer | object | 客户信息 |
| remark | string | 备注 |

### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "productionPieceId": "PP_1001",
        "orderItemId": "OI_2001",
        "orderId": "ORD_3001",
        "quantity": 2,
        "pendingPkgQuantity": 2,
        "packedQuantity": 0,
        "status": "待打包",
        "address": "浙江省杭州市西湖区xxx",
        "isUrgent": true,
        "previewUrl": "https://oss.example.com/previews/PP_1001.png",
        "createTime": "2026-06-09T09:12:00Z",
        "width": 70.0,
        "height": 90.0,
        "materialConfig": {
          "materialSnapshot": {
            "name": "白卡纸"
          }
        },
        "logisticsCarrierInfo": {
          "carrierId": "SF",
          "carrierName": "顺丰"
        },
        "orderCustomer": {
          "customerName": "张三",
          "customerPhone": "13800000000"
        },
        "procedureFlow": {
          "nodes": [
            { "nodeId": "NODE_PENDING_PACKING", "nodeName": "待打包", "pieceQuantity": 2 },
            { "nodeId": "NODE_LAMINATION", "nodeName": "覆膜", "pieceQuantity": 2 }
          ]
        }
      }
    ],
    "materialList": ["白卡纸"],
    "sizeList": [70.0],
    "processList": ["待打包", "覆膜"]
  },
  "timestamp": 1781000000000
}
```

### 常见错误

| 场景 | 响应/异常 |
|---|---|
| 请求体为空 | 后端抛出 `查询参数不能为空` |
| `manufacturerMetaId` 为空 | 后端抛出 `manufacturerMetaId 不能为空` |
| `orderId` / `orderItemId` 全为空 | 后端抛出 `orderId、orderItemId 必须且只能传一个` |
| `orderId` / `orderItemId` 同时传 | 后端抛出 `orderId、orderItemId 必须且只能传一个` |

---

## 5. 对接注意事项

1. 新接口均为全量返回，不需要传 `current` / `size`。
2. `typesettingId` 查询会归一匹配 `-Mirror`，例如传 `TS_001` 会匹配 `TS_001` 和 `TS_001-Mirror`。
3. 排版范围查询中，三种 ID 一次只能选择一种：
   - `typesettingId`：查待排版的排版数据。
   - `orderId`：先查订单项，再查待排版生产工件。
   - `orderItemId`：直接查待排版生产工件。
4. 待打包范围查询中，两种 ID 一次只能选择一种：
   - `orderId`：先查订单项，再查待打包生产工件。
   - `orderItemId`：直接查待打包生产工件。
5. `listByOrderId` 只返回 `quantity != 0` 的订单项，不返回分页元数据。
