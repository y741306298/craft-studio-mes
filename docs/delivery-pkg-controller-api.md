# DeliveryPkgController 接口文档

## 1) 查询待打包零件列表

- **URL**: `POST /api/manufacturerSide/deliveryPkg/list`
- **说明**: 查询指定工厂下待打包零件清单（全量），返回每个零件的待打包数量、已打包数量、物流信息等。

### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| manufacturerMetaId | string | 是 | 工厂（制造商）标识 |
| orderId | string | 否 | 兼容字段，本接口当前未使用 |
| carrierId | string | 否 | 兼容字段，本接口当前未使用 |
| deliveryManId | string | 否 | 兼容字段，本接口当前未使用 |
| deliverySiidId | string | 否 | 兼容字段，本接口当前未使用 |
| userId | string | 否 | 兼容字段，本接口当前未使用 |
| remark | string | 否 | 兼容字段，本接口当前未使用 |
| productionPieces | array | 否 | 兼容字段，本接口当前未使用 |

### 请求示例

```json
{
  "manufacturerMetaId": "MFR_10001"
}
```

### 返回示例

```json
{
  "code": 200,
  "message": "success",
  "current": 1,
  "size": 2,
  "total": 2,
  "data": [
    {
      "productionPieceId": "PP_1001",
      "orderItemId": "OI_2001",
      "orderId": "ORD_3001",
      "quantity": 10,
      "pendingPkgQuantity": 8,
      "packedQuantity": 2,
      "status": "部分打包",
      "previewUrl": "https://example.com/preview/PP_1001.png",
      "materialConfig": {},
      "logisticsCarrierInfo": {
        "carrierId": "KD100_YTO",
        "carrierName": "圆通速递"
      },
      "orderCustomer": {
        "name": "张三",
        "mobile": "13800000000"
      }
    }
  ]
}
```

---

## 2) 新增打包

- **URL**: `POST /api/manufacturerSide/deliveryPkg/add`
- **说明**: 按指定零件与数量创建打包记录，并指定发货人、打印机；支持“自主配送”分支。

### 业务规则

1. `pieces` 不能为空，且每一项都要提供 `piece` 与 `quantity > 0`。
2. `pieces` 内所有项必须属于**同一个 `orderId`**。
3. `pieces` 内所有项必须属于**同一个 `logisticsCarrierInfo`**（按 `carrierId` 校验）。
4. 非“自主配送”时：复用现有快递电子面单逻辑（调用 `toPkg`）。
5. “自主配送”时：
   - 必须提供 `routeId` 与 `routeNodeId`；
   - 跳过快递电子面单逻辑，直接保存包裹信息并更新工序节点数量。

### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| manufacturerMetaId | string | 是 | 工厂（制造商）标识 |
| deliveryManId | string | 是 | 发货人 ID |
| deliverySiidId | string | 是 | 云打印机设备 ID |
| carrierId | string | 否 | 快递方式 ID（非自主配送建议传入） |
| routeId | string | 条件必填 | 当物流为“自主配送”时必填 |
| routeNodeId | string | 条件必填 | 当物流为“自主配送”时必填（路线段落/节点） |
| pieces | array | 是 | 打包零件列表 |
| pieces[].quantity | integer | 是 | 本次该零件打包数量，需 > 0 |
| pieces[].piece.productionPieceId | string | 是 | 生产零件 ID |
| pieces[].piece.orderId | string | 是 | 订单 ID（用于同单校验） |
| pieces[].piece.logisticsCarrierInfo.carrierId | string | 是 | 物流方式 ID（用于同物流校验） |
| pieces[].piece.logisticsCarrierInfo.carrierName | string | 是 | 物流方式名称（`自主配送` 走特殊分支） |

> 说明：`pieces[].piece` 可携带前端列表页返回的完整 `DeliveryPkgPieceVO`，后端关键使用字段如上。

### 请求示例（普通快递）

```json
{
  "manufacturerMetaId": "MFR_10001",
  "deliveryManId": "DM_001",
  "deliverySiidId": "SIID_001",
  "carrierId": "KD100_YTO",
  "pieces": [
    {
      "quantity": 2,
      "piece": {
        "productionPieceId": "PP_1001",
        "orderId": "ORD_3001",
        "logisticsCarrierInfo": {
          "carrierId": "KD100_YTO",
          "carrierName": "圆通速递"
        }
      }
    },
    {
      "quantity": 1,
      "piece": {
        "productionPieceId": "PP_1002",
        "orderId": "ORD_3001",
        "logisticsCarrierInfo": {
          "carrierId": "KD100_YTO",
          "carrierName": "圆通速递"
        }
      }
    }
  ]
}
```

### 请求示例（自主配送）

```json
{
  "manufacturerMetaId": "MFR_10001",
  "deliveryManId": "DM_001",
  "deliverySiidId": "SIID_001",
  "routeId": "ROUTE_01",
  "routeNodeId": "NODE_03",
  "pieces": [
    {
      "quantity": 3,
      "piece": {
        "productionPieceId": "PP_2001",
        "orderId": "ORD_5001",
        "logisticsCarrierInfo": {
          "carrierId": "SELF",
          "carrierName": "自主配送"
        }
      }
    }
  ]
}
```

### 返回示例（成功）

```json
{
  "code": 200,
  "message": "success",
  "data": "success"
}
```

### 返回示例（失败）

```json
{
  "code": 400,
  "message": "仅支持同一订单且同一物流方式一起打包",
  "data": null
}
```
