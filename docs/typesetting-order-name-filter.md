# 待排版列表按订单名称筛选

`POST /list`（listTypesettingAndProductionPieces）和 `POST /filters` 的请求新增：

```json
{
  "manufacturerMetaId": "工厂ID",
  "customerName": "张",
  "orgName": "公司",
  "current": 1,
  "size": 50
}
```

- `customerName` 匹配订单的 `customer.customerName`；`orgName` 匹配订单的 `orgInfo.name`。
- 去除首尾空白后按区分大小写的普通文本包含匹配；正则特殊字符作为文本。两个条件同时传入时取 AND。空白条件忽略。
- 名称过滤只适用于有明确所属订单的零件。传入名称条件时不返回印版；与 `sourceType=TYPESETTING` 组合时返回空列表。
- 与材料、工艺、时间、路线、订单项等现有零件条件取交集。过滤发生于数据库计数、排序和分页之前，列表与 total 使用相同的订单项范围。
- `/filters` 使用相同名称条件；`/listById` 不增加名称过滤行为。

## 查询开销

未传名称条件时不执行新增的名称查询。传入条件时，在指定工厂的有效订单项上执行一次聚合，关联有效订单匹配名称，只向应用返回订单项业务 ID 和历史主键 ID。结果供加急/普通零件的计数和分页共同使用，无逐件数据库请求；无匹配时直接返回空结果。

新增索引定义：`orderItem(manufacturerId, deleteAt)` 与 `orderInfo(orderId, deleteAt)`；零件侧复用已有工厂、订单项索引。订单关联使用 `$lookup` 的 `let/pipeline` 形式。

名称包含匹配不能依靠普通 B-tree 名称索引直接定位。开销随工厂有效订单项数量、关联订单数量以及匹配 ID 数量增加；宽泛关键词会产生较大的 `$in` 范围。新增索引能帮助缩小关联范围，但不能保证与不带名称条件的查询耗时相同。上线前应在实际数据上对新增聚合和零件查询执行 explain("executionStats")，检查索引、扫描量、排序及耗时。本次未做真实数据库性能测量。
