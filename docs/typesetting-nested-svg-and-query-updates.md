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

### 3.1 入参精简

`POST /api/manufacturerSide/typesetting/toLayout` 使用专用的 `ToLayoutRequest`，现有 JSON 字段名保持不变。
`typesettingCells` 不再复用包含大量列表展示字段的 `TypesettingProductionPieceVO`，改用专门的
`ToLayoutCellRequest`，只定义 `sourceType`、`sourceId`、`quantity` 三个入参字段。调用方只应提交下面这些字段：

`toLayout` 内部仍保留原有的 `toProductionPiece()` / `toTypesettingInfo()` 转换：精简 cell 转换到内部 VO 后，
`sourceId` 分别映射为领域对象 ID，`quantity` 映射为本次排版数量；服务端随后按 ID 查询完整数据库记录。
因此三个 cell 字段可以覆盖这两个转换方法在 `toLayout` 中实际会用到的字段。

| 字段 | 必填 | 是否直接使用入参 | 说明 |
| --- | --- | --- | --- |
| `manufacturerMetaId` | 是 | 是 | 用于生成排版任务 ID、上传目录以及查询工厂排版配置 |
| `layoutMode` | 是 | 是 | 决定排版算法、间距和回调地址 |
| `typesettingCells` | 是，至少一项 | 是 | 本次参与排版的来源列表 |
| `typesettingCells[].sourceType` | 是 | 是 | 仅支持 `PRODUCTION_PIECE`（生产零件）和 `TYPESETTING`（历史印版） |
| `typesettingCells[].sourceId` | 是 | 是 | 生产零件或历史印版的数据库 ID |
| `typesettingCells[].quantity` | 是，且大于 0 | 是 | 本次占用并提交算法的数量；留白零件会按业务规则修正为 1 |
| `containers` | 否 | 是 | 排版容器规格；不传时使用默认规格 `1500 × 1000` |
| `containers[].width` | `containers` 项存在时需要 | 是 | 容器宽度，后端还会按工艺配置进行内缩 |
| `containers[].height` | `containers` 项存在时需要 | 是 | 容器高度，后端还会按工艺配置进行内缩 |

最小请求示例：

```json
{
  "manufacturerMetaId": "factory-id",
  "layoutMode": "layout-mode-code",
  "typesettingCells": [
    {
      "sourceType": "PRODUCTION_PIECE",
      "sourceId": "production-piece-id",
      "quantity": 2
    }
  ]
}
```

下列属性不再属于 `toLayout` 入参。后端根据 `sourceType + sourceId` 查询 `ProductionPiece` 或
`TypesettingInfo` 后取得，避免客户端回传的列表快照过期或被篡改：

- 材料：`materialConfig`、`materialConfigs`、`oriName`、`oriMaterialId`、`oriMaterialType`；
- 工艺：`processingFlow`、`procedureFlow`；
- 排版素材和尺寸：`templateCode`、`maskSvg`、`previewUrl`、`width`、`height`；
- 状态和业务标记：`status`、`isUrgent`、`isRedo`、`haveBlood`、`leaveQuantity`；
- 展示与关联信息：`id`、`groupId`、`orderItemId`、`remark`、`description`、`createTime`、
  `typesettingCells`（来源印版的历史来源明细）。

其中 `orderItemId`、`isRedo`、`haveBlood` 会在查询后回填到服务端请求快照，供异步回调和来源追踪使用；
这些字段不是客户端输入。

### 3.2 完成校验后立即维护数量
在所有 `toLayout` 校验完成、调用 `generateGridNestedFilesAsync` / `generateNestedFilesAsync` 之前：

1. 对本次参与的 `ProductionPiece`：
   - 调用严格数量划转，将数量从 `NODE_TYPESETTING` 转到 `NODE_TYPESETTING_IN_PROGRESS`；任一零件的待排版数量或目标节点容量不足时，整批拒绝排版。
2. 对本次引用的 `TypesettingInfo`：
   - 按本次 `quantity` 扣减 `leaveQuantity` 并落库。

提前占用来源数量，确保异步排版请求执行期间，相同零件或历史印版不会被再次提交排版。

### 3.3 新建排版记录保存来源
新建 `TypesettingInfo` 时，将请求中的 `typesettingCells` 转为 `TypesettingSourceCell` 并保存，作为本次排版提交的来源快照。

### 3.4 异步算法调用次数与重复提交

单次进入后端的 `toLayout` 调用只会执行一次异步排版算法：代码按 `layoutMode.layoutCategory` 进入一个
`switch` 分支，并在每个分支调用一种算法后立即 `break`，不存在同一次方法执行同时调用通用和专用算法的路径。

如果观察到两条异步算法调用记录，应结合两条记录的 `callbackCustomValue.id`（即本次生成的 `typesettingId`）判断：

- ID 相同：需要继续排查算法服务、网关或日志采集侧是否重复记录/执行；MES 的一次方法执行只发出一次 HTTP 请求；
- ID 不同：说明 MES 实际收到了两次 `toLayout` 请求，常见来源是前端重复点击、客户端超时重试或网关重试。

来源锁用于防止两个请求**同时**处理同一来源，但第一个请求完成后会释放，并不提供跨请求幂等。可排版数量校验也不是
重复请求校验：例如待排版数量为 10、每次请求数量为 1 时，两个串行请求都会正常通过，并分别占用 1；只有后一个请求的
数量大于当时剩余的待排版数量时才会失败。历史印版同理，按当时的 `leaveQuantity` 判断。

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
