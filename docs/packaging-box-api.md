# 包装箱管理接口

基础路径为 `/api/configSide/packagingBox`。接口沿用配置侧登录校验，请携带 `Authorization: Bearer <token>`。POST 请求使用 `Content-Type: application/json`。

| 操作 | 方法与相对路径 | 请求 | 成功响应 `data` |
| --- | --- | --- | --- |
| 新增 | `POST /add` | 包装箱字段 | 新增的包装箱对象 |
| 详情 | `GET /{id}` | 路径参数 `id` | 包装箱对象 |
| 编辑 | `POST /edit` | `id` 和包装箱字段 | `"success"` |
| 删除 | `DELETE /delete/{id}` | 路径参数 `id` | `"success"` |
| 分页列表 | `POST /list` | 分页参数和可选筛选条件 | 分页对象 |
| 纸板长度枚举 | `GET /cardboardHeights` | 无 | 纸板长度数字数组 |
| 纸板宽度枚举 | `GET /cardboardWidths` | 无 | 纸板宽度数字数组 |

## 新增与编辑

新增时服务端生成 `id`。编辑时在请求体中传入 `id` 和要保存的整套字段。编辑保留原创建时间并刷新更新时间，省略可选字段或传入 `null` 会清空该字段。删除采用软删除，已删除记录会从详情和列表中排除。

| 字段 | JSON 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | 字符串 | 编辑时必填 | 包装箱 ID |
| `boxName` | 字符串 | 是 | 箱名，去除首尾空白后须有内容 |
| `code` | 字符串 | 是 | 编码，去除首尾空白后须有内容 |
| `cardboardDepth` | 数字 | 否 | 纸板厚度 |
| `cardboardHeight` | 数字 | 否 | 纸板长度 |
| `cardboardWidth` | 数字 | 否 | 纸板宽度 |
| `boxLength` | 数字 | 否 | 箱长 |
| `boxWidth` | 数字 | 否 | 箱宽 |
| `height` | 数字 | 否 | 箱高 |
| `cuttingKnife` | 字符串 | 否 | 切割刀 |
| `rollingCutter` | 字符串 | 否 | 滚压刀 |
| `grindingKnife` | 字符串 | 否 | 磨刻刀 |

尺寸支持小数，提供时必须大于 0。请在调用端统一尺寸单位，接口按原数值保存。

新增请求示例

```json
{
  "boxName": "小号包装箱",
  "code": "BOX-001",
  "cardboardDepth": 2.5,
  "cardboardHeight": 560,
  "cardboardWidth": 420,
  "boxLength": 240,
  "boxWidth": 160,
  "height": 120,
  "cuttingKnife": "CK-01",
  "rollingCutter": "RC-01",
  "grindingKnife": "GK-01"
}
```

包装箱对象包含表中的所有字段，以及 `createTime` 和 `updateTime`。时间使用北京时间，格式为 `yyyy-MM-dd HH:mm:ss`。

## 分页列表

| 字段 | JSON 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | 整数 | 是 | 页码，从 1 开始 |
| `size` | 整数 | 是 | 每页条数，范围为 1–100 |
| `boxName` | 字符串 | 否 | 按箱名做忽略大小写的包含匹配 |
| `code` | 字符串 | 否 | 按编码做区分大小写的精确匹配 |
| `cardboardHeight` | 数字 | 否 | 按纸板长度做精确数值匹配 |
| `cardboardWidth` | 数字 | 否 | 按纸板宽度做精确数值匹配 |

文本筛选值会去除首尾空白，空值跳过。尺寸须大于 0，`560` 和 `560.0` 视为同一数值。传入多个条件时须同时满足。结果按 `updateTime` 倒序，同一更新时间按 ID 倒序。`data.items` 为当前页的包装箱对象数组，`data.current` 和 `data.size` 为请求的页码和页大小，`data.total` 为筛选后的总记录数。超出最后一页时，`items` 为空数组。

```json
{
  "current": 1,
  "size": 20,
  "boxName": "小号",
  "code": "BOX-001",
  "cardboardHeight": 560,
  "cardboardWidth": 420
}
```

## 纸板尺寸枚举

新增或编辑包装箱时，保存成功的非空纸板长度、宽度分别加入对应枚举。已有包装箱的纸板尺寸也会纳入枚举。编辑尺寸或软删除包装箱后，已录入的尺寸仍可复用。分页筛选按未删除包装箱当前保存的尺寸匹配。

两个 GET 接口分别在 `data` 中返回对应维度的全部数值，按升序排列并按数值去重。例如纸板长度可返回 `[320, 560, 620.5]`。暂无数据时返回 `[]`。

## 响应状态

统一响应包含 `code`、`message`、`data` 和 `timestamp`。成功时 HTTP 状态与 `code` 均为 `200`。参数错误、详情不存在、编辑或删除的记录不存在时均为 `400`，具体原因在 `message` 中。登录校验失败时为 `401`。
