# 矩形旗出血算法调用

`AlgorithmCoreApiService.generateRectBlood(request)` 同步返回结果；
`generateRectBloodAsync(request)` 通过已有的函数计算异步调用封装提交任务，最终结果由算法回调返回，HTTP 202 空响应体时返回 null。

配置完整的接口地址（包含 `/rect_blood`）：

```properties
# 测试环境
external.api.rectBloodUrl=https://test-crill-flag-zfdugbmgiv.cn-hangzhou.fcapp.run/rect_blood
# 正式环境改为：
# external.api.rectBloodUrl=https://craftstill-flag-mlndcuvvit.cn-hangzhou.fcapp.run/rect_blood
```

配置默认为空，未配置时调用会报“算法服务地址未配置”。

```java
RectBloodRequest request = new RectBloodRequest();
request.setImageUrl(imageUrl);
RectBloodRequest.BloodConfig bloodConfig = new RectBloodRequest.BloodConfig();
bloodConfig.setScrollEdge(RectBloodRequest.ScrollEdge.LEFT);
bloodConfig.setScrollWidth(30);
bloodConfig.setSewWidth(10);
bloodConfig.setMirrorRequired(true);
// 不设置 scrollColorHex 时，算法自动采样边缘主色。
request.setBloodConfig(bloodConfig);
// 复用现有 UploadConfig，ossConfig 使用运行时获取的有效 STS 临时授权。
request.setUploadConfig(uploadConfig);
RectBloodResponse response = algorithmCoreApiService.generateRectBlood(request);
```

异步调用需额外设置 `CallbackConfig`，其中 `callbackUrl` 必填，
`callbackCustomValue` 复用现有类型（包含 `id` 等字段）。
响应与回调均可使用 `RectBloodResponse` 解析，包含 `code`、`message`、
`data.flagUrl`、`data.mirrorFlagUrl`、`data.callbackCustomValue`。
调用沿用已有的日志与调用记录机制，记录类型为 `generateRectBlood` 或
`generateRectBloodAsync`，来源 ID 取透传数据中的 `id`。
