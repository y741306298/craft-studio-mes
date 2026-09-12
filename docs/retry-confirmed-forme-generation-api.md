# 重试 confirmed 印版生成接口

## 接口

`POST /api/manufacturerSide/typesetting/forme/retryConfirmed`

请求体必须提供创建时间范围。接口查询范围内所有 `status=confirmed` 的印版，并根据每条记录保存的 `layoutMode`、
`element.nestedSvg` 和 `remark` 重新构建、提交印版生成请求。

```json
{
  "startTime": "2026-09-12T00:00:00.000Z",
  "endTime": "2026-09-12T23:59:59.999Z"
}
```

`startTime` 和 `endTime` 都是必填字段，查询包含两个时间边界，且开始时间不能晚于结束时间。
重试和 `confirmPrint`、`confirmLayout` 共用同一个印版请求组装入口，确保 forme、outputs、上传配置、
回调配置、marks 和定位点参数保持一致。

支持的操作标记：

- `FORME_OP:LAYOUT`：重新生成确认排版印版；
- `FORME_OP:PRINT:<deviceCode>`：重新生成确认打印印版。

提交前会重新读取记录；如果记录已被回调更新为其他状态，该记录会计入 `skipped`，不会重复提交。
单条记录失败不会中断后续记录。

## 响应字段

| 字段 | 含义 |
| --- | --- |
| `total` | 本次扫描到的 confirmed 印版数 |
| `submitted` | 成功重新提交数 |
| `skipped` | 提交前状态已经发生变化的跳过数 |
| `failed` | 构建或提交失败数 |
| `failures[].id` | 失败印版 ID |
| `failures[].message` | 失败原因 |

示例：

```json
{
  "code": 200,
  "data": {
    "total": 3,
    "submitted": 2,
    "skipped": 0,
    "failed": 1,
    "failures": [
      {
        "id": "6aa534a620108ba79ad79600",
        "message": "印版缺少 nestedSvg，无法重新生成"
      }
    ]
  }
}
```
