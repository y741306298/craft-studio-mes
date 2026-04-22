# 排版模块近期改动说明（toLayout / callback / list 查询）

## 1. 背景
本次改动围绕排版链路的三个关键问题展开：

1. **toLayout 提交后数量一致性**：
   - 生产零件在工序流中“待排版”到“排版中”的数量流转；
   - 引用历史印版（`TypesettingInfo`）时 `leaveQuantity` 的实时扣减。
2. **回调结果可追溯性**：
   - 通过解析 `nestedSvg`，识别每个回调结果实际用到了哪些来源单元；
   - 将来源信息持久化到 `TypesettingInfo.typesettingCells`。
3. **列表查询业务准确性**：
   - 不再依赖数据库分页查询结果，改为全量拉取后按业务规则过滤：
     - 零件：仅保留“待排版”节点数量 > 0；
     - 印版：仅保留 `leaveQuantity > 0`。

---

## 2. 数据模型调整

### 2.1 新增统一来源单元 VO
新增 `TypesettingSourceCell`：

- `sourceType`
- `sourceId`
- `orderItemId`
- `quantity`

用于统一表达来源，不再拆分旧的 piece/typesetting 两类 cell。

### 2.2 TypesettingInfo / TypesettingPo 字段统一
- `TypesettingInfo.typesettingCells` 改为 `List<TypesettingSourceCell>`；
- 移除旧的 `pieceCells`（PO/DO 映射同步调整）。

---

## 3. toLayout 行为改动

### 3.1 成功提交异步排版后数量维护
在 `generateGridNestedFilesAsync` / `generateNestedFilesAsync` 受理后：

1. 对本次参与的 `ProductionPiece`：
   - 调用 `transferPieceQuantityBetweenNodes`，将数量从 `NODE_TYPESETTING` 转到 `NODE_TYPESETTING_IN_PROGRESS`。
2. 对本次引用的 `TypesettingInfo`：
   - 按本次 `quantity` 扣减 `leaveQuantity` 并落库。

### 3.2 新建排版记录保存来源
新建 `TypesettingInfo` 时，将请求中的 `typesettingCells` 转为 `TypesettingSourceCell` 并保存，作为本次排版提交的来源快照。

---

## 4. callback 行为改动

### 4.1 回调结果字段落库
`NestingResponse` 支持以下结构并兼容落库：

- 新结构：`containerSize.width/height`
- 旧结构：`width/height`

落库策略：
1. `nestedSvg`（保存前进行 OSS 完整 URL 归一化）
2. `utilization`
3. `width/height`（优先 `containerSize`，兜底旧字段）

### 4.2 nestedSvg 来源解析
对每个 `results[i].nestedSvg`：

1. 先补全 OSS URL；
2. 下载或读取到**临时文件**；
3. 解析 SVG 中 `data-source-index` 出现次数；
4. 结合 Redis 中缓存的原始 `LayoutConfirmRequest.typesettingCells`，映射出本结果实际使用到的来源与数量；
5. 保存到当前 `TypesettingInfo.typesettingCells`；
6. 解析完成后删除临时文件。

---

## 5. OSS 兼容策略

### 5.1 配置读取优先级
优先读取：

- `ali-cloud.oss.endpoint`
- `ali-cloud.oss.raw-bucket`

并兼容旧配置：

- `spring.cloud.alicloud.oss.endpoint`
- `spring.cloud.alicloud.oss.bucket-name`

### 5.2 URL 补全规则
与 `ProcedureService.buildCompleteOssUrl` 逻辑一致：

1. 若已是 `http/https`，直接返回；
2. 相对路径去掉前导 `/`；
3. 组装为：`https://{bucket}.{endpoint}/{path}`。

---

## 6. listTypesettingAndProductionPieces 查询策略改动

### 6.1 查询方式
不再依赖分页查询作为业务结果来源，而是：

1. 基于 `manufacturerId` + 其他筛选条件拉取全量（`1, Integer.MAX_VALUE`）；
2. 内存过滤；
3. 最后按原接口分页参数做内存分页返回。

### 6.2 过滤规则
- **ProductionPiece**：仅保留“待排版”节点 `pieceQuantity > 0`。
- **TypesettingInfo**：仅保留 `leaveQuantity > 0`。

### 6.3 计数规则
`countPartsOnly` 与 `countTypesettingOnly` 改为基于过滤后结果 `size()`，保证 total 与实际返回一致。

---

## 7. 风险与注意事项

1. **性能风险**：全量拉取 + 内存过滤在数据量大时会带来压力，需要结合后续数据规模评估是否下推过滤到仓储层。
2. **回调可达性**：若 `nestedSvg` 对象不可下载或路径异常，将导致来源解析为空（但不会阻塞回调主流程）。
3. **配置一致性**：环境需保证 OSS bucket/endpoint 配置正确，否则相对路径补全后的下载会失败。

---

## 8. 建议测试点

1. toLayout 传入混合来源（零件 + 历史印版），验证：
   - 节点数量流转；
   - leaveQuantity 扣减。
2. callback 使用 `containerSize` 返回，验证 `TypesettingInfo.element` 四字段：
   - nestedSvg
   - utilization
   - width
   - height
3. callback 的 `nestedSvg` 分别使用：
   - 完整 URL
   - 相对路径
   验证来源解析与临时文件删除。
4. 列表接口验证：
   - 零件仅返回“待排版”数量 > 0
   - 印版仅返回 leaveQuantity > 0
   - total 与分页条目一致。
