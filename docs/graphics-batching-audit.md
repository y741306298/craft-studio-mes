# 四类图形功能的任务粒度检查

检查对象：MES 自身实现并迁移到 imgUtil 的 PNG 绘制、文字标签、条纹/定位标记、SVG 蒙版与编辑。原有外部抠图、出血、套料、制版等算法不计入本次统计。

检查基于 MES c974c918 和图形服务 f9a66bb 的静态调用链。本报告记录现状和后续修复范围；本轮没有修改下列业务实现，也没有对全部生产工艺进行端到端验证。

## 结论

四类功能均存在同一业务对象被拆成多个远程图形任务的路径。已经完成的 SPLICE_PIECE 只覆盖 SuperWidthSpliceProcessService 的拼接回调预处理，不能代表其他工艺已经合并。

合并单位应为一个零件的完整装饰、一个印版的一组素材、一个最终 SVG 编辑结果。每次业务提交生成一个图形任务，所有素材及最终文件准备完成后统一回调；绘制和释放按顺序执行，共用超时、总文件大小及队列限制。画布像素上限已按后续要求移除。

下述次数指首次执行、图形步骤未命中持久化结果缓存时的 POST 提交数，不是日志行数。相同 workflowId 内，相同操作和参数会复用已接收结果；不同参数仍会分别提交。片段 SVG 的 GET 下载也会增加开销，但不计入 POST 次数。

## 逐条发现

### 1. 留白：边框、四边标签、裁剪、旋转、SVG 合成分开提交（高优先级）

入口：`craftstudio-mes-application/src/main/java/com/mes/application/command/typesetting/proces/liubai/AbstractFixedLiubaiProcessStrategy.java` 的 process、uploadLiubaiTagAssets、buildExpandedSvg。

默认生成边框和非空文字标签时，依次调用：

- LIUBAI_BORDER：1次。
- LIUBAI_TAG：1次，文字测量实际依靠生成 PNG。
- TRANSFORM：上边180度、左边90度、右边负90度，共3次。
- 横向/竖向裁剪：按需要额外0–2次；相同竖向裁剪重复调用会命中结果缓存。
- LIUBAI_EXPAND：1次，完成后还需 GET 下载 SVG。
- uploadTagSvg 实际调用 SVG_SAVE：1次。

合计通常7–9次提交。具体规格可禁用边框或标签，因此不能将7–9当成所有留白工艺的固定次数。继承该基类的材料/包边/易拉宝等策略也需随基类一起核对。材料规格的后续缩放还存在第6项的额外链路。

修复范围：将字体测量、按边长裁剪、四向旋转、边框生成、SVG 合成及上传放在同一个零件任务；文字和工艺判断仍由 MES 提供。一次返回最终 SVG、边框和四边标签地址。验证全部结果后再更新 mask、marks 和宽高。

### 2. 热升华：循环内逐点提交，而且标签复用留白的细粒度实现（高优先级）

入口：`craftstudio-mes-application/src/main/java/com/mes/application/command/orderPreprocessing/sublimation/SublimationDecorationService.java` 的 decorate 和 appendLabels。

当前 SUB_OPEN、SUB_ORIGINAL、每个 SUB_POINT、SUBLIMATION_BORDER、SUB_BORDER、SVG_SAVE 均分别提交。N个点在不计文字标签时，需要N+5次；需要反面时，SUB_BACK 和 SVG_SAVE 再增加2次。

工艺标签和追溯码又调用 uploadLiubaiTagAssets，继续产生字体测量、裁剪、旋转及 LIUBAI_GROUPS 请求。追溯码的宽度测量也通过 LIUBAI_TAG 实际生成图片。同参数在同一工作流内可能复用，但整个链路仍有多次前后依赖。

修复范围：每个热升华零件一次提交全部点位、标签、追溯码及正反面参数；一次返回正面蒙版、可选反面蒙版和所有素材。必须保留反面不带工艺标记的现有规则。

### 3. 印版 PNG / 标签素材：同一 build 仍逐素材提交（中优先级）

目录：`craftstudio-mes-application/src/main/java/com/mes/application/command/typesetting/layout/`。

| 实现 | 当前提交情况 |
| --- | --- |
| CaifuLayoutBuildService | 两张 BLACK，2次 |
| CaifuA30LargeBoardLayoutBuildService | 两张 BLACK，2次 |
| CaifuA30SmallGraphLayoutBuildService | 两张 BLACK，2次 |
| NineSegmentRollLayoutBuildService | 两张 BLACK，2次；createBlackPng 继承自 CaifuLayoutBuildService |
| LiuduLayoutBuildService | 三张 BLACK，3次 |
| CaifuOpenBackA30HFilmLayoutBuildService | 三张基础 BLACK、两张 CAIFU_TAG，基础5次；不同高度的分段黑条可增加提交 |
| CaifuOpenBackA30HNoFilmLayoutBuildService | 三张基础 BLACK；启用文字条时增加两张 CAIFU_TAG；不同高度黑条可增加提交 |
| CaifuOpenBackA30ProNoFilmLayoutBuildService | 继承 NoFilm 的上述行为 |
| CircleQrLayoutBuildService | 正反方向两张 CIRCLE_TAG，2次 |
| CrossQrLayoutBuildService | 正反方向两张 CROSS_TAG，2次 |
| DoubleSideMountingLayoutBuildService | 正反方向两张 DOUBLE_TAG，2次 |

