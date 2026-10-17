# 彩白彩图像转换

通过 `AlgorithmCoreApiService` 调用外部算法，复用 JSON POST、调用记录和 OSS 临时授权上传模型。

## 地址配置

在项目使用的外部配置源中添加完整接口地址：

```properties
external.api.convertColorWhiteColorUrl=https://test-cr-palette-thgxsboxut.cn-hangzhou.fcapp.run/convert
```

以上为测试地址，正式地址待提供。配置默认留空，未配置时调用会报“算法服务地址未配置”，不会默认请求测试环境。

## 调用方式

- 同步：`convertColorWhiteColor(ColorWhiteColorRequest)`，等待处理完成，返回 `ColorWhiteColorResponse`。
- 异步：`convertColorWhiteColorAsync(ColorWhiteColorRequest)`，沿用现有 FC 调用机制，发送 `X-Fc-Invocation-Type: Async`；最终结果发送到 `callbackConfig.callbackUrl`。HTTP 202 接受响应可能无响应体，此时方法返回 `null`。

请求示例（临时凭证需在调用时动态获取）：

```json
{
  "imageUrl": "https://example.com/input.tif",
  "uploadConfig": {
    "ossConfig": {
      "stsToken": {
        "accessKeyId": "<temporary-access-key-id>",
        "accessKeySecret": "<temporary-access-key-secret>",
        "expiration": "<expiration>",
        "securityToken": "<temporary-security-token>"
      },
      "bucket": "<bucket>",
      "region": "oss-cn-hangzhou"
    },
    "uploadPath": "test/cwc/"
  },
  "callbackConfig": {
    "callbackUrl": "https://example.com/callback",
    "callbackCustomValue": { "id": "232" }
  }
}
```

同步调用的回调配置可省略；异步调用必须提供回调地址。调用前校验请求、图像 URL、上传配置、OSS 配置和上传路径非空。

结果和回调模型均使用服务文档中的字段名：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "downloadUrl": "https://example.com/test/cwc/cwc.tif",
    "callbackCustomValue": { "id": "232" }
  }
}
```

`callbackCustomValue` 复用现有模型，支持 `id`、`orderItemId`、`preprocessRequestId` 和 `presetType`。
订单回调接口：`POST /api/manufacturerSide/order/callback/convert_color_white_color`。
可通过 `external.callbackApi.convert_color_white_color` 配置完整回调地址；未配置时，沿用抠图回调的域名，将 `/callback/generate_mask_files` 替换为上述新路径。

`addOrderWithItems` 入库后沿用后台队列。订单项工艺节点的 `nodeName` 为“弱溶剂彩白彩”时，先提交彩白彩异步转换，本轮不继续灰度图转换或抠图。上传配置沿用其他算法的 STS 获取方式和 `pieceImg/{manufacturerId}/` 路径，`callbackCustomValue.id` 为 `orderItemId`，同时透传 `preprocessRequestId` 以过滤旧回调。

成功回调根据 `data.callbackCustomValue.id` 查找订单项，将 `data.downloadUrl` 同时更新到 `productionImgFile.rawFile` 和 `productionImgFile.filePreview.raw`，并持久化 `colorWhiteColorDownloadUrl`。更新成功后续处理任务入队，继续原有灰度图转 SVG、抠图等逻辑；生产图片已等于转换结果时跳过再次转换。

失败、缺少下载地址的回调不更新图片；已退回、已取消或预处理请求已过期的回调忽略。Redis 锁和完成标记避免并发及重复回调反复提交续处理任务。队列满时回调返回失败，可重试。

调用记录类型为 `convertColorWhiteColor` / `convertColorWhiteColorAsync`，`sourceId` 来自 `callbackCustomValue.id`。HTTP 成功与业务成功分开判断：调用方应检查返回的 `code` 是否为 `200`。

异步机制沿用已有算法约定，仍需使用有效临时授权及可访问的回调地址完成实际联调。
