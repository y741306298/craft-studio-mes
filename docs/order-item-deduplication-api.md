# 订单项去重接口

## 接口

- **Method**: `POST`
- **Path**: `/api/manufacturerSide/order/item/deduplicate`
- **参数**: Query 参数 `orderId`（必填）

示例：

```http
POST /api/manufacturerSide/order/item/deduplicate?orderId=2101141077316124674
```

接口会查询该 `orderId` 下所有未删除的订单项，按 `createTime` 升序、MongoDB 文档 ID
升序排列，保留第一条；其余订单项及其通过 `orderItemId` 关联的 `productionPiece` 会被软删除。

## 返回示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": "2101141077316124674",
    "keptOrderItemId": "保留的订单项业务 ID",
    "matchedOrderItemCount": 3,
    "deletedOrderItemCount": 2,
    "deletedProductionPieceCount": 4
  },
  "timestamp": 1789830000000
}
```

如果 `orderId` 为空、没有对应订单项，或者待删除订单项缺少 MongoDB ID/业务 ID，接口会拒绝执行删除。
