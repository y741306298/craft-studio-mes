# 矩形旗缝套筒预处理

订单新增后沿用 `OrderPreprocessTaskQueue` 后台队列，顺序为：彩白彩转换（如需）→ 矩形旗缝套筒出血（如需）→ 灰度蒙版转 SVG → 原有抠图/拼接或直接生成零件。两个异步转换阶段均等待各自回调后重新入队，读取最新订单项。

仅当 `nodeName` 包含“矩形旗缝套筒”，且该节点 `paramConfigs[].param.type=RECT_EDGE_SELECTION` 时提交矩形出血算法。不依赖“热升华”节点。缺少选边配置时直接继续原流程；选边配置存在但缺少 `param.edges` 时按失败任务重试。

请求字段：

| 字段 | 来源/默认值 |
| --- | --- |
| `imageUrl` | `orderItem.productionImgFile.rawFile` |
| `bloodConfig.scrollEdge` | 同节点 `RECT_EDGE_SELECTION.param.edges`，转换为远端契约的单边枚举；支持边名或单元素列表 |
| `bloodConfig.scrollColorHex` | 同节点任一 `ACC.param.accessorySnapshot.name` 为“大白边”时为 `#FFFFFF`，否则 `null` |
| `bloodConfig.scrollWidth` | `20` 毫米（2cm） |
| `bloodConfig.sewWidth` | `10` 毫米（1cm） |
| `bloodConfig.mirrorRequired` | `true` |
| `uploadConfig` | 沿用 STS；文件路径为 `orderItem/rectBlood/{manufacturerId}/{orderItemId}/{preprocessRequestId}/{uuid}.tif` |
| `callbackConfig.callbackCustomValue` | `id=orderItemId`，同时携带 `orderItemId` 和 `preprocessRequestId` |

算法地址读取 `external.api.rectBloodUrl`，由 `AlgorithmCoreApiService.generateRectBloodAsync` 提交。回调地址可配置 `external.callbackApi.rect_blood`，未配置时从抠图回调地址 `/callback/generate_mask_files` 推导为 `/callback/rect_blood`，保留原控制器路径前缀。

`OrderController.handleRectBloodCallback` 接收 JSON，成功报文为 `code=200`、`data.flagUrl`、可选的 `data.mirrorFlagUrl`、`data.callbackCustomValue`。算法需原样透传 `callbackCustomValue`，其中 `preprocessRequestId` 用于校验旧回调；兼容此前仅透传 `id` 的调用。

回调先保存订单项，再提交续处理任务：

- `flagUrl` 写入 `productionImgFile.rawFile` 和 `rectBloodDownloadUrl`，保留 `productionImgFile.filePreview` 原值。
- `mirrorFlagUrl` 写入 `mirrorFlagUrl`。抠图时传入 `rawImage.mirrorUrl`；直接生成零件时写入零件的 `mirrorConfigs`。
- 当前生产图等于已保存的矩形出血结果时跳过重复转换，同时避免因图片地址变化再次触发彩白彩转换。
- 沿用 Redis 回调锁和完成标记；取消、退回或旧版本回调不更新订单项；续处理入队失败可重试回调。

已移除旧的热升华缝管套/道旗缝边解析、SVG 本地外扩、描边和标签处理。保留热升华只能大视觉排版、不能混排的校验，以及双面对裱节点匹配中的热升华布料名称排除规则。

## 出血后的矩形尺寸

原材料 `usageSize3D` 的厘米宽高先乘以 10 转为毫米。仅在当前生产图是矩形出血回调结果时计算外扩尺寸，始终保留原材料规格不变：

- 缝套边选 LEFT/RIGHT：宽 = 原宽 + scrollWidth + sewWidth + 4，高 = 原高 + 2 × sewWidth + 4。
- 缝套边选 TOP/BOTTOM：宽 = 原宽 + 2 × sewWidth + 4，高 = 原高 + scrollWidth + sewWidth + 4。
- 上述 4mm 为每个方向两侧各 2mm 黑边，不在 MES 重复绘制算法已经生成的黑边。

矩形 SVG 的 width/height、viewBox 和闭合路径使用计算后的尺寸；无抠图与双面直接生成的工件宽高也使用同一结果。拼接/抠图算法已返回的零件 SVG 尺寸优先保留，缺失时才使用生产图尺寸兜底。重复生成不会在原材料规格上累加外扩。

例：原图 700×300mm、TOP、scrollWidth=20mm、sewWidth=10mm，最终为 724×334mm。
