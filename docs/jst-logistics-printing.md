# JST 快递预打印、打包与复打改动说明

## 实现范围

在原 WDT 快递流程中增加 JST 支持，涵盖定时预打印、打包时即时打单、消费预打印面单和包裹复打。已阅读 pangolin main 提交 `e44fe4315210bfb328308a3aa8f81c00e78a382a` 下的 JST `LogisticsTest`、`JstGatherPlatform` 和 `LogisticsPrint`；MES 基于 `aff33c68` 修改。

`LogisticsTest` 的真实接口测试带有 `@Disabled`，本次没有执行真实换仓、审核、出库或面单生成操作。

## 平台如何识别

使用订单 `channel.type = GATHER_PLATFORM` 和 `channel.code`，不使用电商平台字段 `platformCode` 分派。

| channel.code | 行为 |
| --- | --- |
| JST，忽略大小写和两侧空格 | 调用 `GatherPlatformType.JST` |
| WDT，忽略大小写和两侧空格 | 调用 `GatherPlatformType.WDT` |
| 空值、空字符串 | 按历史 WDT 流程处理 |
| 其他值 | 报错，不默认调用 WDT |

渠道订单 ID 使用 `channel.orderId`，JST 对应其内部订单号，例如测试源码中的 `542919`。

## 新增了什么

1. `GatherPlatformResolver`：统一识别 WDT/JST 和历史空编码，拒绝未知编码。
2. 快递配置请求、配置实体及面单记录新增 `platformType`，支持 `WDT` 和 `JST`。
3. 配置仓储新增 `findByPlatformType`，面单仓储新增 `findForPlatform`，按平台隔离查询。
4. 配置接口新增通用路径别名，原 WDT 路径继续可用：
   - `/api/manufacturerSide/gather-platform/logistics-config`
   - `/api/configSide/gather-platform/logistics-config`
5. 新增 `GatherPlatformLogisticsTest`，使用模拟平台客户端覆盖平台分派、JST 预打印、缺少配置、复打、不同运单号拒绝和测试环境保护。

## 修改了什么

| 文件或组件 | 修改内容 |
| --- | --- |
| AppPreOrderLabelTaskService | 按渠道平台读取对应配置，使用对应客户端换仓及预打印，面单保存平台快照；将打印异常和重试方法命名改为聚单平台通用名称 |
| AppDeliveryPkgService | 按平台查预打印记录；没有记录时调用对应平台即时打单；JST 的已有面单在打包时重新获取打印数据；即时新打出的面单直接使用，避免紧接着重复调用复打 |
| AppDeliveryPkgService 复打 | 使用记录中的平台快照选择客户端；检查订单和面单所属平台一致；复打返回不同运单号时报错，不更新原记录和包裹 |
| DeliveryPkgController | `/reprint` 自动识别聚单平台订单并走聚单平台复打；`/gather-platform/reprint` 继续支持直接调用 |
| WdtConfig / WdtLabelRecord | 增加 platformType，保留原类名以兼容现有引用 |
| WdtConfigPO / WdtLabelRecordPO | 保存及还原 platformType |
| WdtConfigRepository / WdtConfigRepositoryImp | 配置按平台、工厂和快递预设查找，过滤已删除数据 |
| WdtLabelRecordRepository / WdtLabelRecordRepositoryImp | 面单按平台隔离查询；空的订单号、渠道订单号或运单号不再作为 OR 查询条件，避免匹配无关记录 |
| WdtService | 校验并规范化 platformType，增加指定平台查配置的方法 |
| WdtConfigRequest | 接收 platformType 并传入领域配置 |
| 两个 WdtController | 增加通用配置路径别名 |

## JST 快递配置示例

请求：`POST /api/manufacturerSide/gather-platform/logistics-config/add`。

```json
{
  "platformType": "JST",
  "manufacturerMetaId": "实际工厂ID",
  "warehouseId": "15715817",
  "logisticsId": "ZTO",
  "logisticsName": "中通快递",
  "presetType": "ZTO"
}
```

`warehouseId` 和 `logisticsId` 必须使用实际 JST 账户的数据。示例仓库和物流编码来自 LogisticsTest，并非所有账户的通用默认值。`presetType` 使用 `LogisticsCarrierPresetType` 的枚举名称。代码没有硬编码示例仓库。