注意：这些 layout 已经在多个位置复用同一个素材 URL，并非每个 marks.add 都触发请求。圆形/十字/双面标签内部的文字、二维码和旋转也已经在图形服务内合成，剩余问题是同一印版的多个输出仍分开提交。

修复范围：每个印版一次提交所有唯一素材的参数，按尺寸和内容去重，统一返回素材地址映射。保留正反方向不同文字、颜色、DPI、尺寸和二维码内容。

### 4. 套料分隔标记：PNG、SVG、再次保存分成3次（中优先级）

入口：`craftstudio-mes-application/src/main/java/com/mes/application/command/typesetting/nesting/CaifuOpenBackA30HFilmNestingComposeService.java` 的 uploadCaifuOpenBackMarker。

当前依次执行 NESTING_MARKER、NESTING_MARKER_SVG、SVG_SAVE，共3次提交，且中间 SVG 需要下载。NoFilm、ProNoFilm 的继承策略也受影响。

修复范围：一组分隔标记一次返回 PNG 和 SVG 两个文件；保留右侧30mm留白和左端偏移规则。多处插入标记仍复用同一组素材。

### 5. SVG 蒙版与打扣：生成/编辑后又提交保存（高优先级）

| 路径 | 代码入口 | 当前链路 |
| --- | --- | --- |
| 矩形蒙版 | orderPreprocessing/AppOrderPreprocessingService.generateAndUploadPathMaskSvg | SVG_MASK → 下载 → SVG_SAVE，2次提交 |
| 打扣 | typesetting/proces/buckle/AbstractBuckleProcessStrategy.process | BUCKLE_APPEND → 下载 → SVG_SAVE，2次提交 |
| 材料尺寸调整 | typesetting/proces/material/AbstractMaterialProcessStrategy.applyMaterialLayoutInset | SVG_RESIZE → 下载 → SVG_SAVE，2次提交 |
| 重做替换 ID | productionPiece/AppProductionPieceService.rewriteMarkedSvgForRedo | SVG_REDO_ID → 下载 → 有变化时 SVG_SAVE，1–2次提交 |
| 反面标记清理 | typesetting/AppTypesettingService.removeSpliceMarksFromNestedMirrorSvg | SVG_REMOVE_SPLICE → 可选 SUB_CLEAN → 有变化时 SVG_SAVE，1–3次提交 |

以上路径均在 `craftstudio-mes-application/src/main/java/com/mes/application/command/` 下。OssTagUploadService.uploadTagSvg 的各重载实际会再调用 RemoteGraphics.saveSvg，因此方法名中的“上传”不能被当作单纯传文件。

打扣已经一次携带全部扣点，不存在每个扣点单独提交的问题；冗余发生在最终保存。矩形蒙版和简单编辑通常可以直接使用生成任务返回的 OSS artifact。反面清理应将两个编辑步骤放在同一任务后只发布一次。重做及清理仍须保留无变化时复用原文件的语义。

### 6. 材料处理与留白串联（高优先级的跨步骤问题）

AbstractMaterialProcessStrategy.process 先调用父类留白流程，再 applyMaterialLayoutInset，后者会重新下载、编辑和保存已经生成的蒙版。合并留白时应一并规划最终根尺寸，避免只修复基类后又保留第二轮远程处理。

### 7. 旧排版拼接服务（当前未确认进入实际业务调用链）

`typesetting/service/SuperWidthSpliceMarkService` 内部仍存在 STRIPES_PX、MARK_TEXT、TRANSFORM/FLOOR 等逐项远程操作。

全项目静态搜索没有找到该类在业务代码中的引用或 apply 调用方，不能据此认定它正在产生生产请求。该类仍是 Spring Service；后续需确认是否保留，避免重新接入时恢复旧调用方式。它也不等同于已经改好的 SuperWidthSpliceProcessService。

## 已完成的部分

SuperWidthSpliceProcessService 已使用 SPLICE_PIECE：一个零件的8种 PNG 素材、旋转和最终 SVG 共用一个任务，内部不创建子任务，统一回调。ImgUtilClient.splicePiece 当前按8个素材结果校验，是拼接专用适配，不适合直接套在输出数量不同的留白、热升华或印版流程上。

AlgorithmCoreApiService 已统一提交、调用记录及回调关联；更改任务粒度时应继续沿用该入口。API Key、签名回调及队列上限继续生效。

## 后续修改的验收口径

1. 同一个零件完整装饰或同一个印版素材集合只提交一个图形任务；不同业务对象按各自编号隔离。
2. 一个任务内按顺序生成素材并释放画布，不通过异步并发逐原语提交来模拟批量。
3. 任一素材失败时不回写该业务对象的部分 mask/marks；共用任务超时、总文件大小及存储清理。
4. 成功回调返回最终 SVG 和全部素材地址；绘图完成后不再下载中间 SVG 并调用 SVG_SAVE。
5. 按同一 taskId 的真实 POST 调用记录统计次数；区分缓存命中日志、回调重试、业务对象数量和绘图任务数量。
6. 使用生产样本比较字体、尺寸、DPI、裁剪、旋转、负 viewBox、img/data-forme/require-plt 元数据、反面清理及各工艺顺序。
