# TypesettingController 接口文档

本文档根据 `TypesettingController` 当前代码整理，包含每个接口的用途、请求参数、字段说明与响应结构。

- Controller 前缀：`/api/manufacturerSide/typesetting`
- 统一返回：`ApiResponse<T>`

## 统一响应结构（ApiResponse）

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 业务状态码，默认 `200`；常见还有 `400/401/404/500` |
| message | string | 响应消息，默认 `success` |
| data | any | 业务数据 |
| timestamp | long | 响应时间戳（毫秒） |

---

## 1) 统一查询排版和生产工件

- **URL**: `POST /list`
- **说明**: 按条件查询“待排版工件 + 排版记录”的统一列表。

### 请求体：`TypesettingQuery`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| manufacturerMetaId | string | 否（建议传） | 厂商 ID |
| queryType | string | 否 | 查询类型：`ALL`（全部）/`PART`（零件）/`TYPESETTING`（排版） |
| status | string | 否 | 排版状态码：`pending`/`in_progress`/`confirming`/`printing`/`failed` |
| material | string | 否 | 材料名称（筛选） |
| nodeName | string | 否 | 工艺节点名称（筛选） |
| startDate | date-time | 否 | 开始时间 |
| endDate | date-time | 否 | 结束时间 |

### 响应：`ApiResponse<List<TypesettingProductionPieceVO>>`

`data` 为列表，每项字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | string | 记录主键（可能是工件记录 ID 或排版记录 ID） |
| orderItemId | string | 订单项 ID |
| quantity | int | 数量 |
| leaveQuantity | int | 剩余数量 |
| material | string | 材料名称 |
| materialCode | string | 材料编码 |
| processingFlow | string | 工艺流程 |
| previewUrl | string | 预览图 URL |
| remark | string | 备注 |
| sourceType | string | 来源类型（如 PART / TYPESETTING） |
| sourceId | string | 来源业务 ID（工件 ID 或排版 ID） |
| status | string | 状态码 |
| maskSvg | string | 排版轮廓 SVG |
| layoutMode | string | 排版方式编码 |
| materialConfigs | array<string> | 排版记录使用的物料编码列表 |
| templateCode | string | 工件模板 SVG 编码 |

---

## 2) 查询待确认排版（分页）

- **URL**: `GET /confirming/list`
- **说明**: 查询状态为 `confirming` 的排版记录。

### Query 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| manufacturerMetaId | string | 是 | 厂商 ID |
| current | int | 否 | 当前页，默认 `1` |
| size | int | 否 | 每页大小，默认 `20` |

### 响应

- 返回类型：`ApiResponse<PagedResult<TypesettingInfo>>`
- `data` 中的记录为 `TypesettingInfo`，常用字段如下：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | string | 主键 |
| manufacturerMetaId | string | 厂商 ID |
| typesettingId | string | 排版业务 ID |
| status | string | 排版状态 |
| quantity | int | 总数量 |
| leaveQuantity | int | 剩余数量 |
| materialConfigs | array<string> | 物料编码列表 |
| typesettingCells | array<object> | 参与排版来源单元 |
| maskSvg | string | 排版轮廓 SVG |
| layoutMode | string | 排版方式 |
| layoutCategory | string | 排版大类：`shaped_typesetting` / `grid_typesetting` |
| requireJsonFile | boolean | 是否需要 json 文件 |
| requirePltFile | boolean | 是否需要 plt 文件 |
| requireSvgFile | boolean | 是否需要 svg 文件 |
| codeGenerateType | string | 码位生成策略（例如 `plt_qr` / `side_aux_line`） |
| tempCodeFormat | string | 临时码格式 |
| anchorPointShape | string | 定位点形状：`circle`/`square`/`none` |
| marks | object<string,string> | 排版附加标记资源映射 |
| element | object | 算法结果（`nestedSvg`、`utilization`、`width`、`height` 等） |

---

## 3) 开始排版

- **URL**: `POST /toLayout`
- **说明**: 校验材料/工艺并触发排版。

