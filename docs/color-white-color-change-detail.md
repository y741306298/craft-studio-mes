# 彩白彩图像转换与订单预处理接入：详细变更说明

版本依据：`f65fe6241f0087cb744f4dd7908dd94ae0934ba4`，已推送到 `main`。

文档日期：2026-10-17（北京时间）。本文描述该提交实际实现，区分已有机制、新增行为和待验证事项。

## 1. 业务目标与完成范围

订单项包含“弱溶剂彩白彩”工艺时，先把原生产图片提交到彩白彩转换算法。算法完成后回调 MES，MES 保存转换后的图片地址，再继续原有灰度图转 SVG、抠图、分切和生产零件生成流程。

本次新增同步和异步算法客户端，其中订单业务使用异步方式。新增订单 API 仍在订单入库并提交后台任务后返回，不等待图片转换完成。

未包含该工艺的订单项继续原有流程。一个批次中的彩白彩订单项等待回调时，不阻塞其他可以继续处理的订单项。

## 2. 变更前后流程

变更前：

```text
addOrderWithItems
  → 订单与订单项入库、生成任务及相关统计
  → 提交后台预处理队列
  → 灰度蒙版转 SVG（如需要）
  → 原有预处理策略：抠图 / 分切 / 直接生成生产零件
```

变更后：

```text
addOrderWithItems
  → 原有入库、任务创建、统计逻辑
  → 提交后台预处理队列
  → 读取订单项最新数据
  → 判断工艺节点是否包含“弱溶剂彩白彩”
      ├─ 不包含 → 原有预处理流程
      └─ 包含
          ├─ 生产图片已等于保存的转换结果 → 原有预处理流程
          └─ 尚未转换 → 提交异步转换 → 当前订单项本轮处理结束
                              ↓ 算法完成
                     新增彩白彩回调接口
                              ↓
                     校验报文、请求版本、订单状态
                              ↓
                     更新两个图片字段并持久化转换结果
                              ↓
                     提交续处理任务到后台队列
                              ↓
                     重新读取订单项，跳过重复转换
                              ↓
                     灰度蒙版转 SVG → 抠图等原有逻辑
```

这里“异步调用完成”指算法处理成功并完成结果回调，而不是收到提交请求的 HTTP 202 就开始抠图。

## 3. 新增算法客户端

### 3.1 Java 方法

所属类：`AlgorithmCoreApiService`。

| 方法 | 模式 | 用途 |
| --- | --- | --- |
| `convertColorWhiteColor(ColorWhiteColorRequest)` | 同步 | 等待转换完成并返回算法响应 |
| `convertColorWhiteColorAsync(ColorWhiteColorRequest)` | 异步 | 提交转换任务，最终结果由算法回调 |

两者均复用现有统一算法调用方法，使用 JSON POST，发起请求前保存算法调用记录。异步调用增加 `X-Fc-Invocation-Type: Async` 请求头。

算法接口地址通过完整 URL 配置，调用时不额外拼接 `/convert`，因此配置值必须包含该路径。

### 3.2 新增请求模型

`ColorWhiteColorRequest`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `imageUrl` | String | 待转换的生产图片地址 |
| `uploadConfig` | UploadConfig | 复用现有 OSS 临时授权和上传路径模型 |
| `callbackConfig` | CallbackConfig | 复用现有回调模型；异步模式必填 |

统一校验请求非空、图片地址非空、上传配置非空、OSS 配置非空、上传路径非空；异步模式额外校验回调地址。此处没有增加对 STS 各字段和有效期的逐项校验。

### 3.3 新增响应模型

`ColorWhiteColorResponse`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | Integer | 算法业务状态码 |
| `message` | String | 算法返回消息，字段名按服务文档使用 `message` |
| `data.downloadUrl` | String | 转换后图像下载地址 |
| `data.callbackCustomValue` | CallbackCustomValue | 算法原样返回的回调自定义数据 |

