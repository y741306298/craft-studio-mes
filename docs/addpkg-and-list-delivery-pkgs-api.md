# addPkg 与 listDeliveryPkgs 接口文档

本文档对应以下两个接口：

- `DeliveryPkgController#addPkg`：新增包裹并返回打印数据。
- `DeliveryPkgController#listDeliveryPkgs`：按条件分页查询已生成的包裹。

> 本次调整重点是聚单平台（当前为 WDT/旺店通）打包流程：复用已有 WDT 面单记录，缺少面单记录时即时向 WDT 申请面单；只有查不到 WDT 物流配置时才降级为本地 `CUSTOM` 标签。同时，聚单平台云打印数据会保存到 `DeliveryPkg.logisticsCloudPrintData`，并由 `addPkg` 返回。

---

## 1. addPkg：新增打包

### 1.1 基本信息

| 项目 | 内容 |
|---|---|
| HTTP Method | `POST` |
| URL | `/api/manufacturerSide/deliveryPkg/add` |
| Content-Type | `application/json` |
| 返回类型 | `ApiResponse<DeliveryPkgAddResultVO>` |

### 1.2 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `manufacturerMetaId` | string | 是 | 工厂标识；用于查询快递 100 Token 或 WDT 配置，并保存到包裹。 |
| `carrierId` | string | 否 | 请求对象中保留该字段；实际物流信息由服务端根据生产零件所属订单明细查询。 |
| `deliveryManId` | string | 条件必填 | 发货人 ID。普通非聚单平台订单缺少该字段时降级为 `CUSTOM` 打包。 |
| `deliverySiidId` | string | 条件必填 | 电子面单账号/打印配置 ID。普通非聚单平台订单缺少该字段时降级为 `CUSTOM` 打包。 |
| `siid` | string | 否 | 快递 100 云打印设备编码，保存为包裹默认重打设备。 |
| `routeId` | string | 否 | 配送路线 ID；未传时优先继承订单上的 `routeId`。 |
| `routeNodeId` | string | 否 | 配送路线节点 ID；未传时优先继承订单上的 `routeNodeId`。 |
| `pieces` | array | 是 | 本次打包的零件集合，不能为空。 |
| `pieces[].productionPieceId` | string | 是 | 生产零件业务 ID；订单、物流、预览图等数据由服务端批量查询。 |
| `pieces[].quantity` | integer | 是 | 本次打包数量，必须大于 `0`，且不能超过该零件当前“待打包”数量。 |

> 兼容说明：升级期间仍接受旧结构中的 `pieces[].piece.productionPieceId`，但新调用方应使用上表中的扁平结构。

### 1.3 请求示例

```json
{
  "manufacturerMetaId": "MFR_10001",
  "carrierId": "SF",
  "deliveryManId": "DM_001",
  "deliverySiidId": "SIID_001",
  "siid": "PRINT_DEVICE_001",
  "routeId": "ROUTE_001",
  "routeNodeId": "ROUTE_NODE_001",
  "pieces": [
    {
      "productionPieceId": "PP_1001",
      "quantity": 2
    }
  ]
}
```

### 1.4 通用校验

接口创建包裹前会执行以下校验：

1. `pieces` 不能为空。
2. 每一项必须同时包含 `productionPieceId` 和大于 `0` 的 `quantity`，且零件编号不能重复。
3. 服务端批量查询到的生产零件、订单明细和物流信息必须完整。
4. 同一请求中的零件必须具有相同的 `orderId`、`carrierId` 和 `presetType`。
5. 每个 `productionPieceId` 必须能查询到有效生产零件。
6. `quantity` 不能超过对应零件当前“待打包”节点的数量。

任何一项不满足时，不进入后续打包分支，并返回参数错误。

#### 请求体无法生成包裹的排查

`addPkg` 只有在请求体已经被反序列化为 `DeliveryPkgAddRequest` 后才会进入业务逻辑。因此，以下情况会在 Controller 调用前直接失败，也不会新增 `DeliveryPkg`：

