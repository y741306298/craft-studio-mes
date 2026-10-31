# addPkg 异步状态更新与恢复

## 返回与客户端约定

`addPkg` 同步完成：持久化请求身份与订单占用 → 从数据库校验零件和数量 → 获取/复用面单 → 持久化包裹、打印结果和任务执行租约 → 同步批量更新本次零件的数量及完成状态 → 原子标记 quantitiesApplied 并排队汇总任务 → 返回打印响应。

成功返回前，本次生产件的待打包/已打包数量、包裹关联和单件完成状态已经落库。完全打包的零件标记已完成，并清零其他工序节点的残留数量，因此 listTypesettingAndProductionPieces、listTypesettingFilterOptions、listTypesettingAndProductionPiecesById 以及待打包列表不会在后台汇总前继续匹配这些零件。部分打包仅迁移本次数量，保留仍需排版/生产/打包的剩余数量，不会把整件提前隐藏。

订单项完成状态、关联正常零件的汇总完成/加急标记仍由后台处理；汇总仍需满足订单项阈值，并额外确保物理零件本身已全部打包，避免后台再次把部分打包零件提前完成。返回成功不表示后台汇总已完成，也不表示纸质面单已经打印。单件完成以该生产件 quantity 为准，订单项数量只在生产件缺失 quantity 时作为回退，不能因为订单项阈值较小就提前完成部分打包的生产件。

请求新增 `requestId`：每次新的打包操作生成 UUID；网络超时重试必须复用原值。相同 requestId + 相同内容仅在 quantitiesApplied=true 或旧任务已 DONE 时返回已保存结果，不再次打印；若正在同步写入或部分失败等待恢复，则提示处理中而不返回打包成功；相同 requestId + 不同内容拒绝。已明确失败的请求保留失败记录；修正后重新发起操作需使用新 requestId，不会复用旧任务身份重新占用订单。

旧客户端不传 requestId 时，服务端按订单、零件和数量、物流、路线和打印设备等内容生成确定性键，相同内容将重放原结果。**有意再次打相同零件和数量的另一包，必须传新的 requestId**。建议前端在每次用户确认打包时生成并保存 UUID，不在每次 HTTP 重试时重新生成。

响应新增 `packagingTaskId`、`packagingState`。使用以下接口查询最新状态：

```
GET /api/manufacturerSide/deliveryPkg/packaging-task/{taskId}?manufacturerMetaId=...
```

## 占用与状态机

采用保守的**订单级独占预占**：MongoDB `deliveryPackagingOrderLock` 以工厂+订单的摘要作为 `_id`。一个订单在所有实例上只允许一个未完成的打包任务；任务完成或明确无副作用的失败后才释放。手动注销、释放包裹也经过同一个占用入口，避免与后台更新竞态。

接口取得订单占用后重新读取、校验数据库中的数量；面单和包裹保存后，在返回成功前执行本次零件的节点数量迁移。占用仍保留至后台汇总完成，期间拒绝同订单后续打包并返回占用任务 ID，避免汇总操作竞态。不同订单可独立受理。

持久化任务集合 `deliveryPackagingTask`：

```
PREPARING → PROCESSING（本次零件同步更新）→ READY → PROCESSING（后台汇总）→ DONE
                         ↘ RETRY（恢复未完成的更新）↗
PREPARING / PROCESSING → REVIEW（需核对）
PREPARING → FAILED（尚无副作用，释放占用）
REVIEW → CLOSED（客服确认已核对/处理副作用后关闭）
```

- 包裹结果先与 PROCESSING 执行租约原子保存，再更新本次零件；全部本次零件写入成功后，在同一个任务文档原子设置 quantitiesApplied=true、state=READY。后台不能在同步执行租约有效时抢占任务，返回成功前本次零件更新必须已经完成。
- 同步更新失败或部分 bulkWrite 成功时不返回成功。任务和订单占用继续保留，后台通过原子回执跳过已应用的零件、补做未应用的零件；接口重试不会重新打印。结果已持久化但进程退出时，租约到期后也可接管恢复。
- 定时任务默认每秒最多消费 10 个任务；配置 `delivery.packaging.poll-ms` 可调整间隔。
- worker 通过原子 claim 获得 owner 和 2 分钟租约，批量读写前续租。过期 PROCESSING 可被其他实例接管。
- 状态更新失败指数退避，最多自动尝试 10 次，之后转 REVIEW，保留订单占用。人工重试只重做状态更新，不重新打单。
- PREPARING 超过 30 分钟转 REVIEW。打印超时、保存响应丢失、进程退出等无法证明上游结果的情况，不自动解锁、不自动重打。任务预分配的 pkgId 可用于核对是否已经落库。
- DONE 后、释放占用前崩溃，定时恢复会补释放。不会因为租约或进程退出就盲目释放未完成任务的订单占用。

