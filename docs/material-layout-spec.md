# 材料排版规格配置说明

## 背景

材料排版规格配置用于描述工厂在排版时，对某个材料按照长度阶梯执行的内缩规则。当前设计分为两个层次：

1. **可配置材料（MaterialLayoutSpec）**：只保存哪些材料允许被配置，以及材料快照、使用尺寸等默认展示信息。材料本身的可配置范围不随工厂变化。
2. **工厂材料步进配置（ManufacturerMaterialLayoutSpecCfg）**：工厂角色选中某个可配置材料后，直接在该工厂、该材料组合下维护 1m 到 10m 的阶梯内缩值。

> 关键点：步进配置信息跟着工厂走，保存到 `ManufacturerMaterialLayoutSpecCfg`，不是先给材料配好步进信息再绑定工厂。

## 领域模型

### MaterialLayoutSpec（可配置材料）

| 字段 | 说明 |
| --- | --- |
| `id` | 可配置材料配置 ID。 |
| `materialId` | 材料 ID，参考订单明细中的 `orderItem.material.materialId`。 |
| `materialSnapshot` | 材料快照，参考订单明细中的 `orderItem.material.materialSnapshot`，用于保留配置时的材料名称、属性等展示信息。 |
| `usageSize3D` | 材料使用尺寸，参考订单明细中的 `orderItem.material.usageSize3D`。 |

### MaterialLayoutSpecStep

| 字段 | 说明 | 示例 |
| --- | --- | --- |
| `maxLengthMeter` | 长度阶梯上限，单位：米。 | `1` 表示 1m 内。 |
| `insetCm` | 当前阶梯内缩值，单位：厘米。 | `2.5` 表示内缩 2.5cm。 |

### ManufacturerMaterialLayoutSpecCfg（工厂材料步进配置）

| 字段 | 说明 |
| --- | --- |
| `id` | 工厂材料步进配置 ID。 |
| `manufacturerMetaId` | 制造商/工厂元数据 ID。 |
| `materialId` | 工厂角色选中的可配置材料 ID。 |
| `materialSnapshot` | 工厂配置时的材料快照；请求未传时会从可配置材料中补齐。 |
| `usageSize3D` | 工厂配置时的材料使用尺寸；请求未传时会从可配置材料中补齐。 |
| `insetSteps` | 当前工厂、当前材料自己的阶梯内缩数据，必须覆盖 1m 到 10m。 |

> 说明：应用服务会校验 `insetSteps` 必须覆盖 `maxLengthMeter = 1..10` 的阶梯值，避免配置缺失导致排版计算结果不稳定。

## 接口说明

### 可配置材料接口

Controller：`MaterialLayoutSpecController`

基础路径：`/api/configSide/materialLayoutSpec`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/list` | 分页查询可配置材料，支持按 `materialId`、`materialName` 筛选。 |
| `GET` | `/{id}` | 查询可配置材料详情。 |
| `POST` | `/add` | 新增可配置材料。 |
| `POST` | `/edit` | 编辑可配置材料。 |
| `GET` / `DELETE` | `/delete/{id}` | 删除可配置材料。 |

#### 新增/编辑可配置材料请求示例

```json
{
  "id": "编辑时传入，新增不传",
  "materialId": "material_001",
  "materialSnapshot": {
    "materialName": "示例材料"
  },
  "usageSize3D": {
    "length": 1000,
    "width": 500,
    "height": 18
  }
}
```

### 工厂材料步进配置接口

Controller：`ManufacturerMaterialLayoutSpecCfgController`

基础路径：`/api/configSide/manufacturerMaterialLayoutSpecCfg`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/list` | 分页查询工厂材料步进配置，支持按 `manufacturerMetaId`、`materialId` 筛选。 |
| `POST` | `/detail` | 按请求体中的可配置材料 `materialId` 和工厂 `manufacturerMetaId` 查询工厂材料步进配置详情。 |
| `POST` | `/add` | 新增工厂材料步进配置。 |
| `POST` | `/edit` | 编辑工厂材料步进配置。 |
| `GET` / `DELETE` | `/delete/{id}` | 删除工厂材料步进配置。 |

#### 新增/编辑工厂材料步进配置请求示例

```json
{
  "id": "编辑时传入，新增不传",
  "manufacturerMetaId": "manufacturer_001",
  "materialId": "material_001",
  "materialSnapshot": {
    "materialName": "示例材料"
  },
  "usageSize3D": {
    "length": 1000,
    "width": 500,
    "height": 18
  },
  "insetSteps": [
    { "maxLengthMeter": 1, "insetCm": 1.0 },
    { "maxLengthMeter": 2, "insetCm": 1.5 },
    { "maxLengthMeter": 3, "insetCm": 2.0 },
    { "maxLengthMeter": 4, "insetCm": 2.5 },
    { "maxLengthMeter": 5, "insetCm": 3.0 },
    { "maxLengthMeter": 6, "insetCm": 3.5 },
    { "maxLengthMeter": 7, "insetCm": 4.0 },
    { "maxLengthMeter": 8, "insetCm": 4.5 },
    { "maxLengthMeter": 9, "insetCm": 5.0 },
    { "maxLengthMeter": 10, "insetCm": 5.5 }
  ]
}
```

## 配置流程建议

1. 平台侧先通过 `/api/configSide/materialLayoutSpec/add` 维护可配置材料清单，材料清单中不维护步进值。
2. 工厂角色通过 `/api/configSide/manufacturerMaterialLayoutSpecCfg/add` 选择 `materialId`，并配置该工厂、该材料自己的 1m 到 10m 阶梯内缩数据。
3. 后续某个工厂要调整材料排版规则时，只编辑该工厂对应的 `ManufacturerMaterialLayoutSpecCfg`，不会影响其他工厂。
