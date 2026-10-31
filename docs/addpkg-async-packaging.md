# addPkg 异步状态更新与恢复

## 返回与客户端约定

`addPkg` 同步完成：持久化请求身份与订单占用 → 从数据库校验零件和数量 → 获取/复用面单 → 保存包裹及打印结果 → 原子发布持久化状态任务 → 返回打印响应。

生产件数量、生产件状态、订单项完成状态由后台任务更新。返回成功表示面单/打印数据和包裹已保存、状态任务已持久化，不表示后台已完成，也不表示纸质面单已经打印。

请求新增 `requestId`：每次新的打包操作生成 UUID；网络超时重试必须复用原值。相同 requestId + 相同内容返回已保存结果，不再次打印；相同 requestId + 不同内容拒绝。已明确失败的请求保留失败记录；修正后重新发起操作需使用新 requestId，不会复用旧任务身份重新占用订单。

旧客户端不传 requestId 时，服务端按订单、零件和数量、物流、路线和打印设备等内容生成确定性键，相同内容将重放原结果。**有意再次打相同零件和数量的另一包，必须传新的 requestId**。建议前端在每次用户确认打包时生成并保存 UUID，不在每次 HTTP 重试时重新生成。

响应新增 `packagingTaskId`、`packagingState`。使用以下接口查询最新状态：

```
GET /api/manufacturerSide/deliveryPkg/packaging-task/{taskId}?manufacturerMetaId=...
```

## 占用与状态机

采用保守的**订单级独占预占**：MongoDB `deliveryPackagingOrderLock` 以工厂+订单的摘要作为 `_id`。一个订单在所有实例上只允许一个未完成的打包任务；任务完成或明确无副作用的失败后才释放。手动注销、释放包裹也经过同一个占用入口，避免与后台更新竞态。

这不是从生产节点提前扣减数量。接口取得占用后重新读取、校验数据库中的数量；占用期间拒绝同订单后续打包，并返回占用任务 ID。因此列表短时间内可能仍显示待打包，但提交会被阻止。不同订单可独立受理。

持久化任务集合 `deliveryPackagingTask`：

```
PREPARING → READY → PROCESSING → DONE
                  ↘ RETRY ↗
PREPARING / PROCESSING → REVIEW（需核对）
PREPARING → FAILED（尚无副作用，释放占用）
REVIEW → CLOSED（客服确认已核对/处理副作用后关闭）
```

- 包裹结果与 READY 在同一个任务文档原子保存，成功返回前必须完成。
- 定时任务默认每秒最多消费 10 个任务；配置 `delivery.packaging.poll-ms` 可调整间隔。
- worker 通过原子 claim 获得 owner 和 2 分钟租约，批量读写前续租。过期 PROCESSING 可被其他实例接管。
- 状态更新失败指数退避，最多自动尝试 10 次，之后转 REVIEW，保留订单占用。人工重试只重做状态更新，不重新打单。
- PREPARING 超过 30 分钟转 REVIEW。打印超时、保存响应丢失、进程退出等无法证明上游结果的情况，不自动解锁、不自动重打。任务预分配的 pkgId 可用于核对是否已经落库。
- DONE 后、释放占用前崩溃，定时恢复会补释放。不会因为租约或进程退出就盲目释放未完成任务的订单占用。

## 局部批量更新与幂等

worker 批量查询必要字段，使用无 upsert 的 `bulkWrite(UpdateOneModel)`，只 `$set` 发生变化的节点数量、节点状态、生产件状态、加急标记、包裹关联和更新时间，不替换整条文档或整个 procedureFlow。

同一生产件的数量变更与 `packagingAppliedJobIds` 回执写在同一个原子更新中。重试跳过已有回执的生产件，但始终重新计算订单项完成状态，覆盖“数量已更新、汇总状态未更新就中断”的情况。

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

4 个纯 Java 字段补丁测试已单独编译运行。另有请求幂等、任务持久化/租约和 worker 重放测试；完整项目构建仍依赖当前环境缺失的 shared/pangolin 私有构件及 MongoDB 驱动，尚未完成实际 MongoDB 集成验证。

上线前需在依赖齐全的环境运行上述测试，并进行 MongoDB 故障注入验证（并发提交、响应丢失、部分 bulkWrite 成功、worker 中断/租约接管）。滚动部署期间避免新旧打包写入逻辑同时受理请求；旧实例不识别订单占用和版本保护。
