# DeliveryPkgController 接口文档

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
