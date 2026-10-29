# 印版生成加工清单

confirmLayout、confirmPrint、镜像印版和 confirmed 补偿重试共用请求构建入口。
原 partSliceConfigs 和 machiningConfigs 已由 forme.machiningManifest.items 替代，
不再发送旧字段或 path 层级。

```json
{
  "forme": {
    "machiningManifest": {
      "items": [
        {
          "compId": "6ae05980a36fe728113c40fd",
          "compType": "PART",
          "machiningType": "CUTTING",
          "knife": "SP7",
          "param": {
            "type": "GRID",
            "xs": [100, 200, 300, 400],
            "ys": [60, 120, 180, 240]
          }
        }
      ]
    }
  }
}
```

- compId：符合条件的生产零件自身 ID。
- compType：通过 MachiningCompType.PART 枚举生成 PART；FORME 为预留类型。
- machiningType：小块裁切生成 CUTTING。
- knife：默认 SP7。
- param.type：默认 GRID，覆盖工艺参数中的同名字段。
- param.xs/ys：沿用原“小块裁切”的参数和 cm → mm 转换，不修改数据库原参数。

只读取 PART 来源零件的“小块裁切”工艺节点，每个零件仅取首个有效参数，
无符合条件的配置时发送 machiningManifest: {"items": []}。

forme 的刀型、SVG、边距、标记、定位点，以及 outputs、uploadConfig、callbackConfig
继续使用原有业务规则和运行时配置，示例凭据不写入代码。
