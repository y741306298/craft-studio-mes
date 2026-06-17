# 配送路线与地址识别记录接口文档

本文档覆盖配送路线（DeliveryRoute）本次修改接口，以及地址识别记录（AddressRecognitionRecord）新增接口。

## 1. 基础路径

两套接口路径一致，仅 base path 不同：

| 端 | Base Path |
| --- | --- |
| 配置端 | `/api/configSide/delivery/deliveryRoute` |
| 厂商端 | `/api/manufacturerSide/delivery/deliveryRoute` |

后续接口路径均以 `{basePath}` 表示以上任一 base path。

## 2. 通用分页结构

### 2.1 分页请求公共字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | number | 是 | 当前页，从 1 开始 |
| `size` | number | 是 | 每页数量，1-100 |

### 2.2 分页响应公共结构

```json
{
  "data": {
    "items": [],
    "current": 1,
    "size": 10,
    "total": 0
  }
}
```

### 2.3 普通成功响应

写操作通常返回：

```json
{
  "data": "success"
}
```

## 3. 数据对象

### 3.1 RouteNode

轻量路线节点，仅包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 服务端按传入顺序生成，值为 `"1"`, `"2"`, `"3"` ... |
| `name` | string | 节点名称，不能为空 |

### 3.2 DeliveryRouteListResponse

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | Mongo ID |
| `routeId` | string | 业务路线 ID |
| `routeName` | string | 路线名称 |
| `routeNodes` | RouteNode[] | 轻量路线节点列表 |
| `status` | string | 路线状态 |
| `createTime` | datetime | 创建时间 |
| `updateTime` | datetime | 更新时间 |

### 3.3 AddressRecognitionRecordResponse

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 地址识别记录 ID |
| `address` | object | 原始地址对象，保存订单收件地址 |
| `orgInfo` | object | 下单企业信息，包含 `name` |
| `consignee` | object | 收货人信息，包含 `name`、`phone`、`address` |
| `fullAddress` | string | 补全后的完整地址展示文本 |
| `orderId` | string | 关联订单号 |
| `routeId` | string | 已绑定路线 ID；未分配时为空 |
| `nodeId` | string | 已绑定路线节点 ID；未分配时为空 |
| `status` | string | 枚举：`ASSIGNED` / `UNASSIGNED` |
| `statusName` | string | 中文状态：`已分配` / `未分配` |
| `order` | integer | 节点内排序值 |
| `createTime` | datetime | 创建时间 |
| `updateTime` | datetime | 更新时间 |

## 4. 配送路线接口

### 4.1 分页查询配送路线

```http
POST {basePath}/list
```

#### 请求体

```json
{
  "current": 1,
  "size": 10,
  "manufacturerMetaId": "MANUFACTURER_ID",
  "name": "城北"
}
```

#### 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | number | 是 | 当前页 |
| `size` | number | 是 | 每页数量 |
| `manufacturerMetaId` | string | 是 | 厂商 ID |
| `name` | string | 否 | 路线名称模糊搜索 |
| `routeName` | string | 否 | 兼容旧字段；当 `name` 为空时使用 |

#### 响应体

```json
{
  "data": {
    "items": [
      {
        "id": "routeMongoId",
        "routeId": "ROUTE_001",
        "routeName": "城北线路",
        "routeNodes": [
          { "id": "1", "name": "节点A" },
          { "id": "2", "name": "节点B" }
        ],
        "status": "ACTIVE",
        "createTime": "2026-06-11 10:00:00",
        "updateTime": "2026-06-11 10:00:00"
      }
    ],
    "current": 1,
    "size": 10,
    "total": 1
  }
}
```

### 4.2 分页查询配送路线（raw）

```http
POST {basePath}/list/raw
```

请求体和响应体同 `/list`。当前也直接返回 `routeNodes`，不再补全旧的 `deliveryRouteNodes`。

### 4.3 获取配送路线详情

```http
GET {basePath}/{id}
```

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 配送路线 Mongo ID |

#### 响应体

```json
{
  "data": {
    "id": "routeMongoId",
    "routeId": "ROUTE_001",
    "routeName": "城北线路",
    "routeNodes": [
      { "id": "1", "name": "节点A" }
    ],
    "status": "ACTIVE",
    "createTime": "2026-06-11 10:00:00",
    "updateTime": "2026-06-11 10:00:00"
  }
}
```