回调处理也使用该模型。异步提交的 HTTP 202 可能没有响应体，此时客户端返回 `null`，不代表转换失败。

在订单提交转换的逻辑中，非空响应如果带有 `code`，仅允许 `200` 或 `202`；其他值抛出提交失败异常。最终结果回调必须明确为 `code = 200`。

## 4. 配置项

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `external.api.convertColorWhiteColorUrl` | 空 | 彩白彩算法完整地址；未配置时实际调用报“算法服务地址未配置” |
| `external.callbackApi.convert_color_white_color` | 空 | 新增回调的完整外部地址；优先使用 |
| `external.callbackApi.generate_mask_files` | 沿用现有配置 | 新回调地址未配置时，用于推导同域名的新回调地址 |

测试环境配置示例：

```properties
external.api.convertColorWhiteColorUrl=https://test-cr-palette-thgxsboxut.cn-hangzhou.fcapp.run/convert
external.callbackApi.convert_color_white_color=https://<MES外部域名>/api/manufacturerSide/order/callback/convert_color_white_color
```

正式算法地址尚未提供。代码不会自动把测试算法地址作为默认值。

回调地址推导规则：仅当已有抠图回调地址以 `/callback/generate_mask_files` 结尾时，替换为 `/callback/convert_color_white_color`。如果新配置为空且旧地址不符合此规则，则报“彩白彩转换回调地址未配置”。配置新地址可以避免依赖推导规则。

本次没有直接修改配置中心中的环境配置值，需要部署时补齐。

## 5. 订单项工艺判断与请求组装

### 5.1 判断位置

`addOrderWithItems` 继续调用 `orderPreprocessTaskQueue.submit(orderItemsResult)`。实际新增判断位于该队列的消费流程，并非在下单 HTTP 请求中直接等待算法。

队列先根据 `orderItemId` 重新读取数据库里的订单项，再调用 `convertColorWhiteColorIfNecessary`。这也避免回调恢复或任务重试时继续使用转换前的图片快照。

### 5.2 命中规则

遍历 `orderItem.procedureFlow.nodes`，任一非空节点满足：

```java
"弱溶剂彩白彩".equals(node.getNodeName())
```

即视为命中。该规则为节点名称精确匹配，不依据包含关系、别名或工艺编码判断。订单项、工艺流程或节点列表为空时不命中。

### 5.3 上传和回调配置

| 请求内容 | 实际来源 |
| --- | --- |
| `imageUrl` | `orderItem.productionImgFile.rawFile` |
| `uploadConfig.ossConfig` | `aliCloudAuthService.getObjectStorageTempAuthConfig(orderItemId)` |
| `uploadConfig.uploadPath` | 有厂家 ID 时 `pieceImg/{manufacturerId}/`，否则 `pieceImg/` |
| `callbackConfig.callbackUrl` | 新回调配置或按抠图回调地址推导 |
| `callbackCustomValue.id` | `orderItem.orderItemId` |
| `callbackCustomValue.orderItemId` | `orderItem.orderItemId` |
| `callbackCustomValue.preprocessRequestId` | 当前预处理请求 ID；不存在时生成并先保存到订单项 |

`id` 不采用抠图的 `orderItemId#preprocessRequestId` 编码，按本次需求直接使用 `orderItemId`；请求版本通过单独字段透传。

业务请求示例（凭证均为占位符）：

```json
{
  "imageUrl": "https://images.example/input.tif",
  "uploadConfig": {
    "ossConfig": {
      "stsToken": {
        "accessKeyId": "<动态获取>",
        "accessKeySecret": "<动态获取>",
        "expiration": "<动态获取>",
        "securityToken": "<动态获取>"
      },
      "bucket": "<授权对应的bucket>",
      "region": "oss-cn-hangzhou"
    },
    "uploadPath": "pieceImg/factory-1/"
  },
  "callbackConfig": {
    "callbackUrl": "https://mes.example/api/manufacturerSide/order/callback/convert_color_white_color",
    "callbackCustomValue": {
      "id": "item-1",
      "orderItemId": "item-1",
      "preprocessRequestId": "request-1"
    }
  }
}
```