- `pieces` 数组元素多包了一层匿名对象，例如 `"pieces": [ { { "piece": ... } } ]`。JSON 对象中的每个 `{` 后必须是字段名，不允许连续出现两个 `{`；正确的旧版结构是 `"pieces": [ { "piece": ..., "quantity": 3 } ]`。
- 把 Markdown 链接（例如 `[https://...](https://...)`）作为 URL 值传入，而不是纯 URL 字符串。
- 在 JSON 字符串中直接换行。地址中的换行必须编码为 `\n`，不能在开始和结束引号之间放置原始换行。
- 把两个完整 JSON 对象直接拼接成一个请求体，例如 `}{` 或在首个对象后继续追加 `{...}`。

对于普通快递 100 流程，请求格式合法但电子面单下单或预下单复打失败时，服务会降级生成 `presetType=CUSTOM` 的本地自定义面单，照常新增 `DeliveryPkg` 并完成零件打包。响应中的 `logisticsCloudPrintData` 和快递单号为空，调用方应使用返回的本地二维码/条码打印数据。

### 1.5 流程分支

#### 1.5.1 路由优先级

读取首个零件的 `orderId` 查询订单，并补齐请求中缺失的路线信息。之后首先按订单渠道分流：

1. 订单渠道为 `GATHER_PLATFORM`：进入聚单平台/WDT 流程，不会因为缺少快递 100 打印机或 Token 而误入快递 100 流程。
2. 其他订单：进入普通 `CUSTOM` 或快递 100 流程。

#### 1.5.2 聚单平台/WDT 流程

聚单平台订单按以下顺序处理：

1. 根据 MES 订单 ID、渠道订单 ID 和订单上的物流单号查询 `WdtLabelRecord`。
2. **已存在 WDT 面单记录**：直接使用记录中的 `logisticsOrderId` 和 `logisticsCloudPrintData` 创建本次包裹，不再调用 WDT 打印接口。
3. **不存在 WDT 面单记录**：
   1. 根据工厂和 `presetType` 查询 `WdtConfig`。
   2. 找到配置后，向 WDT 执行快递换仓/配置，并调用云打印获取面单。
   3. WDT 返回有效 `logisticsOrderId` 后，新增一条状态为 `PRE_ORDERED` 的 `WdtLabelRecord`，保存物流单号、收件信息和 `logisticsCloudPrintData`。
   4. WDT 调用异常或未返回物流单号时，接口返回业务错误，**不会降级为本地标签**。
4. **只有查不到 `WdtConfig` 时**：允许继续打包，创建 `presetType=CUSTOM` 的本地可打印自定义包裹；该分支没有 WDT 云打印数据。
5. 使用 WDT 面单时，每次 `addPkg` 都生成一个新的 `deliveryPkgId` 并新增一条 `DeliveryPkg`。因此同一订单分两次打包，会生成两条包裹数据，而不是复用第一次的包裹。
6. 将 WDT 面单记录中的 `logisticsCloudPrintData` 保存到新包裹，并将 `logisticsOrderId` 保存为包裹的 `kuaidiNum`。
7. 扣减零件“待打包”数量、增加“已打包”数量，并记录本次物流信息。
8. 将 `WdtLabelRecord` 更新为 `CONSUMED`，其 `deliveryPkgId` 指向本次新生成的包裹；清空订单上的 `kuaidiNum`。

> 注意：当前 `WdtLabelRecord` 只有一个 `deliveryPkgId` 字段。若同一个面单记录被用于多次打包，记录上的 `deliveryPkgId` 会更新为最后一次生成的包裹 ID，但每一次生成的 `DeliveryPkg` 都会独立保留。

#### 1.5.3 普通订单的本地 CUSTOM 流程

以下任一条件满足时使用本地 `CUSTOM` 流程：

- 零件物流预设类型为 `CUSTOM`；
- `deliveryManId` 为空；
- `deliverySiidId` 为空；
- 按承运商与工厂查不到快递 100 Token。

该流程直接新增包裹、更新零件打包数量，并返回本地二维码/条码标签数据，不调用快递 100 下单。

#### 1.5.4 普通订单的快递 100 流程

- 若订单已有可用的预下单 `kuaidiNum` 和 `DeliveryRecord`，则打印/复用预下单面单，并恢复或创建对应包裹。
- 若没有可用预下单记录，则调用快递 100 下单打印；成功后创建包裹并保存物流单号，失败时降级创建 `presetType=CUSTOM` 的本地自定义面单包裹。
- 预下单面单缺少打印设备、复打异常或复打返回失败时，同样降级为自定义面单；失败的 `DeliveryRecord` 保留为 `PRINT_FAILED`，订单上的预下单快递单号会被清空，避免后续重复消费。

