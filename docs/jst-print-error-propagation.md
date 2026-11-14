# 聚水潭打印错误原因透传

## 原因

`DeliveryPkgController.addPkg` 记录异常后原样抛出。`AppDeliveryPkgService` 的聚单平台打印、复打分支用 `ex.getMessage()` 构造业务异常，`ApiControllerHandler` 再将该消息写入响应的 `message`。

聚水潭适配器 `LogisticsPrint.getPrintData` 原来在以下情况均抛出 `LogisticsLabelPrintException("打印出错")`：

- `LId` 为空。
- `task.documents` 不存在或数量不为 1。
- `document.contents` 不存在或为空。

此外，原来的 `fetchPrintDataObject` 只在 `requestBodys` 为空数组时识别 `apiResponse.code == 910001`，没有处理其他上游错误码和原因。上游错误可能因此进入缺字段校验，或者被当成尚未生成面单而继续申请面单。

## 修复

在 `craftstudio-pangolin` 的 JST 模块新增纯响应解析器 `PrintDataResponseParser`，并由 `LogisticsPrint.fetchPrintDataObject` 调用：

- 首先识别上游错误码或 `success=false`，直接以 `msg` / `message` / `Message` 等上游错误原因作为异常消息；上游没有原因时才用错误码兜底。
- `createLogisticsLabel` 同样检查申请面单响应中的 `IsSuccess=false`、`ExceptionMessage` 和每条面单的 `Code` / `Message`，失败立即抛出，避免继续获取单号后用缺少 `LId` 覆盖申请失败原因。
- 校验物流单号和面单结构时，优先返回已有上游原因，不加“缺少 LId”等前缀；上游确实没有原因时才说明缺失的字段或错误数量。
- `910001` 继续抛出 `SmsCodeRequiredAuthException`，由现有 MES 身份锁流程处理。
- 成功的空面单数组继续返回 `null`，沿用首次申请面单流程；有效面单数据保持原样。
- 空响应和非 JSON 响应返回明确的解析失败说明。

例如，上游响应 `{"apiResponse":{"code":123,"msg":"该快递未配置电子面单"},"requestBodys":[]}` 现在抛出 `LogisticsLabelPrintException("该快递未配置电子面单")`。现有 `platform.printLogisticsLabel` 调用处已经捕获并重新抛出异常，外层保留 `ex.getMessage()` 和原异常 cause，最终接口的 `message` 原样返回“该快递未配置电子面单”。

## 验证和生效条件

`PrintDataResponseParserTest` 的 41 个测试用例已通过，覆盖成功、首次创建、申请面单失败、上游错误优先于缺少单号、上游原因原样返回、身份校验和面单结构异常。使用 JDK 22、fastjson2 2.0.58、JUnit Console 1.10.2 单独编译并运行实际解析器及测试，没有访问真实聚水潭接口。

工作区原来的 `craftstudio-pangolin` 是未展开的 gitlink，记录提交 `97d5bdb08251dcaa5e965da11eaf3c5ed4dd157d`，该提交无法从依赖仓库读取。本次补齐的可访问源码基于 `y741306298/craftstudio-pangolin` 的 `main` 提交 `e44fe4315210bfb328308a3aa8f81c00e78a382a`，其 POM 版本是 0.1.0；MES 当前声明依赖版本为 0.1.1。因此这次完成的是依赖源码修复，不能视为已经修复已发布的 0.1.1 制品或线上服务。

MES 完整编译因环境缺少 Pangolin 0.1.1、Shared 1.2.1 等依赖而未完成。需将修复合入实际发布分支、构建发布新的 Pangolin 版本，再更新 MES 的依赖并部署。当前未修改 MES 的依赖版本，也未发布或部署。

依赖源码修复已提交至 `craftstudio-pangolin/main`：`91dcf0355fb4a30fe290282c55933c55ed4d6f24`。MES 的 `craftstudio-pangolin` 源码引用同步指向此提交；这个源码引用更新不会替换 Maven 实际使用的 0.1.1 制品。

没有本次失败请求的上游响应日志，不能判断它具体触发了上述哪个校验分支。
