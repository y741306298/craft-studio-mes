# 热升华矩形旗组合工艺

订单仍由 `addOrderWithItems` 入库并提交预处理队列。彩白彩（如需）完成后，热升华组合工艺汇总参数并调用 `generateRectBloodAsync`，结果进入现有 `/callback/rect_blood` 的 `handleRectBloodCallback`。出血完成后才生成矩形零件、扣点/绑带点、灰色工艺标签及最外圈2mm黑边。

## 匹配及参数来源

工艺节点按 `nodeOrder` 排序（相同顺序保持原列表顺序）。名称包含“热升华”即识别为主工艺，只读取其后续节点。下面的特殊工艺全部精确匹配，不用子串匹配：

| 后续工艺 | 处理 |
| --- | --- |
| 缝管套 | 从后续独立工艺节点“白色直径Xcm／彩色直径Xcm”读取X；`scrollWidth=X×10` 毫米。白色的 `scrollColorHex=#ffffff`，彩色为 `null`，不再读取缝管套配件 |
| 缝布带管套 | `scrollWidth=10mm` |
| 单针锁边 | `sewWidth=20mm` |
| 双针锁边、三线锁边、四线锁边 | `sewWidth=30mm` |
| 双面缝、双面夹黑缝 | 保留正反面独立资源，调用预留拼合入口，当前不合成单张图片 |
| 反面相同画面 | 正面算法设置 `mirrorRequired=true`，使用其 `mirrorFlagUrl` |
| 反面不同画面 | 读取同节点 `param.file.rawFile`（缺省沿用 `param.file.filePreview.raw`），按与正面相同的缝套边和外扩参数单独提交算法 |
| 旗帜四角打扣 | 四角扣点中心距相邻两边均20mm |
| 旗帜打扣、缝绑带 | 按对应节点选边生成扣点或绑带点，使用独立标记key |

缝套、打扣和绑带的选边与“矩形旗缝套筒”相同：从各自节点的 `param.type=RECT_EDGE_SELECTION` 读取 `param.edges` 数组，例如 `["TOP"]`。打扣/绑带可以选择多边，缝套必须是一条边。“缝管套”颜色由后续规格节点决定；“缝布带管套”仍固定白色、外扩10mm。没有锁边时 `sewWidth=0`；多种锁边组合取最大的外扩值，不累加。多个缝套节点仅允许相同的边及相同的直径，冲突时报配置错误。

示例（仅展示 `procedureFlow.nodes`）：

```json
[
  {"nodeName":"热升华（单面布）","nodeOrder":1},
  {"nodeName":"缝管套","nodeOrder":2,"paramConfigs":[
    {"param":{"type":"RECT_EDGE_SELECTION","edges":["TOP"]}}
  ]},
  {"nodeName":"白色直径3cm","nodeOrder":3},
  {"nodeName":"单针锁边","nodeOrder":4},
  {"nodeName":"旗帜打扣","nodeOrder":5,"paramConfigs":[
    {"param":{"type":"RECT_EDGE_SELECTION","edges":["LEFT","RIGHT"]}}
  ]}
]
```

新组合工艺不要求有缝套。没有缝套时传 `scrollEdge=null`、`scrollWidth=0`，`sewWidth` 作用于四边；只有热升华而没有锁边或缝套时仅计入四周2mm黑边。暂不支持异形切割或拼接旗帜，命中时明确报错。原有“热升华 + 矩形旗缝套筒”且没有上述新增特殊工艺的订单沿用原入口，参数默认值不变。普通订单继续走原策略。

## 矩形尺寸与点位

原材料 `usageSize3D` 的厘米宽高乘10得到毫米值，不修改材料原始规格。选中缝套边外扩 `scrollWidth`，其余三边外扩 `sewWidth`，四边各再计入2mm黑边：

- 套筒边为TOP/BOTTOM：宽 = 原宽 + 2×sewWidth + 4，高 = 原高 + scrollWidth + sewWidth + 4。
- 套筒边为LEFT/RIGHT：宽 = 原宽 + scrollWidth + sewWidth + 4，高 = 原高 + 2×sewWidth + 4。
- 没有套筒边：宽 = 原宽 + 2×sewWidth + 4，高 = 原高 + 2×sewWidth + 4。

矩形SVG的width/height、viewBox、闭合路径、零件宽高以及正反面遮罩共用此尺寸。重处理从原始材料尺寸重新计算，不重复累加。外圈2mm黑边在该画布内部绘制，不再次扩大画布。