### 1.6 返回字段

成功响应的 `data` 为 `DeliveryPkgAddResultVO`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `pkgId` | string | 本次生成或恢复的包裹 ID。聚单平台每次打包都会生成新的 ID。 |
| `carrierId` | string | 包裹承运商 ID。 |
| `carrierName` | string | 包裹承运商名称。 |
| `presetType` | string | 实际保存的预设类型；聚单平台查不到 WDT 配置时为 `CUSTOM`。 |
| `recipientName` | string | 收件人姓名。 |
| `recipientMobile` | string | 收件人手机号。 |
| `recipientAddress` | string | 收件地址。 |
| `width` | string | 标签宽度，当前固定为 `70.00`。 |
| `height` | string | 标签高度，当前固定为 `90.00`。 |
| `routeDesc` | string | 路线描述；未绑定有效路线时为 `未定义路线`。 |
| `remark` | string | 包裹备注。 |
| `orgInfo` | object/null | 下单企业信息。 |
| `qrCode` | object | 本地包裹详情二维码信息。 |
| `qrCode.format` | string | 当前为 `base64-png`。 |
| `qrCode.content` | string | PNG 图片 Base64 内容，不包含 Data URL 前缀。 |
| `qrCode.width` | number | 当前为 `30.0`。 |
| `qrCode.height` | number | 当前为 `30.0`。 |
| `barCode` | object | 本地条码信息。 |
| `logisticsCloudPrintData` | object/array/string/null | 聚单平台返回并持久化的原始云打印数据；WDT 面单流程返回对应内容，本地 `CUSTOM` 等无云打印数据的流程返回 `null`。 |

### 1.7 聚单平台成功返回示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pkgId": "DP202607260001",
    "carrierId": "SF",
    "carrierName": "顺丰",
    "presetType": "SF_WDT",
    "recipientName": "张三",
    "recipientMobile": "13800000000",
    "recipientAddress": "浙江省杭州市西湖区示例路 1 号",
    "width": "70.00",
    "height": "90.00",
    "routeDesc": "华东线-西湖区",
    "remark": "订单:ORD_3001",
    "orgInfo": null,
    "qrCode": {
      "format": "base64-png",
      "content": "iVBORw0KGgoAAA...",
      "width": 30.0,
      "height": 30.0
    },
    "barCode": {
      "format": "base64-png",
      "content": "https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/line.jpg",
      "width": 70.0,
      "height": 25.0
    },
    "logisticsCloudPrintData": {
      "templateUrl": "https://example.com/wdt/template",
      "documents": []
    }
  },
  "timestamp": 1785024000000
}
```

### 1.8 查不到 WDT 配置时的返回差异

接口仍返回 `200`，但会返回本地可打印标签：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pkgId": "DP202607260002",
    "carrierId": "SF",
    "carrierName": "顺丰",
    "presetType": "CUSTOM",
    "qrCode": {
      "format": "base64-png",
      "content": "iVBORw0KGgoAAA...",
      "width": 30.0,
      "height": 30.0
    },
    "barCode": {
      "format": "base64-png",
      "content": "https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/line.jpg",
      "width": 70.0,
      "height": 25.0
    },
    "logisticsCloudPrintData": null
  },
  "timestamp": 1785024000000
}
```

---

## 2. listDeliveryPkgs：包裹分页查询

### 2.1 基本信息

| 项目 | 内容 |
|---|---|
| HTTP Method | `POST` |
| URL | `/api/manufacturerSide/deliveryPkg/pkgList` |
| Content-Type | `application/json` |
| 返回类型 | `PagedApiResponse<DeliveryPkgListItemResponse>` |

### 2.2 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `current` | integer | 是 | 页码，从 `1` 开始。 |
| `size` | integer | 是 | 每页数量，范围为 `1`～`100`。 |
| `manufacturerMetaId` | string | 否 | 工厂标识，精确匹配。 |
| `status` | string | 否 | 包裹状态；支持枚举名或中文描述，见状态表。 |
| `orderId` | string | 否 | 订单 ID，模糊匹配。 |
| `recipientName` | string | 否 | 收件人姓名查询条件。 |
| `recipientPhone` | string | 否 | 收件人手机号查询条件。 |
| `kuaidiNum` | string | 否 | 物流单号；聚单平台包裹中对应 WDT 的 `logisticsOrderId`。 |
| `createTimeStart` | string | 否 | 创建时间起始值。 |
| `createTimeEnd` | string | 否 | 创建时间结束值。 |