### 请求体：`LayoutConfirmRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| manufacturerMetaId | string | 否（建议传） | 厂商 ID |
| typesettingCells | array<TypesettingProductionPieceVO> | 是 | 参与排版的单元列表 |
| containers | array<ContainerInfo> | 否 | 容器尺寸列表 |
| layoutMode | string | 否 | 排版方式（决定调用异形/网格排版） |

`ContainerInfo` 字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| width | int | 否 | 容器宽 |
| height | int | 否 | 容器高 |

### 响应：`ApiResponse<LayoutConfirmResult>`

| 字段 | 类型 | 说明 |
|---|---|---|
| success | boolean | 是否成功 |
| layoutId | string | 生成的排版 ID |
| layoutUrl | string | 排版结果地址 |
| productionPieceCount | int | 处理的工件数量 |
| message | string | 失败或补充说明 |

---

## 4) 确认排版

- **URL**: `POST /confirmLayout`
- **说明**: 传入 `TypesettingInfo`（至少需要 `id`），确认排版并触发后续版式生成。

### 请求体

- 类型：`TypesettingInfo`
- 关键字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | string | 是 | 排版记录主键 |
| layoutMode | string | 否 | 可选覆盖排版方式 |

### 响应

- `ApiResponse<LayoutConfirmResult>`（字段与“开始排版”接口相同）。
- 当 `success=false` 时，接口会返回 `code=400` 和具体 `message`。

---

## 5) 确认打印

- **URL**: `POST /confirmPrint`
- **说明**: 将排版状态推进为待打印。

### 请求体：`ConfirmPrintRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | string | 是 | 排版 ID（不能为空） |
| deviceInfoId | string | 是 | 设备编号（不能为空） |
| layoutMode | string | 否 | 可选覆盖排版方式 |
| productionPieceIds | array<string> | 否 | 该字段主要给“开始打印”接口复用 |

### 响应：`ApiResponse<ConfirmPrintResult>`

| 字段 | 类型 | 说明 |
|---|---|---|
| success | boolean | 是否成功 |
| message | string | 结果说明 |
| updatedPieceCount | int | 更新的工件数量 |
| updatedPieceIds | array<string> | 更新的工件 ID 列表 |

---

## 6) 开始打印

- **URL**: `POST /startPrint`
- **说明**: 将工件状态推进为打印中。

### 请求体：`ConfirmPrintRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| productionPieceIds | array<string> | 是 | 生产工件 ID 列表（不能为空） |
| id | string | 否 | 本接口不使用 |
| deviceInfoId | string | 否 | 本接口不使用 |
| layoutMode | string | 否 | 预留字段 |

### 响应

- `ApiResponse<ConfirmPrintResult>`（字段同“确认打印”）。

---

## 7) 释放排版

- **URL**: `POST /releaseLayout`
- **说明**: 删除排版文件并回退相关零件状态。

### 请求体：`ReleaseLayoutRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| typesettingIds | array<string> | 是 | 排版 ID 列表（不能为空） |

### 响应：`ApiResponse<ReleaseLayoutResult>`

| 字段 | 类型 | 说明 |
|---|---|---|
| success | boolean | 是否成功 |
| message | string | 结果说明 |
| releasedPieceCount | int | 回退状态的零件数量 |
| releasedPieceIds | array<string> | 回退零件 ID 列表 |
| deletedLayoutIds | array<string> | 删除的排版 ID 列表 |

---

## 8) 生成二维码（Base64）

- **URL**: `POST /generateQrCode`

### 请求体：`GenerateQrCodeRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| manufacturerMetaId | string | 是 | 制造商 ID |
| content | string | 是 | 二维码内容 |

### 响应：`ApiResponse<GenerateQrCodeResult>`

| 字段 | 类型 | 说明 |
|---|---|---|
| manufacturerMetaId | string | 制造商 ID |
| content | string | 二维码内容 |
| qrCodeBase64 | string | PNG 的 Base64 内容 |

---

## 9) 生成临时码

- **URL**: `POST /generateTempCode`

### 请求体：`GenerateTempCodeRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| manufacturerMetaId | string | 是 | 制造商 ID |

### 响应：`ApiResponse<GenerateTempCodeResult>`

