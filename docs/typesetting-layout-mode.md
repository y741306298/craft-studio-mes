# 排版方式（Typesetting Layout Mode）详细说明

## 1. 背景与目标

为满足不同设备与工艺的排版输出要求，`TypesettingInfo` 新增了可编辑的 `layoutMode` 字段。  
系统会根据 `layoutMode` 自动推导：

- 是否需要输出 `json` 文件
- 是否需要输出 `plt` 文件
- 是否需要输出 `svg` 文件
- 二维码 / 订单号 / 临时码的生成策略
- 定位点形状与策略

该能力用于统一异形切割与 XY 切割在印版生成阶段的配置行为，减少人工配置错误。

---

## 2. 关键数据结构

### 2.1 `TypesettingInfo` 新增字段

| 字段名 | 类型 | 含义 |
|---|---|---|
| `layoutMode` | `String` | 排版方式编码（可编辑） |
| `layoutCategory` | `String` | 排版大类（`shaped_typesetting` 异形排版 / `grid_typesetting` 网格排版） |
| `requireJsonFile` | `Boolean` | 是否需要 JSON 文件 |
| `requirePltFile` | `Boolean` | 是否需要 PLT 文件 |
| `requireSvgFile` | `Boolean` | 是否需要 SVG 文件 |
| `codeGenerateType` | `String` | 二维码/订单号/临时码生成策略 |
| `tempCodeFormat` | `String` | 临时码格式（例如 `xxx`） |
| `anchorPointShape` | `String` | 定位点形状（`circle` / `square` / `none`） |

> 注意：`requireJsonFile`/`requirePltFile`/`requireSvgFile` 与码位字段不建议由前端直接填写，后端会根据 `layoutMode` 自动覆盖写入。

### 2.2 枚举：`TypesettingLayoutMode`

新增枚举 `com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode`，集中管理排版方式到输出策略的映射关系。

---

## 3. 排版方式映射规则