#### 状态可选值

| 枚举名 | 中文描述 |
|---|---|
| `PENDING_PACKING` | 待打包 |
| `PACKING` | 打包中 |
| `PENDING_DELIVERY` | 待发货 |
| `DELIVERED` | 已发货 |

`status` 不区分枚举名大小写；传入无法识别的枚举名或中文描述时返回“status参数无效”。

### 2.3 请求示例

```json
{
  "current": 1,
  "size": 20,
  "manufacturerMetaId": "MFR_10001",
  "orderId": "ORD_3001",
  "recipientName": "张三",
  "recipientPhone": "13800000000",
  "kuaidiNum": "WDT_LOGISTICS_10001",
  "createTimeStart": "2026-07-01T00:00:00Z",
  "createTimeEnd": "2026-07-31T23:59:59Z",
  "status": "PENDING_PACKING"
}
```

### 2.4 查询与组装规则

1. 将 `status` 转为 `DeliveryPkgStatus`，其他非空条件组成查询条件。
2. 按 `current`、`size` 查询当前页，并使用相同条件统计 `total`。
3. 每个包裹会根据 `routeId`、`routeNodeId` 计算 `routeDesc`；路线信息不完整或路线不存在时返回 `未定义路线`。
4. 根据包裹保存的 `carrierId`、`carrierName` 和 `presetType` 组装 `logisticsCarrierInfo`，方便调用方直接复用统一物流信息结构。
5. 对每条 `deliveryPkgItems` 补充生产零件的材料配置、加工流程、宽高以及订单 ID。
6. 将持久化在 `DeliveryPkg` 中的 `logisticsCloudPrintData` 原样返回，使列表页也能使用与 `addPkg` 一致的云打印参数。
7. 批量查询包裹对应的订单，并在每个列表项的 `orderInfo` 中返回订单信息（包括 `orderInfo.orgInfo`）。

### 2.5 全量查询接口

`POST /api/manufacturerSide/deliveryPkg/pkgListAll` 接受与分页接口相同的条件字段（包括
`createTimeStart`、`createTimeEnd`），但忽略 `current` 和 `size`，通过非分页仓储查询返回全部匹配包裹。
响应类型为 `ApiResponse<List<DeliveryPkgListItemResponse>>`，列表项的组装规则与 `pkgList` 一致。

### 2.6 返回结构

成功响应的分页信息位于 `data` 内，而不是响应顶层：

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | integer | 业务状态码，成功为 `200`。 |
| `message` | string | 业务消息，成功为 `success`。 |
| `data.items` | array | 当前页包裹列表。 |
| `data.current` | integer | 当前页码。 |
| `data.size` | integer | 每页数量。 |
| `data.total` | integer | 符合条件的总记录数。 |
| `timestamp` | integer | 响应时间戳（毫秒）。 |

