# DeliveryPkgController 接口文档

## 0) 待打包零件查询（含筛选条件与筛选项集合）

- **URL**: `POST /api/manufacturerSide/deliveryPkg/list`
- **说明**:
  - 查询“待打包”零件列表。
  - 数据源侧先做 Mongo 条件查询：`procedureFlow.nodes` 中存在 `nodeName=待打包` 且 `pieceQuantity > 0` 的零件。
  - 支持条件查询：`materialName`、`processName`、`width`。
  - 在组装 `DeliveryPkgPieceVO` 后，再按 `customerPhone`、`startTime`、`endTime`、`carrierName` 做补充过滤。
  - 返回结果包含三个同级筛选集合：`materialList`、`sizeList`、`processList`。

### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| manufacturerMetaId | string | 是 | 工厂标识 |
| customerPhone | string | 否 | 客户手机号（模糊匹配） |
| startTime | string | 否 | 创建时间起（ISO-8601，示例：`2026-05-01T00:00:00Z`） |
| endTime | string | 否 | 创建时间止（ISO-8601，示例：`2026-05-31T23:59:59Z`） |
| carrierName | string | 否 | 物流承运商名称（模糊匹配） |
| materialName | string | 否 | 材料名（精确匹配 `materialConfig.materialSnapshot.name`） |
| processName | string | 否 | 工序名（匹配 `procedureFlow.nodes.nodeName`） |
| width | number | 否 | 零件宽度（匹配 `productionPiece.width`） |

### 请求示例

```json
{
  "manufacturerMetaId": "MFR_10001",
  "customerPhone": "1380000",
  "startTime": "2026-05-01T00:00:00Z",
  "endTime": "2026-05-31T23:59:59Z",
  "carrierName": "顺丰",
  "materialName": "白卡纸",
  "processName": "覆膜",
  "width": 70.0
}
```

### 返回字段（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| items | array | 待打包零件列表（`DeliveryPkgPieceVO`） |
| materialList | array[string] | 从 `items[].materialConfig.materialSnapshot.name` 去重得到 |
| sizeList | array[number] | 从 `items[].width` 去重得到 |
| processList | array[string] | 从 `items[].procedureFlow.nodes[].nodeName` 去重得到 |

### 返回示例

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
        "previewUrl": "https://oss.example.com/previews/PP_1001.png",
        "createTime": "2026-05-08T09:12:00Z",
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
          "name": "张三",
          "mobile": "13800000000"
        },
        "procedureFlow": {
          "nodes": [
            { "nodeId": "NODE_PENDING_PACKING", "nodeName": "待打包", "pieceQuantity": 2 },
            { "nodeId": "NODE_LAMINATION", "nodeName": "覆膜", "pieceQuantity": 2 }
          ]
        }
      }
    ],
    "materialList": ["白卡纸", "铜版纸"],
    "sizeList": [70.0, 90.0],
    "processList": ["待打包", "覆膜", "烫金"]
  }
}
```

---

## 1) 包裹分页查询

- **URL**: `POST /api/manufacturerSide/deliveryPkg/pkgList`
- **说明**: 支持按 `orderId`、`recipientName`、`recipientPhone`、`createTime`（起止时间）和状态分页查询包裹列表。

### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| current | number | 是 | 页码，从 1 开始 |
| size | number | 是 | 每页条数 |
| manufacturerMetaId | string | 否 | 工厂标识 |
| status | string | 否 | 包裹状态（枚举名或中文描述） |
| orderId | string | 否 | 订单 ID |
| recipientName | string | 否 | 收件人姓名 |
| recipientPhone | string | 否 | 收件人手机号 |
| createTimeStart | string | 否 | 创建时间起，ISO-8601 时间（如 `2026-05-01T00:00:00Z`） |
| createTimeEnd | string | 否 | 创建时间止，ISO-8601 时间（如 `2026-05-08T23:59:59Z`） |

### 请求示例

```json
{
  "current": 1,
  "size": 20,
  "manufacturerMetaId": "MFR_10001",
  "orderId": "ORD_3001",
  "recipientName": "张三",
  "recipientPhone": "13800000000",
  "createTimeStart": "2026-05-01T00:00:00Z",
  "createTimeEnd": "2026-05-08T23:59:59Z",
  "status": "PENDING_PACKING"
}
```

### 返回示例

```json
{
  "code": 200,
  "message": "success",
  "current": 1,
  "size": 20,
  "total": 1,
  "data": [
    {
      "deliveryPkgId": "DP20260508001",
      "deliveryPkgCode": "DP20260508001",
      "orderId": "ORD_3001",
      "recipientName": "张三",
      "recipientPhone": "13800000000",
      "recipientAddress": "浙江省杭州市西湖区xxx",
      "deliveryPkgStatus": "PENDING_PACKING",
      "createTime": "2026-05-08T09:12:00Z",
      "deliveryPkgItems": [
        {
          "orderItemId": "OI_2001",
          "productionPieceId": ["PP_1001"],
          "quantity": 2,
          "previewUrl": "https://oss.example.com/previews/PP_1001.png"
        }
      ]
    }
  ]
}
```

---

## 2) 新增打包

- **URL**: `POST /api/manufacturerSide/deliveryPkg/add`
- **说明**: 创建包裹并返回打印所需信息；`deliveryPkgItems.previewUrl` 会保存自 `productionPiece.productImageFile.filePreview.preview`。

---

## 3) 包裹信息重打

- **URL**: `POST /api/manufacturerSide/deliveryPkg/reprint`
- **说明**: 根据 `deliveryPkgId` 查询包裹并按 addPkg 出参结构返回 `DeliveryPkgAddResultVO`，用于重打。

### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| deliveryPkgId | string | 是 | 包裹业务 ID |

### 请求示例

```json
{
  "deliveryPkgId": "DP20260508001"
}
```

### 返回示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pkgId": "DP20260508001",
    "recipientName": "张三",
    "recipientMobile": "13800000000",
    "recipientAddress": "浙江省杭州市西湖区xxx",
    "width": "70.00",
    "height": "90.00",
    "routeDesc": "华东线:西湖区-拱墅区",
    "remark": "这是一个备注",
    "qrCode": {
      "format": "base64-png",
      "content": "https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/qr.jpeg",
      "width": 30,
      "height": 30
    },
    "barCode": {
      "format": "base64-png",
      "content": "https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/line.jpg",
      "width": 70,
      "height": 25
    }
  }
}
```

---

## 4) 包裹释放

- **URL**: `POST /api/manufacturerSide/deliveryPkg/release`
- **说明**: 根据 `deliveryPkgId` 找到包裹及其 `deliveryPkgItems` 对应 `productionPiece`，将 item 的 `quantity` 从“已打包”节点回退到“待打包”节点。

### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| deliveryPkgId | string | 是 | 包裹业务 ID |

### 请求示例

```json
{
  "deliveryPkgId": "DP20260508001"
}
```

### 返回示例

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```
