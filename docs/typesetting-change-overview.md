# 排版能力改造整体说明（本轮改动汇总）

本文档汇总本次排版模块改造的**全部核心变更**，覆盖数据结构、映射规则、接口、方法、持久化和调用链，便于产品、前后端、算法和测试统一对齐。

---

## 1. 改造目标

本次改造围绕以下目标展开：

1. 支持可编辑的排版方式（`layoutMode`），并由后端自动推导排版相关配置，降低前端与人工配置错误。
2. 引入排版方式的大类（`layoutCategory`）：
   - `shaped_typesetting`（异形排版）
   - `grid_typesetting`（网格排版）
3. 持久化排版算法回调关键信息：`nestedSvg`、`utilization`、`width`、`height`。
4. 提供二维码生成能力（不上传）。
5. 提供临时码循环生成能力（按工厂维护 `1..100000` 号码池，队首取号后放回队尾）。

---

## 2. 关键模型与字段变更

## 2.1 `TypesettingInfo`（排版主实体）

新增/增强字段：

- `layoutMode`：排版方式编码（可编辑）。
- `layoutCategory`：排版大类（异形/网格）。
- `requireJsonFile` / `requirePltFile` / `requireSvgFile`：按排版方式自动推导。
- `codeGenerateType`：码位生成策略（如 `plt_qr` / `side_aux_line`）。
- `tempCodeFormat`：临时码格式（当前统一 `xxx`）。
- `anchorPointShape`：定位点形状（`circle` / `square` / `none`）。
- `elements`：排版算法回调结果列表，保存：
  - `nestedSvg`
  - `utilization`
  - `width`
  - `height`

方法：

- `applyLayoutModeConfig()`：根据 `layoutMode` 自动回填 `layoutCategory` 与所有派生配置字段。

## 2.2 `TypesettingLayoutMode`（排版方式枚举）

职责：集中定义 `layoutMode -> 配置` 的映射，包含：

- `code`
- `description`
- `layoutCategory`
- 文件输出要求（json/plt/svg）
- `codeGenerateType`
- `tempCodeFormat`
- `anchorPointShape`

当前内置模式：

1. `shaped_cutting_plt_qr_circle`（异形排版）
2. `shaped_cutting_plt_qr_square`（异形排版）
3. `xy_cutting_aux_line_caifu`（网格排版）
4. `xy_cutting_aux_line_nine_segment`（网格排版）
5. `xy_cutting_aux_line_liudu`（网格排版）

默认/校验规则：

- `layoutMode` 为空 -> 默认 `shaped_cutting_plt_qr_circle`
- `layoutMode` 无法匹配 -> 抛出 `IllegalArgumentException`

## 2.3 `TypesettingElement`（排版结果元素）

字段扩展：

- `nestedSvg`
- `utilization`
- `width`
- `height`

用于承接算法回调中的排版结果细项。

## 2.4 `NestingResponse.Result`（算法回调 DTO）

字段扩展：

- `width`
- `height`

并在回调处理时映射到 `TypesettingElement`。

---

## 3. 自动回填与持久化行为

## 3.1 自动回填入口

在领域服务 `TypesettingService` 中：

- `addTypesetting(...)`
- `updateTypesetting(...)`

都会调用 `typesettingInfo.applyLayoutModeConfig()`，确保每次新增/更新都按 `layoutMode` 统一回填派生配置。

## 3.2 持久化映射

`TypesettingPo` 已补充新增字段映射，支持读写：

- `layoutMode`
- `layoutCategory`
- `requireJsonFile`
- `requirePltFile`
- `requireSvgFile`
- `codeGenerateType`
- `tempCodeFormat`
- `anchorPointShape`

`elements` 中的 `nestedSvg/utilization/width/height` 随 `TypesettingInfo` 一并持久化。

---

## 4. 业务方法与接口新增

## 4.1 二维码生成（不上传）

服务方法：

- `AppTypesettingService.generateQrCode(GenerateQrCodeRequest request)`

核心流程：

1. 校验 `manufacturerMetaId` 和 `content`。
2. 使用 ZXing 生成二维码 PNG（512x512）。
3. 返回二维码 PNG 的 Base64 内容。
4. 返回 `GenerateQrCodeResult`：
   - `manufacturerMetaId`
   - `content`
   - `qrCodeBase64`

接口：

- `POST /api/manufacturerSide/typesetting/generateQrCode`

请求 DTO：

- `GenerateQrCodeRequest`
  - `manufacturerMetaId`
  - `content`

响应 VO：

- `GenerateQrCodeResult`

## 4.2 临时码循环生成

服务方法：

- `AppTypesettingService.generateTempCode(GenerateTempCodeRequest request)`

核心规则：

1. 每个 `manufacturerMetaId` 在 Redis 维护一个 `1..100000` 队列。
2. 每次通过 Lua 脚本执行原子操作：`LPOP` 取队首 -> `RPUSH` 放队尾。
3. 临时码格式为数字本身（`xxx`），不再拼接 `n/m`。

接口：

- `POST /api/manufacturerSide/typesetting/generateTempCode`

请求 DTO：

- `GenerateTempCodeRequest`
  - `manufacturerMetaId`

响应 VO：

- `GenerateTempCodeResult`
  - `manufacturerMetaId`
  - `codeNumber`
  - `tempCode`

---

## 5. 回调落库链路

方法：

- `AppTypesettingService.handleNestingCallback(NestingResponse response)`

行为：

1. 根据回调 `id` 查找排版记录。
2. 回调成功时将状态改为确认中。
3. 将每个 `NestingResponse.Result` 映射为 `TypesettingElement`：
   - `nestedSvg`
   - `utilization`
   - `width`
   - `height`
4. 写入 `typesettingInfo.elements` 并更新入库。

---

## 6. 依赖变更

根 `pom.xml` 新增：

- `com.google.zxing:core`
- `com.google.zxing:javase`

---

## 7. 文档与协作建议

建议前端只提交 `layoutMode`，其他派生字段以后端回填结果为准。  
若后续新增设备/排版策略，优先扩展 `TypesettingLayoutMode`，不要在前端硬编码规则。

---

## 8. 验收清单（建议）

1. 验证 5 种 `layoutMode` 对应的 `layoutCategory`、文件要求与码位策略是否一致。
2. 验证回调后 `elements` 中是否完整保存 `nestedSvg/utilization/width/height`。
3. 验证二维码接口是否能生成并返回可解码的 Base64 图片数据。
4. 验证临时码是否按工厂维度循环复用（取号后回队尾）。
5. 验证历史数据兼容（旧记录无新增字段时读取不报错）。