同一组路径下继续提供 `/update`、`/list`、`GET /{id}` 和 `DELETE /{id}`。更新时传入 `id` 与完整配置，包括 `platformType: JST`；省略平台会按 WDT 处理。

列表保持现有工厂和快递预设筛选规则，会返回两种平台的配置，响应中的 `platformType` 可用于区分。

## 预打印与打包规则

- 每十分钟的定时任务继续使用现有任务锁，等待生产工件生成后处理；退单仍删除任务。
- JST 使用自己的配置执行换仓和物流设置，再获取面单；成功后保存单号、打印数据和平台快照。
- 保留订单与工件的单号同步，以及现有物流 MQ 通知格式。
- 打印阶段异常沿用最多三次尝试机制；换仓异常仍按原有失败路径处理。Redis 重试键保留历史 `preOrderLabelTask:wdtPrintRetry:` 命名，以兼容升级时尚未完成的任务。
- 打包优先查对应平台的既有面单；JST 既有面单重新获取云打印数据后绑定包裹，记录标记为 `CONSUMED`。
- 没有预打印记录时走即时打印，成功结果直接用于打包。
- 缺少对应平台快递配置时不会借用另一平台配置：预打印沿用原有任务失败处理，打包沿用原有 CUSTOM 降级行为。因此上线前必须配置 JST 快递映射。

## 复打接口

在现有 DeliveryPkgController 路径下，以下两个接口都支持 JST：

- `POST /reprint`
- `POST /gather-platform/reprint`

请求仍使用 `DeliveryPkgActionRequest`：

```json
{"deliveryPkgId": "实际包裹ID"}
```

JST 调用 `rePrint(channelOrderId, "mes-logistics", true, presetType, logisticsOrderId)`；WDT 抖音面单继续调用远端复打，其余 WDT 面单沿用本地打印数据。返回现有包裹打印结果结构，包含 `logisticsCloudPrintData`，无需新增 JST 专属前端数据结构。

**JST 实现限制：** 当前 pangolin 的 `rePrint` 底层仍调用 `LogisticsPrint.print`，未直接使用传入的原运单号。通常会读取既有面单；面单缺失时底层可能触发审核、出库或面单生成。本次在 MES 增加返回运单号一致性检查，防止把不同单号写回原包裹；该检查无法撤销上游已经发生的动作。生产使用需保证 JST 原面单仍有效。

`kuaidi100=test` 时，预打印和打包继续使用测试单号；新增复打保护，直接返回现有包裹，不访问真实聚单平台。

`/release` 沿用原本的本地解包流程，本次没有增加 JST 远端运单取消操作。

## 存储及历史数据兼容

继续使用 MongoDB `wdtConfig` 和 `wdtLabelRecord` 集合，仅增加 `platformType` 字段，无需新建集合。历史缺失或空平台字段按 WDT 查询；JST 必须明确保存为 JST。原有按工厂/快递查配置和查面单方法仍保留，并默认只查询 WDT。

本次不修改 pangolin 仓库、不更新主仓库中的 pangolin gitlink，也不改变现有 Maven 依赖版本。

## 验证结果

- `git diff --check` 通过。
- 17 个新增或修改的 Java 文件通过 JavaParser 语法检查；该检查不替代完整编译。
- 使用真实源码及 JDK 22 编译运行了 10 项平台分派检查，全部通过。
- 使用真实实体、PO 和 WdtService 编译运行了 6 项配置校验及持久化转换检查，全部通过，包括 JST 平台快照、历史空字段和无效平台拒绝。
- 新增的六个模拟单元测试尚未完成 Maven 执行：完整构建在依赖解析阶段失败，云端缺少内部 `craftstudio-shared-{domain,infra,application}:1.1.5` 和 `craftstudio-pangolin-{domain,infra}:0.1.0` 构件，当前 Maven 镜像为 Maven Central。
- 未运行真实 JST 接口测试、数据库集成测试或完整服务启动验证。

内部依赖恢复后可执行：

```bash
mvn -pl craftstudio-mes-application -am \
  -Dtest=GatherPlatformLogisticsTest,AppPreOrderLabelTaskServiceMqRetryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```
