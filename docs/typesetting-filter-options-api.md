# 待排版材料与工艺筛选项接口

## 接口

- **Method**: `POST`
- **Path**: `/api/manufacturerSide/typesetting/filters`
- **Content-Type**: `application/json`

该接口使用与待排版列表相同的查询条件，但忽略 `current`、`size`，查询所有命中数据后生成筛选项。
分页列表接口 `/api/manufacturerSide/typesetting/list` 和 `/api/manufacturerSide/typesetting/list/condition`
不再生成 `processingFlowList`、`materialList`，这两个字段返回 `null`。

## 请求参数

请求结构与 `TypesettingQuery` 相同。`manufacturerMetaId` 必填，其余筛选条件均可选：

```json
{
  "manufacturerMetaId": "manufacturer-meta-id",
  "materialName": "白卡纸",
  "processingName": [
    {
      "processName": "覆膜"
    }
  ],
  "sourceType": "part",
  "routeId": null,
  "startTime": null,
  "endTime": null
}
```

`current`、`size` 即使传入也不会限制筛选项的数据范围。

## 响应

```json
{
  "success": true,
  "data": {
    "processingFlowList": [
      {
        "processName": "覆膜",
        "accessoryName": "哑膜"
      }
    ],
    "materialList": ["白卡纸"]
  }
}
```

- `processingFlowList`：从全部命中数据的工艺节点中按“工艺名称 + 辅料名称”去重。
- `materialList`：从全部命中数据中按材料名称去重。
- 没有命中数据时，两个字段均返回空数组。