### 4.4 新增配送路线

```http
POST {basePath}/add
```

#### 请求体

```json
{
  "routeName": "城北线路",
  "manufacturerMetaId": "MANUFACTURER_ID",
  "routeNodes": [
    { "name": "节点A" },
    { "name": "节点B" }
  ],
  "status": "ACTIVE"
}
```

#### 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `routeName` | string | 是 | 路线名称 |
| `manufacturerMetaId` | string | 是 | 厂商 ID |
| `routeNodes` | RouteNode[] | 是 | 路线节点列表 |
| `routeNodes[].name` | string | 是 | 节点名称，不能为空 |
| `routeNodes[].id` | string | 否 | 无需前端传；服务端按顺序生成 |
| `status` | string | 否 | 路线状态 |

#### 业务说明

服务端会校验 `routeNodes` 非空、每个节点名称非空，并按传入顺序将节点 ID 设置为 `"1"`, `"2"`, `"3"` ...。

#### 响应体

```json
{
  "data": "success"
}
```

### 4.5 编辑配送路线

```http
POST {basePath}/edit
```

#### 请求体

```json
{
  "id": "routeMongoId",
  "routeName": "城北线路-更新",
  "manufacturerMetaId": "MANUFACTURER_ID",
  "routeNodes": [
    { "name": "节点A" },
    { "name": "节点B" },
    { "name": "节点C" }
  ],
  "status": "ACTIVE"
}
```

#### 说明

* `id` 必填，用于定位要编辑的路线。
* 若传入 `routeNodes`，服务端会重新按顺序生成 `routeNodes[].id`。
* 路线不存在时返回失败，提示“配送路线不存在”。

#### 响应体

```json
{
  "data": "success"
}
```

### 4.6 删除配送路线

```http
DELETE {basePath}/{id}
```

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 配送路线 Mongo ID |

#### 响应体

```json
{
  "data": "success"
}
```

### 4.7 激活配送路线

```http
POST {basePath}/{id}/activate
```

#### 响应体

```json
{
  "data": "success"
}
```

### 4.8 停用配送路线

```http
POST {basePath}/{id}/deactivate
```

#### 响应体

```json
{
  "data": "success"
}
```

## 5. 地址识别记录接口

### 5.1 查询未分配地址识别记录

```http
POST {basePath}/address-recognition/unassigned/list
```

#### 请求体

```json
{
  "current": 1,
  "size": 10,
  "name": "科技园"
}
```

#### 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | number | 是 | 当前页 |
| `size` | number | 是 | 每页数量 |
| `name` | string | 否 | 地址识别记录名称/地址关键字模糊搜索 |
| `detailAddress` | string | 否 | 兼容旧字段；当 `name` 为空时使用 |

#### 响应体

```json
{
  "data": {
    "items": [
      {
        "id": "recordMongoId",
        "address": {
          "terminalRegionCode": "CN-xxxx",
          "detailAddress": "科技园某栋"
        },
        "fullAddress": "中国xx省xx市xx区科技园某栋",
        "orderId": "ORDER_001",
        "routeId": null,
        "nodeId": null,
        "status": "UNASSIGNED",
        "statusName": "未分配",
        "createTime": "2026-06-11 10:00:00",
        "updateTime": "2026-06-11 10:00:00"
      }
    ],
    "current": 1,
    "size": 10,
    "total": 1
  }
}
```

### 5.2 查询已分配地址识别记录

```http
POST {basePath}/address-recognition/assigned/list
```

#### 请求体

```json
{
  "current": 1,
  "size": 10,
  "routeId": "ROUTE_001",
  "nodeId": "1",
  "name": "科技园"
}
```

#### 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | number | 是 | 当前页 |
| `size` | number | 是 | 每页数量 |
| `routeId` | string | 是 | 路线 ID |
| `nodeId` | string | 是 | 路线节点 ID |
| `name` | string | 否 | 地址识别记录名称/地址关键字模糊搜索 |
| `detailAddress` | string | 否 | 兼容旧字段；当 `name` 为空时使用 |

#### 响应体