## 局部批量更新与幂等

worker 批量查询必要字段，使用无 upsert 的 `bulkWrite(UpdateOneModel)`，只 `$set` 发生变化的节点数量、节点状态、生产件状态、加急标记、包裹关联和更新时间，不替换整条文档或整个 procedureFlow。

同步路径和后台恢复共用同一个 applyQuantities 实现。同一生产件的数量变更与 `packagingAppliedJobIds` 回执写在同一个原子更新中。后台对 quantitiesApplied=true 的任务跳过整段数量迁移，直接处理汇总；未设置该标记的恢复任务逐件跳过已有回执，但始终重新计算订单项完成状态，覆盖“部分数量已更新”以及“数量已更新、汇总状态未更新就中断”的情况。

写入以当前版本、节点快照和相关字段为条件，冲突后重新读取并重试。`packagingRevision` 也在实体/PO 中保留，现有生产件完整对象更新加版本校验，拒绝旧快照覆盖后台数量及幂等回执。遇到这类异常的其他调用方必须重新读取，不能盲目重送旧对象。

重做件只更新自身，不参与正常订单项的完成汇总；缺少有效订单项数量时，单件判断仍回退到生产件数量。

## 人工处理

已保存包裹结果、状态写入失败达到上限：

```
POST /api/manufacturerSide/deliveryPkg/packaging-task/{taskId}/retry?manufacturerMetaId=...
```

仅接受有 result 的 REVIEW，不会再次请求上游打印。

任务在上游返回/保存过程中中断，需由配置端客服核对上游和数据库：

```
POST /api/configSide/delivery-packaging-tasks/{taskId}/reconcile
  ?manufacturerMetaId=...&deliveryPkgId=...&confirmedUpstreamAndPackage=true
```

只有 REVIEW 且已验证生产件明细的任务可关联包裹；会验证包裹工厂、订单和每个零件的数量完全匹配。核对后仅排队补做状态更新。

如果确认无上游结果、无包裹，或者已经人工核对并修正了中断的手工注销/释放操作，可在配置端关闭占用：

```
POST /api/configSide/delivery-packaging-tasks/{taskId}/close-reviewed
  ?manufacturerMetaId=...&confirmedReconciled=true&note=核对及处理记录
```

仅适用于没有已发布 result 的 REVIEW。此操作本身不撤销面单、不回滚数量，必须先完成实际核对和必要的人工修正，不能把它当作超时重试按钮。后续重新打包使用新的 requestId。

## 查询优化与验证范围

同步校验生产件只投影 ID、工厂、订单项、数量和节点校验信息；订单项只读取所属订单/工厂、数量及生产图片文件名，校验和备注复用同一次查询。

索引配置补充 `productionPieceId + deleteAt`、`orderItemId + deleteAt`（两个集合各自对应的查询）及任务状态/调度时间索引。索引不设业务 ID 唯一约束，避免对历史数据隐式迁移。

8 个纯 Java 字段补丁测试已单独编译运行，包含全量打包立即清除待排版节点、部分打包保留剩余量、订单项与单件数量不一致及重复写入时的状态处理。另有请求幂等、返回前同步更新顺序、同步失败不返回成功、任务持久化/租约和 worker 重放测试；完整项目构建仍依赖当前环境缺失的 shared/pangolin 私有构件及 MongoDB 驱动，尚未完成实际 MongoDB 集成验证。

上线前需在依赖齐全的环境运行上述测试，并进行 MongoDB 故障注入验证（并发提交、响应丢失、部分 bulkWrite 成功、worker 中断/租约接管）。滚动部署期间避免新旧打包写入逻辑同时受理请求；旧实例不识别订单占用和版本保护。