## 6. 新增订单回调接口

控制器：厂家侧 `OrderController`。

```http
POST /api/manufacturerSide/order/callback/convert_color_white_color
Content-Type: application/json
```

请求示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "downloadUrl": "https://images.example/cwc.tif",
    "callbackCustomValue": {
      "id": "item-1",
      "orderItemId": "item-1",
      "preprocessRequestId": "request-1"
    }
  }
}
```

### 6.1 校验与处理顺序

1. 校验响应存在且 `code = 200`。
2. 校验 `data`、`callbackCustomValue`、`callbackCustomValue.id` 和 `downloadUrl` 非空。
3. 获取该订单项和预处理请求对应的 Redis 回调锁。
4. 检查是否已有回调处理完成标记，已有则直接结束。
5. 根据 `callbackCustomValue.id` 查询订单项，不存在则报错。
6. 检查取消标记、订单项退回状态、订单退回状态及预处理请求版本。
7. 写入图片地址和转换结果标记并持久化。
8. 通过 `submitConvertedOrderItem` 将续处理任务入队。
9. 入队成功后写入 Redis 完成标记，最后释放锁。

旧回调、已退回或当前已取消的任务会被忽略；控制器仍返回成功，不更新图片、不提交续处理。

请求版本判断沿用已有机制：回调带有 `preprocessRequestId` 时必须与订单项当前值相等；回调不带时，只有订单项当前也没有该值才视为有效。因此算法应完整透传本次请求发送的自定义字段。

### 6.2 数据更新

| 持久化字段 | 更新值 | 用途 |
| --- | --- | --- |
| `productionImgFile.rawFile` | `data.downloadUrl` | 后续生产及抠图使用转换后的原图 |
| `productionImgFile.filePreview.raw` | `data.downloadUrl` | 同步原图预览引用 |
| `colorWhiteColorDownloadUrl` | `data.downloadUrl` | 判断当前生产图片是否已完成转换 |

当 `productionImgFile` 或其 `filePreview` 不存在时创建对象。下载地址直接采用算法回传值，不再拼接 OSS 域名；其他现有图片字段保持原值。

### 6.3 接口返回

成功采用项目统一 `ApiResponse`：

```json
{
  "code": 200,
  "message": "success",
  "data": "回调处理成功",
  "timestamp": 1790000000000
}
```

`timestamp` 是响应时生成的时间戳，示例仅说明格式。

异常捕获后返回 `ApiResponse.fail(serviceError, "回调处理失败：...")`，其中业务 `code` 为 `500`。控制器没有显式设置 HTTP 500 状态；算法侧不能仅凭 HTTP 2xx 判断回调业务已成功。

## 7. 防重复与防过期机制

### 7.1 避免重复转换

新增订单项字段 `colorWhiteColorDownloadUrl`。当其值与当前 `productionImgFile.rawFile` 相等时，跳过转换并继续后续处理。

此机制根据图片地址判断，未使用文件内容哈希；同一 URL 的文件内容被替换但地址不变时，无法单靠该字段识别。生产图片 URL 变更后会重新走转换判断。

### 7.2 回调锁与完成标记

Redis 键：

```text
orderPreprocessing:colorWhiteColorCallback:{orderItemId}:{preprocessRequestId}:lock
orderPreprocessing:colorWhiteColorCallback:{orderItemId}:{preprocessRequestId}:completed
```

锁有效期 10 分钟，完成标记有效期 7 天。释放锁沿用已有 Lua 比较令牌再删除的逻辑。

并发回调未取得锁时返回业务失败，提示重试；已存在完成标记时直接成功结束。

这些机制降低重复处理风险，不构成数据库、Redis 和内存队列之间的跨系统事务，不能承诺在进程崩溃等所有边界条件下严格只执行一次。

## 8. 队列改动、异常与重试

### 8.1 处理顺序与批次隔离

队列将可继续处理的订单项收集到 `conversionReady`。等待彩白彩回调的订单项不会进入该列表，因此不会在转换完成前调用灰度蒙版转换或原有预处理。

彩白彩提交失败按订单项收集失败 ID；批次中其他可处理订单项仍继续执行。后续预处理抛出的批次失败也合并后交给现有重试机制。

### 8.2 续处理入队

新增 `submitConvertedOrderItem(OrderItem)`，直接将续处理任务加入队列。它不受原先 `queuedOrderItemIds` 的去重过滤影响，避免算法很快回调、而初始提交任务尚未退出时，续处理被当成重复任务丢弃。

此方法使用 `queue.offer`，队列未初始化或已满时抛出异常。回调在入队成功前不写完成标记，便于再次回调重试。

### 8.3 现有队列配置继续生效

| 配置 | 默认值 | 含义 |
| --- | --- | --- |
| `order.preprocess.queue.capacity` | 1000 | 队列容量 |
| `order.preprocess.queue.batch-size` | 1000 | 批次大小 |
| `order.preprocess.queue.max-retry` | 3 | 最大重试次数 |
| `order.preprocess.queue.retry-backoff-ms` | 1000 | 基础退避毫秒数，按重试次数递增 |
| `order.preprocess.queue.pending-recovery-age-ms` | 1800000 | 待处理恢复的老化阈值 |
| `order.preprocess.queue.pending-recovery-interval-ms` | 60000 | 待处理恢复扫描间隔 |

这些配置本次未新增，继续复用。失败重试耗尽后由已有队列逻辑标记永久失败。

### 8.4 需要了解的恢复边界

- 新流程没有增加持久化的“彩白彩已提交、等待回调”状态。订单项继续处于待处理阶段时，已有超时恢复扫描可能再次提交转换。
- 续处理队列为进程内队列，重启后依赖既有待处理恢复机制恢复，不是新增持久化消息队列。
- 回调失败的重试依赖算法侧或调用方识别 MES 的业务响应并重新请求；本次没有在 MES 新增主动拉取算法结果或主动补回调机制。
- 图片已保存但续处理入队失败时，完成标记尚未写入，再次有效回调仍可提交续处理。
- 取消检查沿用当前预处理服务的本地取消集合，订单及订单项退回检查则会读取对应状态。

## 9. 数据模型及存储兼容

`OrderItem` 和 MongoDB `orderItem` 对应的 `OrderItemPo` 新增字符串字段 `colorWhiteColorDownloadUrl`，并补齐 PO→领域对象、领域对象→PO 两个方向的映射。

历史文档没有该字段时按 `null` 处理，无需执行关系型数据库 DDL。对于历史订单是否触发转换，仍取决于其工艺节点和是否再次进入后台预处理队列，本次未执行批量历史数据补处理。

## 10. 算法调用记录

`AlgorithmCoreApiCallType` 新增：

| type | 描述 | sourceId 来源 |
| --- | --- | --- |
| `convertColorWhiteColor` | 同步彩白彩图像转换 | `callbackCustomValue.id` |
| `convertColorWhiteColorAsync` | 异步彩白彩图像转换 | `callbackCustomValue.id` |

订单业务使用第二种类型。已有调用记录查询接口可用于检查提交时的请求参数及业务来源：

```http
GET /api/algorithmCoreApiCallRecord?type=convertColorWhiteColorAsync&sourceId=item-1
```

调用记录在发起 HTTP 请求前写入，因此记录存在本身不能证明算法已接收或处理成功。

## 11. 文件变更清单

下列为 `f65fe624` 的 14 个文件，路径相对项目根目录。提交统计为新增 533 行、删除 3 行。

| 模块或目录 | 文件 | 变更 |
| --- | --- | --- |
| application / command/api | `AlgorithmCoreApiService.java` | 新地址配置、同步和异步客户端、参数校验 |
| application / command/api/req | `ColorWhiteColorRequest.java` | 新增请求模型 |
| application / command/api/resp | `ColorWhiteColorResponse.java` | 新增结果与回调模型 |
| application / command/order | `AppOrderService.java` | 更新预处理入口注释；原有入队调用保持 |
| application / command/orderPreprocessing | `AppOrderPreprocessingService.java` | 工艺判断、请求组装、回调处理、地址推导、防重复与防过期 |
| application / command/orderPreprocessing | `OrderPreprocessTaskQueue.java` | 前置转换判断、读取最新数据、续处理入队及失败项合并 |
| application / test/orderPreprocessing | `ColorWhiteColorPreprocessingTest.java` | 新增服务逻辑测试 |
| application / test/orderPreprocessing | `ColorWhiteColorQueueTest.java` | 新增队列衔接测试 |
| domain / order/orderInfo/entity | `OrderItem.java` | 新增转换结果地址字段 |
| domain / shared/algorithm/enums | `AlgorithmCoreApiCallType.java` | 新增两种调用类型 |
| infra / dal/order/po | `OrderItemPo.java` | 新字段及双向存储映射 |
| interfaces / platform/manufacturerSide/order | `OrderController.java` | 新增回调 POST 接口 |
| docs | `algorithm-core-api-call-record-api.md` | 补齐调用类型与来源映射 |
| docs | `color-white-color-api.md` | 新增接入说明 |

## 12. 测试及验证状态

新增测试共 7 项，覆盖：

1. 命中彩白彩工艺时提交转换，并返回等待状态。
2. 普通订单项继续原流程，不调用转换。
3. 成功回调先保存两个图片地址，再触发续处理；转换结果不会再次转换。
4. 失败或过期回调不修改图片、不触发续处理。
5. 已有完成标记的重复回调不再次触发续处理。
6. 混合批次中，等待转换的订单项不进入灰度转换和后续预处理。
7. 原任务仍被标记为已入队时，提前回调的续处理仍能入队。

已通过 `git diff --check`，并静态检查了转换判断先于原有预处理，以及新增字段的双向存储映射。

当前执行环境缺少 Maven，Java 为 21，而项目配置要求 Java 22。因此新增测试未执行，未完成项目编译，也未使用有效 STS 和真实算法完成联调；测试代码存在不等于验证通过。

具备项目依赖和对应 JDK 的环境可执行：

```bash
mvn -pl craftstudio-mes-application -am \
  -Dtest=ColorWhiteColorPreprocessingTest,ColorWhiteColorQueueTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## 13. 部署与联调步骤

1. 配置目标环境的算法完整 URL 和 MES 完整回调 URL。
2. 确认算法服务可以访问输入图片、使用临时授权上传结果，并访问 MES 回调地址。
3. 确认网关及现有鉴权配置允许算法服务请求新增回调路径。本次只新增控制器路由，没有新增鉴权策略。
4. 确认 Redis 和订单数据存储可用；准备含“弱溶剂彩白彩”节点的订单项。
5. 调用原有新增订单接口，检查彩白彩请求的 `id` 为订单项 ID，回调地址为新路径。
6. 在真实回调到达前，确认该订单项尚未发起后续灰度图转换和抠图。
7. 回调后核对两个图片字段和 `colorWhiteColorDownloadUrl`，确认其值均为算法返回的 `downloadUrl`。
8. 检查后续算法输入是否使用更新后的生产图片，并检查原有生产零件生成结果。
9. 验证普通工艺、混合批次、重复回调、失败回调和重做后的旧回调。
10. 确认算法侧对 MES 回调响应的业务码进行判断，业务失败可以重试。

建议验收数据至少包含：一个普通订单项、一个彩白彩订单项、一个需要抠图的彩白彩订单项，以及一个已退回或已刷新预处理请求 ID 的订单项。