```json
{
  "data": {
    "items": [
      {
        "id": "recordMongoId",
        "address": {
          "terminalRegionCode": "CN-xxxx",
          "detailAddress": "科技园某栋"
        },
        "fullAddress": "中国xx省xx市xx区科技园某栋",
        "orderId": "ORDER_001",
        "routeId": "ROUTE_001",
        "nodeId": "1",
        "status": "ASSIGNED",
        "statusName": "已分配",
        "createTime": "2026-06-11 10:00:00",
        "updateTime": "2026-06-11 10:00:00"
      }
    ],
    "current": 1,
    "size": 10,
    "total": 1
  }
}
```

### 5.3 单条绑定地址识别记录

```http
POST {basePath}/address-recognition/bind
```

#### 请求体

```json
{
  "recordId": "recordMongoId",
  "routeId": "ROUTE_001",
  "nodeId": "1",
  "order": 1
}
```

#### 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `recordId` | string | 是 | 地址识别记录 ID |
| `routeId` | string | 是 | 目标路线 ID |
| `nodeId` | string | 是 | 目标路线节点 ID |
| `order` | integer | 否 | 节点内排序值；不传时默认追加到当前节点最大排序值之后 |

#### 业务说明

绑定成功后，地址识别记录会写入 `routeId`、`nodeId`、`order`，状态更新为“已分配”。若记录有关联 `orderId`，会同步更新对应 `OrderInfo` 和所有 `OrderItem` 的 `routeId` / `routeNodeId`。

#### 响应体

```json
{
  "data": "success"
}
```

### 5.4 变更地址识别记录绑定

```http
POST {basePath}/address-recognition/change-bind
```

#### 请求体

```json
{
  "recordId": "recordMongoId",
  "routeId": "ROUTE_002",
  "nodeId": "2",
  "order": 2
}
```

#### 说明

请求体与单条绑定一致，用于将已绑定记录改绑到新的路线/节点。实现复用绑定逻辑，因此同样会同步订单和订单项路线字段。

#### 响应体

```json
{
  "data": "success"
}
```

### 5.5 批量绑定地址识别记录

```http
POST {basePath}/address-recognition/batch-bind
```

#### 请求体

```json
{
  "recordIds": ["recordMongoId1", "recordMongoId2"],
  "routeId": "ROUTE_001",
  "nodeId": "1",
  "order": 1
}
```

#### 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `recordIds` | string[] | 是 | 地址识别记录 ID 列表，不能为空 |
| `routeId` | string | 是 | 目标路线 ID |
| `nodeId` | string | 是 | 目标路线节点 ID |
| `order` | integer | 否 | 节点内排序值；不传时默认追加到当前节点最大排序值之后 |

#### 响应体

```json
{
  "data": "success"
}
```

### 5.6 批量变更地址识别记录绑定

```http
POST {basePath}/address-recognition/batch-change-bind
```

#### 请求体

```json
{
  "recordIds": ["recordMongoId1", "recordMongoId2"],
  "routeId": "ROUTE_002",
  "nodeId": "2",
  "order": 2
}
```

#### 说明

请求体与批量绑定一致，用于将多条已绑定或未绑定的地址识别记录批量改绑到新的路线/节点。实现复用批量绑定逻辑，因此同样会同步对应生产中订单、订单项和生产件的路线字段。

#### 响应体

```json
{
  "data": "success"
}
```

### 5.7 解绑地址识别记录

```http
DELETE {basePath}/address-recognition/{recordId}
```

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `recordId` | string | 是 | 地址识别记录 ID |

> 说明：该接口仅清除记录的路线/节点绑定和节点内排序，并将记录状态改为未分配，不删除地址识别记录本身。

#### 响应体

```json
{
  "data": "success"
}
```

### 5.7 批量解绑地址识别记录

```http
DELETE {basePath}/address-recognition/batch
```

#### 请求体

```json
{
  "recordIds": ["recordMongoId1", "recordMongoId2"]
}
```

#### 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `recordIds` | string[] | 是 | 地址识别记录 ID 列表，不能为空；接口仅清除路线/节点绑定并将记录状态改为未分配，不删除记录 |

#### 响应体

```json
{
  "data": "success"
}
```

## 6. 兼容与注意事项

1. 路线列表搜索优先使用 `name`，兼容旧字段 `routeName`。
2. 地址识别记录搜索优先使用 `name`，兼容旧字段 `detailAddress`。
3. `routeNodes[].id` 不需要前端传，服务端按传入顺序自动生成。
4. 排版/生产工件和待打包列表没有 `name` 搜索变更；本次 `name` 搜索仅限配送路线和地址识别记录。
