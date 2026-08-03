# StatisticsController 接口文档

## 1) 订单统计分页查询

- **URL**: `POST /api/manufacturerSide/statistics/order/list`
- **Controller**: `StatisticsController#listOrderStatistics`
- **说明**:
  - 先按订单创建时间等订单条件查询订单数据，再查询这些订单关联的订单项进行统计，不返回具体订单项明细。
  - 返回 `items` 为订单维度列表，每条包含 `orderId`、订单总金额 `paymentPrice`、订单创建时间 `createTime`。
  - 返回 `materialList` 为本次订单范围内订单项材料按 `materialId` 去重后的统一材料列表，可用于前端材料筛选项。
  - 返回 `statusList` 为全部订单状态枚举，可用于前端状态筛选项；未传 `status` 时默认排除“已退单”订单。
  - 返回 `orgNameList` 为本次订单范围内去重后的 `orgInfo.name` 列表，可用于前端下单企业筛选项。
  - 统计字段与订单列表接口的分页响应保持一致：`totalOrderCount`、`totalArea`、`totalAmount`。
  - 统计口径基于查询命中的 `OrderItem`：
    - `totalArea`：对命中的订单项按面积计算规则累加。
    - `totalOrderCount`：命中的订单项去重后的订单数。
    - `totalAmount`：命中的订单去重后，累加对应订单 `price.paymentPrice`。
  - 支持按日期查询订单范围；传入材料筛选时，使用 `material.materialId` 查询对应订单项并统计。

### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| current | number | 是 | 页码，从 1 开始 |
| size | number | 是 | 每页条数，范围 1-100 |
| manufacturerId | string | 否 | 工厂/制造商标识，匹配订单项 `manufacturerId` |
| orderId | string | 否 | 订单 ID，模糊匹配订单项 `orderId` |
| status | string | 否 | 订单状态枚举名或状态码；未传时默认查询除 `RETURNED`（已退单）之外的订单统计 |
| routeId | string | 否 | 路线 ID，精确匹配订单 `routeId` |
| createDateStart | string | 否 | 创建日期起，格式 `yyyy-MM-dd`；按北京时间当天 `00:00:00` 转换为查询起始时间 |
| createDateEnd | string | 否 | 创建日期止，格式 `yyyy-MM-dd`；按北京时间当天 `23:59:59` 转换为查询结束时间 |
| materialId | string | 否 | 材料 ID，精确匹配订单项 `material.materialId` |
| materialName | string | 否 | 保留字段；当前统计筛选以 `materialId` 为准 |
| materialType | string | 否 | 保留字段；当前统计筛选以 `materialId` 为准 |
| orgName | string | 否 | 下单企业名称，对订单 `orgInfo.name` 进行模糊匹配 |

### 请求示例

```json
{
  "current": 1,
  "size": 20,
  "manufacturerId": "MFR_10001",
  "createDateStart": "2026-07-01",
  "createDateEnd": "2026-07-22",
  "status": "IN_PRODUCTION",
  "routeId": "ROUTE_001",
  "materialId": "MAT_001",
  "orgName": "工艺"
}
```

### 返回字段（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| items | array[object] | 订单维度统计列表 |
| items[].orderId | string | 订单 ID |
| items[].paymentPrice | number | 订单总金额，对应订单 `price.paymentPrice`，保留 2 位小数 |
| items[].createTime | string | 订单创建时间；若订单信息不存在则回退为命中订单项的创建时间 |
| current | number | 当前页码 |
| size | number | 每页条数 |
| total | number | 命中条件后的订单维度总数，即去重后的订单数量 |
| totalOrderCount | number | 统计总订单数，当前与 `total` 一致 |
| totalArea | number | 命中订单项累计总面积，保留 2 位小数 |
| totalAmount | number | 命中订单去重后的订单总金额，保留 2 位小数 |
| materialList | array[object] | 本次订单范围内订单项材料去重列表 |
| materialList[].materialId | string | 材料 ID |
| materialList[].materialName | string | 材料名称 |
| materialList[].materialType | string | 材料类型 |
| statusList | array[object] | 全部订单状态枚举列表 |
| statusList[].code | string | 订单状态码 |
| statusList[].description | string | 订单状态描述 |
| orgNameList | array[string] | 本次订单范围内的下单企业名称去重列表 |

### 返回示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "orderId": "ORD_3001",
        "paymentPrice": 1288.00,
        "createTime": "2026-07-22T09:12:00.000+00:00"
      },
      {
        "orderId": "ORD_3002",
        "paymentPrice": 980.50,
        "createTime": "2026-07-21T15:30:00.000+00:00"
      }
    ],
    "current": 1,
    "size": 20,
    "total": 2,
    "totalOrderCount": 2,
    "totalArea": 35.42,
    "totalAmount": 2268.50,
    "orgNameList": ["工艺科技", "工艺印刷"],
    "statusList": [
      {
        "code": "PENDING",
        "description": "待处理"
      },
      {
        "code": "RETURNED",
        "description": "已退单"
      }
    ],
    "materialList": [
      {
        "materialId": "MAT_001",
        "materialName": "白卡纸",
        "materialType": "WIDE"
      },
      {
        "materialId": "MAT_002",
        "materialName": "铜版纸",
        "materialType": "PERFORM"
      }
    ]
  }
}
```

### 统计与分页规则

- 先根据订单条件查询订单数据，日期条件作用于订单 `createTime`；路线条件精确匹配订单 `routeId`。
- 未传入 `status` 时，默认查询除 `RETURNED`（已退单）之外的所有订单统计；传入 `status` 时按指定状态查询。
- 再查询这些订单关联的订单项，并按 `orderItem.material.materialId` 去重形成 `materialList`。
- 如传入 `orgName`，对订单 `orgInfo.name` 进行包含匹配；`orgNameList` 在企业名称筛选前收集并去重。
- 如传入 `materialId`，只统计材料 ID 匹配的订单项；未传入则统计订单下全部订单项。
- 对存在命中订单项的订单按 `orderId` 去重，形成订单维度 `items`。
- `items` 按订单创建时间倒序排列；订单创建时间为空的数据排在后面。
- 分页只作用在订单维度 `items` 上，不影响统计汇总值。
- 统计汇总值始终基于本次查询命中的全部订单项/订单计算，而不是只统计当前页数据。

### 错误说明

| 场景 | 说明 |
|---|---|
| `size <= 0` 或 `size > 100` | 返回参数错误：`每页大小必须在 1-100 之间` |
| `createDateStart` 格式错误 | 返回参数错误：`开始日期格式错误，应为 yyyy-MM-dd` |
| `createDateEnd` 格式错误 | 返回参数错误：`结束日期格式错误，应为 yyyy-MM-dd` |
| `status` 不是合法 `OrderStatus` 枚举名 | 会由枚举解析抛出参数错误 |