旗帜打扣/缝绑带点的中心距选中边30mm；两端点距相邻边50mm。按最终外扩后的边长确定中间点数：

| 边长L | 中间点数（不含两个端点） |
| --- | --- |
| L ≤ 2000mm | 1 |
| 2000 < L ≤ 3500mm | 2 |
| 3500 < L ≤ 4500mm | 3 |
| 4500 < L ≤ 6000mm | 4 |
| L > 6000mm | 4 + ceil((L−6000)/1000) |

中间点在两个端点之间等距分配。扣点沿用既有打扣的8mm点图及SVG分组元信息；绑带采用相同点图、独立的 `sublimation-tie-*` key。工件太小无法满足边距时明确报错。

有锁边时，灰色工艺文字写在锁边外扩区域；没有锁边时写在缝套区域。文字使用零件工艺流，字号为原来的50%，颜色由#808080调浅50%为#C0C0C0；标签带尺寸和贴边定位不变，复用留白的PNG文字渲染与靠外侧边的方向/定位，位于2mm黑边内侧。最后叠加黑边，相关标记不参与刀线切割。

## 双面与回调

正反面的缝套边相同。反面不同画面在 `sublimationBackSourceUrl` 保存原始来源，在 `sublimationBackDownloadUrl` 保存独立出血结果，并以结果更新 `mirrorFlagUrl`。两面可以任意顺序回调；共用订单项锁，按面记录完成标记，全部完成后才恢复生成零件。另一面提交临时失败时，已受理面在30分钟内不重复提交。

热升华请求的 `callbackCustomValue.presetType` 为 `SUBLIMATION_FRONT` 或 `SUBLIMATION_BACK`，同时将版本与面信息编码进id：

- 正面：`orderItemId#preprocessRequestId`
- 反面：`orderItemId#preprocessRequestId#SUBLIMATION_BACK`

算法即使只回传id，也能区分正反面及旧请求；仍建议原样透传全部 `callbackCustomValue`。沿用取消、退回、旧回调和重复回调检查。

上传文件仍使用UUID文件名；回调只更新生产图 `rawFile` 和处理结果字段，保留 `filePreview.raw`。反面遮罩与正面尺寸/点位相同，但其原图分组引用反面图片，避免排版时误用正面图。

`SublimationDoubleSideJoin` 是双面缝/双面夹黑缝的拼合扩展入口，当前实现只返回两面图片，`joined=false`。单面/双面材料分支及反面真实缝筒边验证均未实现。

缝管套规格仅关联该节点之后、下一个缝管套/缝布带管套节点之前的规格工艺；前面的规格不生效，缺失或直径/颜色冲突时报错。支持小数厘米，但换算结果必须为算法支持的整数毫米。

## 订单溯源号 mark

热升华组合策略额外生成独立的 `sublimation-trace-*` mark，内容为 `x-y-z`：

- `x`：参考 `confirmLayout` 的PLT名称号码来源，复用 `TypesettingSequencePoolService`，新增独立用途 `sublimation_trace`，每工厂按1～10000循环，不占用PLT文件名号码。相同工厂、相同订单共用一个x，首次分配后持久化，回调重试/重处理复用。
- `y`：获取该订单、该工厂下全部订单项（包括非热升华项），按数据库的createTime、_id升序从1开始编号。首次顺序冻结保存；新增订单项追加，既有编号不变。
- `z`：当前orderItem的总quantity，不是每份产品的递增序号。例：x=123、y=2、quantity=3，则三份产品均为`123-2-3`，不拆分生产零件。

x与订单项编号快照保存到独立MongoDB集合 `sublimation_trace_order`，不改写订单状态；按工厂使用Redis锁串行分配，避免多个订单项或多个实例同时分配同一订单身份。记录使用由工厂ID和订单ID生成的固定_id，订单范围不会串到其他工厂。

溯源文字复用现有热升华工艺文字渲染，字号为初始工艺mark的50%，灰度为#C0C0C0。只放在“缝管套/缝布带管套”指定的外扩边末端，距相邻边10mm、避开2mm黑边。没有这两种工艺时，跳过号码分配和mark生成。锁边上的原工艺文字不受影响；没有锁边、工艺文字也位于缝套边时，仍保留5mm间隔。不增加零件或遮罩宽高。正反面使用同一溯源号。

mark URL存入 `productionPiece.marks` 的`sublimation-trace-top/right/bottom/left`对应键；SVG分组附带 `data-trace-code`。工艺流原有`sublimation-tag-*` mark仍保留。画面尺寸不足以按现有字号完整显示号码时明确报错，避免生成被截断的溯源号。
