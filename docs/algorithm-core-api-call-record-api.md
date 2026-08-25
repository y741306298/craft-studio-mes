# AlgorithmCoreApiCallRecord 查询接口

## 查询最近一次调用记录

- **请求方法**：`GET`
- **请求路径**：`/api/algorithmCoreApiCallRecord`
- **查询参数**：
  - `type`：必填，算法调用类型，取值见下表。
  - `sourceId`：必填，业务来源 ID；不同 `type` 对应的字段含义见下表。
- **查询规则**：同时精确匹配 `type` 和 `sourceId`，按 `createTime` 倒序返回最近一条未删除记录。没有匹配记录时 `data` 为 `null`。

示例：

```http
GET /api/algorithmCoreApiCallRecord?type=generateMaskFilesAsync&sourceId=ORDER_ITEM_001
```

## type 枚举

| type | 说明 | sourceId 来源 |
| --- | --- | --- |
| `generateMaskFilesAsync` | 异步图片遮罩抠图 | `callbackCustomValue.orderItemId` |
| `generateMaskFilesSync` | 同步图片遮罩抠图 | `callbackCustomValue.orderItemId` |
| `convertGrayImgToSvgAsync` | 异步灰度图转 SVG | `callbackCustomValue.orderItemId` |
| `convertGrayImgToSvg` | 同步灰度图转 SVG | `callbackCustomValue.orderItemId` |
| `generateNestedFilesAsync` | 异步通用排版 | `callbackCustomValue.id` |
| `generateNestedFilesSync` | 同步通用排版 | `callbackCustomValue.id` |
| `generateGridNestedFilesAsync` | 异步网格排版 | `callbackCustomValue.id` |
| `generateRectNestedFilesAsync` | 异步矩形排版 | `callbackCustomValue.id` |
| `generateVerticalNestedFilesAsync` | 异步竖排排版 | `callbackCustomValue.id` |
| `generateFormeAsync` | 异步生成刀模 | `callbackCustomValue.id` |
| `generateForme` | 同步生成刀模 | `callbackCustomValue.id` |

## 返回数据

`data` 为 `AlgorithmCoreApiCallRecord`，主要字段如下：

| 字段 | 说明 |
| --- | --- |
| `mode` | 调用模式：`sync` 或 `async` |
| `url` | 实际请求地址 |
| `apiPath` | 算法 API 路径 |
| `requestBody` | JSON 字符串格式的请求体快照 |
| `callbackCustomValue` | JSON 字符串格式的回调自定义数据 |
| `type` | 算法调用类型 |
| `sourceId` | 从回调自定义数据解析出的业务来源 ID |
| `createTime` | 记录创建时间 |

当 `type` 不在枚举内，或任一查询参数为空时，接口返回参数错误。