| 枚举值 | 编码 (`layoutMode`) | 排版大类 (`layoutCategory`) | 业务说明 | JSON | PLT | SVG | 码生成方式 | 临时码格式 | 定位点 |
|---|---|---|---|---:|---:|---:|---|---|---|
| `SHAPED_CUTTING_PLT_QR_CIRCLE` | `shaped_cutting_plt_qr_circle` | `shaped_typesetting`（异形排版） | 异形切割（plt二维码）-圆形定位点 | ✅ | ✅ | ✅ | `plt_qr` | `xxx` | `circle` |
| `SHAPED_CUTTING_PLT_QR_SQUARE` | `shaped_cutting_plt_qr_square` | `shaped_typesetting`（异形排版） | 异形切割（plt二维码）-方形定位点 | ✅ | ✅ | ✅ | `plt_qr` | `xxx` | `square` |
| `XY_CUTTING_AUX_LINE_CAIFU_A20PR0` | `xy_cutting_aux_line_caifu_a20pr0` | `grid_typesetting`（网格排版） | xy切割(切割辅助线-裁赋A20PR0） | ✅ | ❌ | ✅ | `side_aux_line` | `null` | `none` |
| `XY_CUTTING_AUX_LINE_CAIFU_A30_SMALL_GRAPH` | `xy_cutting_aux_line_caifu_a30_small_graph` | `grid_typesetting`（网格排版） | xy切割(切割辅助线-裁赋A30小图） | ✅ | ❌ | ✅ | `side_aux_line` | `null` | `none` |
| `XY_CUTTING_AUX_LINE_CAIFU_A30_LARGE_BOARD` | `xy_cutting_aux_line_caifu_a30_large_board` | `grid_typesetting`（网格排版） | xy切割(切割辅助线-裁赋A30大板） | ✅ | ❌ | ✅ | `side_aux_line` | `null` | `none` |
| `XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_FILM` | `xy_cutting_aux_line_caifu_open_back_a30h_film` | `grid_typesetting`（网格排版） | xy切割(切割辅助线-裁赋开背A30H覆膜） | ✅ | ❌ | ✅ | `side_aux_line` | `null` | `none` |
| `XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30H_NO_FILM` | `xy_cutting_aux_line_caifu_open_back_a30h_no_film` | `grid_typesetting`（网格排版） | xy切割(切割辅助线-裁赋开背A30H不覆膜） | ✅ | ❌ | ✅ | `side_aux_line` | `null` | `none` |
| `XY_CUTTING_AUX_LINE_CAIFU_OPEN_BACK_A30PRO_NO_FILM` | `xy_cutting_aux_line_caifu_open_back_a30pro_no_film` | `vertical_typesetting`（竖直排版） | 裁赋开背A30pro不覆膜 | ✅ | ❌ | ✅ | `side_aux_line` | `null` | `none` |
| `XY_CUTTING_AUX_LINE_NINE_SEGMENT` | `xy_cutting_aux_line_nine_segment` | `grid_typesetting`（网格排版） | xy切割(切割辅助线-九段） | ✅ | ❌ | ✅ | `side_aux_line` | `null` | `none` |
| `XY_CUTTING_AUX_LINE_LIUDU` | `xy_cutting_aux_line_liudu` | `grid_typesetting`（网格排版） | xy切割(切割辅助线-六渡） | ✅ | ❌ | ✅ | `side_aux_line` | `null` | `none` |

---

## 4. 自动回填机制（服务端）

### 裁赋开背 A30pro 不覆膜标记规则

- 原 SVG 上扩 3mm、左扩 6mm、右扩 8mm，以扩展后的左上角为原点，不添加上下标签条；扩展后宽度为 `原版宽度 + 14mm`。右侧 B 从 `原版宽度 + 6mm` 延伸到 `原版宽度 + 14mm`，完整落在版面内；增加右扩量不改变 B/D/E 的坐标。
- 元素 A 为宽 3mm、与扩展版面等高的黑色 PNG，放在 `(0, 0)`。
- 元素 C 与 A30H 不覆膜共用参考 y：通过 `caifuMarker_*` 在 nestedSvg 中定位辅助条，取其 `顶部 y + 高度 / 2`，补充顶部参考（成功解析首组时）及扩展版面底边。不读取 gridLines，也不额外加 3mm。
- 每个 y 向上偏移 126mm，在 `x=3mm` 和 `x=6mm+原版宽度` 各放置一个 8mm × 3mm 的黑色 PNG（元素 B）；右侧 B 相对旧版 `原版宽度 + 14mm` 左移 8mm。纵向超出板面的不放置，重复位置只放一对。一个 6mm 高辅助条只使用一个中线参考，其上下边界不会分别生成 B。
- 元素 D 和非出血黑线 E 以 `原版宽度 + 6mm` 为横向参考边界，分别减去原有的 0.5 / 20.3 / 30.3mm 偏移量（0.5mm 仅用于 E），所有 D/E 的 x 相对旧版均左移 5mm，不再额外重复扣减。出血印版的底部标记仍按各自 cell 底边计算。
- D / 非出血黑线按关联子印版的 `haveBlood` 判断，不区分内部零件的主动出血、被出血。至少存在一个出血标记带时，非出血标记带才生成长线 E，其高度为 `max(1, round(子印版高度) - 30)`，并非与子印版严格等高；全部非出血时不放置 D / E。
- 套版间隔、元素排列、辅助标记保存及容器宽度处理沿用 A30H 不覆膜模式。
- 相邻子印版间的 `caifuMarker_*` 辅助条仍为容器宽度、6mm 高；其中黑色横线的左端从 0mm 改为 30mm，长度从 `容器宽度 - 30mm` 改为 `max(1, 容器宽度 - 60mm)`，右端保留原有的 30mm 留白。PNG 和 SVG 同步调整，辅助条中线 y 不变；该内缩仅用于 A30pro。

### 4.1 触发点

在 `TypesettingService` 的以下入口中，后端会调用 `typesettingInfo.applyLayoutModeConfig()`：

- `addTypesetting(...)`
- `updateTypesetting(...)`

### 4.2 行为说明

1. 根据 `layoutMode` 查找 `TypesettingLayoutMode`。
2. 将枚举中的文件输出要求与码位策略回填至 `TypesettingInfo`。
3. 持久化层按回填结果入库。

### 4.3 默认值与异常

- 当 `layoutMode` 为空时，默认使用：
  - `shaped_cutting_plt_qr_circle`
- 当 `layoutMode` 非法（无法匹配枚举）时：
  - 抛出 `IllegalArgumentException`，阻断错误配置进入业务流程。

---

## 5. 创建排版时如何传入 `layoutMode`

在 `AppTypesettingService.layoutConfirm(...)` 中：

- 从 `request.typesettingInfos` 读取第一个 `TypesettingInfo.layoutMode` 作为当前排版任务的模式。
- 若列表为空或 `null`，则保持空值并由领域层按默认模式处理。

> 约定建议：一次排版任务仅使用一种 `layoutMode`，如需混合模式，建议拆分任务。

---

## 6. 持久化兼容性说明

`TypesettingPo` 已补充新增字段映射，支持读写：

- `layoutMode`
- `requireJsonFile`
- `requirePltFile`
- `requireSvgFile`
- `codeGenerateType`
- `tempCodeFormat`
- `anchorPointShape`

历史数据（无新增字段）不会破坏读取；更新后会按新规则回填并持久化。

---

## 7. 接口使用建议（前后端协作）

1. 前端展示“排版方式”下拉，value 使用 `layoutMode` 编码。
2. 前端无需自行推导 `json/plt/svg` 需要性，以后端结果为准。
3. 后续如扩展新设备/工艺，仅需新增枚举项并补充映射。

---

## 8. 示例

### 8.1 请求片段（示意）

```json
{
  "typesettingInfos": [
    {
      "layoutMode": "shaped_cutting_plt_qr_square"
    }
  ]
}
```

### 8.2 落库后关键字段（示意）

```json
{
  "layoutMode": "shaped_cutting_plt_qr_square",
  "layoutCategory": "shaped_typesetting",
  "requireJsonFile": true,
  "requirePltFile": true,
  "requireSvgFile": true,
  "codeGenerateType": "plt_qr",
  "tempCodeFormat": "xxx",
  "anchorPointShape": "square"
}
```

---

## 9. 新增能力：二维码上传与临时码生成

### 9.1 二维码图片生成（不上传）

- 接口：`POST /api/manufacturerSide/typesetting/generateQrCode`
- 入参：
  - `manufacturerMetaId`：工厂/制造商 ID
  - `content`：二维码扫码内容（`string`）
- 处理逻辑：
  1. 服务端按 `content` 生成 PNG 二维码（512x512）。
  2. 直接返回二维码图片的 Base64 内容（`qrCodeBase64`），由调用方自行处理展示或上传。

返回示例：

```json
{
  "manufacturerMetaId": "RMF_001",
  "content": "ORDER-20260420-0001",
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

### 9.2 临时码生成（循环号码池）

- 接口：`POST /api/manufacturerSide/typesetting/generateTempCode`
- 入参：
  - `manufacturerMetaId`：工厂/制造商 ID
- 规则：
  1. 每个 `manufacturerMetaId` 维护一个 `1..100000` 的循环队列。
  2. 每次从队首取一个数字，使用后放到队尾，保证号码可循环复用。
  3. 临时码格式为该数字本身（`xxx`）。

返回示例：

```json
{
  "manufacturerMetaId": "RMF_001",
  "codeNumber": 1,
  "tempCode": "1"
}
```

---

## 10. Nesting 回调结果字段补充

`TypesettingInfo.elements` 会保存排版算法回调结果，包含：

- `nestedSvg`
- `utilization`
- `width`
- `height`

并在回调落库时同步写入 `TypesettingElement`，用于后续排版尺寸展示或设备侧计算。

示例：

```json
{
  "nestedSvg": "test/nest/nested1.svg",
  "utilization": 0.636334,
  "width": 1200,
  "height": 800
}
```
