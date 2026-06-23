# 材料排版规格配置说明

## 背景

材料排版规格配置用于描述不同材料在排版时需要按照长度阶梯执行的内缩规则。当前设计将“材料规格”和“工厂绑定”拆成两个配置域：

1. **材料排版规格（MaterialLayoutSpec）**：保存材料维度的公共配置。材料种类较少，因此只维护一份材料规格，避免按工厂重复录入。
2. **工厂材料排版规格配置（ManufacturerMaterialLayoutSpecCfg）**：保存工厂与材料排版规格的绑定关系。工厂数量较多时，只需要新增绑定关系即可复用已有材料规格。

## 领域模型

### MaterialLayoutSpec

| 字段 | 说明 |
| --- | --- |
| `id` | 材料排版规格配置 ID。 |
| `materialId` | 材料 ID，参考订单明细中的 `orderItem.material.materialId`。 |
| `materialSnapshot` | 材料快照，参考订单明细中的 `orderItem.material.materialSnapshot`，用于保留配置时的材料名称、属性等展示信息。 |
| `usageSize3D` | 材料使用尺寸，参考订单明细中的 `orderItem.material.usageSize3D`。 |
| `insetSteps` | 阶梯内缩数据，必须覆盖 1m 到 10m。 |

### MaterialLayoutSpecStep

| 字段 | 说明 | 示例 |
| --- | --- | --- |
| `maxLengthMeter` | 长度阶梯上限，单位：米。 | `1` 表示 1m 内。 |
| `insetCm` | 当前阶梯内缩值，单位：厘米。 | `2.5` 表示内缩 2.5cm。 |

> 说明：应用服务会校验 `insetSteps` 中必须有且仅需覆盖 `maxLengthMeter = 1..10` 的阶梯值，避免配置缺失导致排版计算结果不稳定。

### ManufacturerMaterialLayoutSpecCfg

| 字段 | 说明 |
| --- | --- |
| `id` | 工厂材料排版规格绑定配置 ID。 |
| `manufacturerMetaId` | 制造商/工厂元数据 ID。 |
| `materialLayoutSpecId` | 关联的材料排版规格配置 ID。 |

## 接口说明

### 材料排版规格接口

Controller：`MaterialLayoutSpecController`

基础路径：`/api/configSide/materialLayoutSpec`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/list` | 分页查询材料排版规格，支持按 `materialId`、`materialName` 筛选。 |
| `GET` | `/{id}` | 查询材料排版规格详情。 |
| `POST` | `/add` | 新增材料排版规格。 |
| `POST` | `/edit` | 编辑材料排版规格。 |
| `GET` / `DELETE` | `/delete/{id}` | 删除材料排版规格。 |

#### 新增/编辑请求示例

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

### 工厂材料排版规格绑定接口

Controller：`ManufacturerMaterialLayoutSpecCfgController`

基础路径：`/api/configSide/manufacturerMaterialLayoutSpecCfg`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/list` | 分页查询工厂与材料排版规格绑定关系，支持按 `manufacturerMetaId`、`materialLayoutSpecId` 筛选。 |
| `GET` | `/{id}` | 查询绑定详情，响应中会带出关联的材料排版规格详情。 |
| `POST` | `/add` | 新增工厂与材料排版规格绑定关系。 |
| `POST` | `/edit` | 编辑工厂与材料排版规格绑定关系。 |
| `GET` / `DELETE` | `/delete/{id}` | 删除工厂与材料排版规格绑定关系。 |

#### 新增/编辑请求示例

```json
{
  "id": "编辑时传入，新增不传",
  "manufacturerMetaId": "manufacturer_001",
  "materialLayoutSpecId": "material_layout_spec_001"
}
```

## 配置流程建议

1. 先通过 `/api/configSide/materialLayoutSpec/add` 维护材料维度的排版规格和 1m 到 10m 阶梯内缩数据。
2. 再通过 `/api/configSide/manufacturerMaterialLayoutSpecCfg/add` 将工厂绑定到已有材料排版规格。
3. 后续同类材料规格变更时，只更新 `MaterialLayoutSpec`，所有绑定该规格的工厂都可复用最新配置。
