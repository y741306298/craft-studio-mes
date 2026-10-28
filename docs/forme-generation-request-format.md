# 确认排版、确认打印的印版生成请求

`confirmLayout` 和 `confirmPrint` 均通过
`buildConfirmFormeGenerationRequest` → `buildFormeGenerationRequest`
组装算法请求，镜像印版和 confirmed 补偿重试也使用该入口。

## 与目标格式的对照

| 字段 | 当前行为及调整 |
| --- | --- |
| 顶层 | 已有 `forme`、`outputs`、`uploadConfig`、`callbackConfig` |
| `forme.knife` | 已有，按业务规则选择刀型，不固定为示例中的 SP3 |
| `forme.svgUrl` | 已有，使用当前排版 nestedSvg 的完整 OSS URL |
| `forme.margin/marks/anchorPoints` | 已有，按排版模式生成；定位点已有 img、type、svg、size、position |
| `forme.partSliceConfigs` | 已移除，原“小块裁切”配置全部迁移到 machiningConfigs |
| `forme.machiningConfigs` | 从符合条件的零件“小块裁切”工艺生成；compId 为零件 ID，compType 固定为 forme&#124;part，knife 默认 SP7，path.type 默认 XY，path.param 为原 xs/ys 参数；无配置时为空数组 |
| `outputs.json` | 已有 objectName 和 env.dtp/basePath；实际使用相对 images 目录，不固定为示例中的本地磁盘路径 |
| `outputs.plt` | 已有 direction=h 和 objectName.normal/reverse |
| `outputs.formeSvg` | 已有 objectName；各输出项仍按排版模式的文件需求生成 |
| `uploadConfig` | 使用运行时 OSS 临时授权和当前模式的上传目录，不使用示例凭据 |
| `callbackConfig` | 使用配置的回调 URL，callbackCustomValue.id 为当前排版 ID |

新增字段在无加工配置时的实际 JSON：

```json
{
  "machiningConfigs": []
}
```

有配置时的单项结构如下；`compType` 按当前协议传完整字面值 `forme|part`，
由 `MachiningCompType.FORME_OR_PART` 枚举提供编码。

```json
{
  "compId": "3232123",
  "compType": "forme|part",
  "knife": "SP7",
  "path": {
    "type": "XY",
    "param": {
      "xs": [100, 200, 300, 400],
      "ys": [60, 120, 180, 240]
    }
  }
}
```

沿用原有零件筛选和去重规则：只处理 PART 来源的零件，每个零件取首个有效“小块裁切”参数，
只映射 xs/ys，并保持原有 cm → mm 转换；只修改请求副本，不修改库存工艺参数。

目标示例中的注释、HTML 空格实体和 Markdown 链接不是 JSON 内容；实际请求由
`JSON.toJSONString` 序列化，URL 为普通字符串，Windows 路径中的反斜杠会自动转义。