| 字段 | 类型 | 说明 |
|---|---|---|
| manufacturerMetaId | string | 制造商 ID |
| codeNumber | long | 数值码 |
| tempCode | string | 格式化后的临时码 |

---

## 10) 查询全部排版方式配置

- **URL**: `GET /layoutModes`
- **说明**: 返回 `TypesettingLayoutMode` 枚举的全部配置项，便于前端构建排版方式下拉和联动配置。

### 请求参数

- 无

### 响应：`ApiResponse<List<TypesettingLayoutModeVO>>`

`data` 为数组，每项字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| code | string | 排版方式编码 |
| description | string | 排版方式描述 |
| layoutCategory | string | 排版大类：`shaped_typesetting` / `grid_typesetting` |
| requireJsonFile | boolean | 是否需要 json 文件 |
| requirePltFile | boolean | 是否需要 plt 文件 |
| requireSvgFile | boolean | 是否需要 svg 文件 |
| codeGenerateType | string | 码位生成策略（如 `plt_qr` / `side_aux_line`） |
| tempCodeFormat | string | 临时码格式（例如 `xxx`） |
| anchorPointShape | string | 定位点形状（`circle` / `square` / `none`） |

---

## 11) 异形排版算法回调

- **URL**: `POST /callback/generate_nested_files`
- **说明**: 接收异形排版算法回调。

### 请求体：`NestingResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| error | string | 错误信息 |
| status | string | 状态 |
| id | string | 回调任务 ID |
| results | array<Result> | 回调结果列表 |

`Result` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| nestedSvg | string | 排版结果 SVG |
| utilization | decimal | 利用率 |
| width | decimal | 宽（兼容历史字段） |
| height | decimal | 高（兼容历史字段） |
| containerSize | object | 当前主返回容器尺寸 |
| gridLines | object | 网格线信息（xs/ys） |

`containerSize` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| width | decimal | 容器宽 |
| height | decimal | 容器高 |

`gridLines` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| xs | array<number> | x 轴网格线坐标 |
| ys | array<number> | y 轴网格线坐标 |

### 响应

- `ApiResponse<String>`，`data` 为：`"回调处理成功"`。

---

## 12) 网格排版算法回调

- **URL**: `POST /callback/generate_grid_nested_files`
- **说明**: 接收网格排版算法回调。

### 请求体 / 响应

- 与“异形排版算法回调”一致：`NestingResponse` -> `ApiResponse<String>`。

---

## 13) 版式生成算法回调

- **URL**: `POST /callback/generate_forme`
- **说明**: 接收版式生成回调（当前返回“回调处理待续”）。

### 请求体：`FormeGenerationResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| error | string | 错误信息 |
| status | string | 状态 |
| id | string | 任务 ID |
| result | object | 回调结果 |

`result` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| json | string | JSON 文件对象名/地址 |
| plt | object | PLT 对象名 |
| formeSvg | string | 版式 SVG |

`plt` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| normal | string | 正向 PLT 对象名 |
| reverse | string | 反向 PLT 对象名 |

### 响应

- `ApiResponse<String>`，`data` 为：`"回调处理待续"`。

---

## 附录：layoutMode 可选值（建议）

`layoutMode` 由 `TypesettingLayoutMode` 枚举维护，常用值如下：

- `shaped_cutting_plt_qr_circle`
- `shaped_cutting_plt_qr_square`
- `xy_cutting_aux_line_caifu_a20pr0`
- `xy_cutting_aux_line_caifu_a30_small_graph`
- `xy_cutting_aux_line_caifu_a30_large_board`
- `xy_cutting_aux_line_caifu_open_back_a30h_film`
- `xy_cutting_aux_line_caifu_open_back_a30h_no_film`
- `xy_cutting_aux_line_nine_segment`
- `xy_cutting_aux_line_liudu_large_board`
- `xy_cutting_aux_line_liudu_small_graph`
- `xy_cutting_aux_line_full_auto_buckle`

> 说明：当 `layoutMode` 为空时，系统默认 `shaped_cutting_plt_qr_circle`。