#### data.items[] 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `deliveryPkgId` | string | 包裹 ID。 |
| `deliveryPkgCode` | string | 包裹编码。 |
| `deliveryPkgStatus` | string | 包裹状态枚举名。 |
| `orderId` | string | 订单 ID。 |
| `orderInfo` | object | 对应订单信息，包含组织信息 `orgInfo`。 |
| `carrierId` | string | 承运商 ID。 |
| `carrierName` | string | 承运商名称。 |
| `logisticsCarrierInfo` | object | 统一物流信息，由包裹上的承运商和预设类型组装。 |
| `logisticsCarrierInfo.carrierId` | string/null | 承运商 ID，与当前包裹的 `carrierId` 一致。 |
| `logisticsCarrierInfo.carrierName` | string/null | 承运商名称，与当前包裹的 `carrierName` 一致。 |
| `logisticsCarrierInfo.presetType` | string/null | 实际物流预设类型，与当前包裹的 `presetType` 一致。 |
| `recipientName` | string | 收件人姓名。 |
| `recipientPhone` | string | 收件人手机号。 |
| `recipientAddress` | string | 收件地址。 |
| `province` / `city` / `district` | string/null | 省、市、区。 |
| `senderName` / `senderPhone` / `senderAddress` | string/null | 寄件人信息。 |
| `weight` | number/null | 重量。 |
| `volume` | number/null | 体积。 |
| `deliveryWay` | string/null | 配送方式。 |
| `presetType` | string | 实际预设类型。 |
| `trackingNumber` | string/null | 发货运单号。 |
| `kuaidiNum` | string/null | 电子面单物流单号；WDT 包裹保存 `logisticsOrderId`。 |
| `packingStartTime` / `packingEndTime` / `deliveryTime` | string/null | 打包及发货时间。 |
| `remarks` | string/null | 包裹备注。 |
| `deliveryManId` | string/null | 发货人 ID。 |
| `deliverySiidId` | string/null | 电子面单账号/打印配置 ID。 |
| `manufacturerMetaId` | string | 工厂标识。 |
| `routeId` | string/null | 路线 ID。 |
| `routeNodeId` | string/null | 路线节点 ID。 |
| `routeDesc` | string | 路线描述。 |
| `logisticsCloudPrintData` | object/array/string/null | WDT 返回并保存的原始云打印数据，与 `addPkg` 返回值一致；无云打印数据时为 `null`。 |
| `deliveryPkgItems` | array | 包裹明细。 |
| `createTime` / `updateTime` | string/null | 基础实体创建、更新时间。 |

#### deliveryPkgItems[] 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `orderItemId` | string/null | 订单明细 ID。 |
| `orderId` | string/null | 根据订单明细补充的订单 ID。 |
| `productionPieceId` | array[string] | 生产零件 ID 列表。 |
| `quantity` | integer | 包裹中的零件数量。 |
| `previewUrl` | string/null | 零件预览图。 |
| `materialConfig` | object/null | 从生产零件补充的材料配置。 |
| `processingFlow` | string/null | 从生产零件补充的加工流程。 |
| `width` | number/null | 零件宽度，保留两位小数。 |
| `height` | number/null | 零件高度，保留两位小数。 |

### 2.6 成功返回示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "deliveryPkgId": "DP202607260001",
        "deliveryPkgCode": "DP202607260001",
        "deliveryPkgStatus": "PENDING_PACKING",
        "orderId": "ORD_3001",
        "carrierId": "SF",
        "carrierName": "顺丰",
        "logisticsCarrierInfo": {
          "carrierId": "SF",
          "carrierName": "顺丰",
          "presetType": "SF_WDT"
        },
        "recipientName": "张三",
        "recipientPhone": "13800000000",
        "recipientAddress": "浙江省杭州市西湖区示例路 1 号",
        "presetType": "SF_WDT",
        "kuaidiNum": "WDT_LOGISTICS_10001",
        "manufacturerMetaId": "MFR_10001",
        "routeId": "ROUTE_001",
        "routeNodeId": "ROUTE_NODE_001",
        "routeDesc": "华东线-西湖区",
        "remarks": "订单:ORD_3001",
        "deliveryPkgItems": [
          {
            "orderItemId": "OI_2001",
            "orderId": "ORD_3001",
            "productionPieceId": ["PP_1001"],
            "quantity": 2,
            "previewUrl": "https://oss.example.com/previews/PP_1001.png",
            "materialConfig": {},
            "processingFlow": "印刷-覆膜-裁切",
            "width": 70.0,
            "height": 90.0
          }
        ],
        "createTime": "2026-07-26T08:30:00Z",
        "updateTime": "2026-07-26T08:30:01Z"
      }
    ],
    "current": 1,
    "size": 20,
    "total": 1,
    "totalOrderCount": null,
    "totalArea": null,
    "totalAmount": null,
    "materialList": null,
    "statusList": null
  },
  "timestamp": 1785054601000
}
```

---

## 3. 前端打印建议

调用 `addPkg` 成功后可按以下优先级选择打印内容：

1. `logisticsCloudPrintData != null`：按聚单平台/WDT 云打印协议解析并打印物流面单。
2. `logisticsCloudPrintData == null` 且 `presetType == "CUSTOM"`：使用响应中的收件信息、`qrCode`、`barCode`、路线和备注打印本地自定义包裹标签。
3. 不应仅根据请求中的 `presetType` 判断实际打印方式，应以响应中的 `presetType` 和 `logisticsCloudPrintData` 为准，因为聚单平台查不到 WDT 配置时会降级为 `CUSTOM`。
