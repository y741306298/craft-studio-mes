# addPkg 异步状态更新与恢复

## 返回与客户端约定

`addPkg` 同步完成：持久化请求身份与订单占用 → 从数据库校验零件和数量 → 获取/复用面单 → 持久化包裹、打印结果和任务执行租约 → 同步批量更新本次零件的数量及完成状态 → 原子标记 quantitiesApplied 并排队汇总任务 → 返回打印响应。

成功返回前，本次生产件的待打包/已打包数量、包裹关联和单件完成状态已经落库。完全打包的零件标记已完成，并清零其他工序节点的残留数量，因此 listTypesettingAndProductionPieces、listTypesettingFilterOptions、listTypesettingAndProductionPiecesById 以及待打包列表不会在后台汇总前继续匹配这些零件。部分打包仅迁移本次数量，保留仍需排版/生产/打包的剩余数量，不会把整件提前隐藏。

订单项完成状态、关联正常零件的汇总完成/加急标记仍由后台处理；汇总仍需满足订单项阈值，并额外确保物理零件本身已全部打包，避免后台再次把部分打包零件提前完成。返回成功不表示后台汇总已完成，也不表示纸质面单已经打印。单件完成以该生产件 quantity 为准，订单项数量只在生产件缺失 quantity 时作为回退，不能因为订单项阈值较小就提前完成部分打包的生产件。

请求新增 `requestId`：每次新的打包操作生成 UUID；网络超时重试必须复用原值。相同 requestId + 相同内容仅在 quantitiesApplied=true 或旧任务已 DONE 时返回已保存结果，不再次打印；若正在同步写入或部分失败等待恢复，则提示处理中而不返回打包成功；相同 requestId + 不同内容拒绝。已明确无副作用的 FAILED 请求可直接再次提交（包括旧客户端不传 requestId）；服务端保留失败记录，并沿确定性的重试链创建新任务、分配新 pkgId。并发重试进入同一后继任务，成功后重放后继任务结果，不会重复打印。CLOSED 且没有 result 的任务也允许重新提交；REVIEW 或已有 result 的部分失败仍走恢复/核对流程。相同 requestId 不允许改变打包内容，修正内容或开始新的打包批次时使用新 requestId。

旧客户端不传 requestId 时，服务端按订单、零件和数量、物流、路线和打印设备等内容生成确定性键，相同内容将重放原结果。**有意再次打相同零件和数量的另一包，必须传新的 requestId**。建议前端在每次用户确认打包时生成并保存 UUID，不在每次 HTTP 重试时重新生成。

响应新增 `packagingTaskId`、`packagingState`。使用以下接口查询最新状态：

```
GET /api/manufacturerSide/deliveryPkg/packaging-task/{taskId}?manufacturerMetaId=...
```

## 执行锁与状态机

`deliveryPackagingOrderLock` 仅用于同一工厂、同一订单的执行互斥，不再作为长期业务预占。请求和手工操作（注销、释放包裹）均在 finally 中释放锁，无论成功、失败、REVIEW 或异常；后台任务每次执行时重新取得锁，在 finally 中释放。后台因订单忙而推迟执行不消耗失败重试次数。任务历史和已保存的包裹结果仍保留，用于原请求幂等重放与部分失败恢复。

每次取得锁都生成独立 owner，释放时同时匹配订单键与 owner，避免旧请求的 finally 误删后来请求的锁。正常关闭进程时释放本进程持有的锁；异常退出、数据库短暂不可达或强制终止时依赖 120 秒租约回收。存活进程每 20 秒为执行中的锁续租，过期锁不能续租复活。定时恢复及新请求取锁前都会清理过期锁和没有租约的旧版锁，因此历史 manual REVIEW 锁不会继续永久卡住订单。后台写入前校验本次执行 owner，锁被清除或丢失时停止后续写入并进入恢复。

部署时必须停止所有旧版实例后再由新版受理请求；旧版实例不识别租约，不能与新版同时写入。

配置端提供清理全部打包锁的管理接口，沿用配置端鉴权：

```
DELETE /api/configSide/delivery-packaging-tasks/locks
```

成功响应 data 包含 `deletedCount`，表示删除的锁数量。仅删除锁，不删除任务、包裹、面单或零件数量，也不会回滚或终止已经在执行的操作。清理后新请求可重新取得锁；同 requestId 的幂等规则仍有效，已有包裹的请求继续复用结果或恢复。进行中的后台任务在下一次写入前检查到锁丢失会停止本次执行。

持久化任务状态：

```
PREPARING → PROCESSING（本次零件同步更新）→ READY → PROCESSING（后台汇总）→ DONE
                         ↘ RETRY（恢复未完成的更新）↗
PREPARING / PROCESSING → REVIEW（需核对，但不再长期锁单）
PREPARING → FAILED（尚无副作用）
REVIEW → CLOSED（已核对/处理副作用后关闭）
```

包裹结果先与 PROCESSING 执行租约原子保存，再更新本次零件；全部本次零件写入成功后设置 quantitiesApplied=true、state=READY。部分写入失败不会返回成功，后台通过原子回执补做未完成部分，不重新打印。后台失败指数退避，10 次后进入 REVIEW；人工重试仅重做状态更新。PREPARING 超过 30 分钟进入 REVIEW，保留核对记录但不保留执行锁。

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

如果确认无上游结果、无包裹，或者已经人工核对并修正了中断的手工注销/释放操作，可在配置端关闭任务：

```
POST /api/configSide/delivery-packaging-tasks/{taskId}/close-reviewed
  ?manufacturerMetaId=...&confirmedReconciled=true&note=核对及处理记录
```

仅适用于没有已发布 result 的 REVIEW；任务是否关闭不影响已结束执行的锁释放。此操作本身不撤销面单、不回滚数量，必须先完成实际核对和必要的人工修正，不能把它当作超时重试按钮。后续重新打包使用新的 requestId。

## 查询优化与验证范围

同步校验生产件只投影 ID、工厂、订单项、数量和节点校验信息；订单项只读取所属订单/工厂、数量及生产图片文件名，校验和备注复用同一次查询。

索引配置补充 `productionPieceId + deleteAt`、`orderItemId + deleteAt`（两个集合各自对应的查询）及任务状态/调度时间索引。索引不设业务 ID 唯一约束，避免对历史数据隐式迁移。

8 个纯 Java 字段补丁测试已单独编译运行，包含全量打包立即清除待排版节点、部分打包保留剩余量、订单项与单件数量不一致及重复写入时的状态处理。另有请求幂等、返回前同步更新顺序、同步失败不返回成功、任务持久化/租约和 worker 重放测试；完整项目构建仍依赖当前环境缺失的 shared/pangolin 私有构件及 MongoDB 驱动，尚未完成实际 MongoDB 集成验证。

上线前需在依赖齐全的环境运行上述测试，并进行 MongoDB 故障注入验证（并发提交、响应丢失、部分 bulkWrite 成功、worker 中断/租约接管）。滚动部署期间避免新旧打包写入逻辑同时受理请求；旧实例不识别订单占用和版本保护。

