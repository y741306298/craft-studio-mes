# 聚水潭打印身份锁与客服恢复

所有使用同一 Redis 的 MES 实例共享 JST 打印身份锁。首次预打印、addPkg 即时打印、复用面单时重新获取打印数据，以及普通复打入口，均经过 JstPrintGuard。

当上游抛出 SmsCodeRequiredAuthException（包括被其他异常包装的情况）时，写入不带过期时间的 `gatherPlatform:JST:smsAuthRequired`。后续普通 JST 打印在请求聚水潭前被拒绝；WDT 不受影响。身份锁不会因重启应用或客服打印成功而自动解除，Redis 数据须由部署环境持久化。

身份锁与操作互斥锁是两个不同的 Redis key。JST 打印、客服恢复打印及验证码校验通过 `gatherPlatform:JST:printOperation` 串行执行，避免校验解锁与其他打印异常相互覆盖。并发请求立即提示稍后重试；操作互斥租约为 30 分钟，用于进程崩溃后的恢复，正常操作须在租约内完成。Redis 不可用时不会继续请求 JST。

预打印因身份锁、验证码异常或操作互斥而暂停时，保持任务 PENDING，不增加普通打印失败次数，也不把身份错误写入订单快递单号。解锁后由后续定时批次继续处理。非身份类打印错误沿用原来的失败处理。

## 客服操作

以下接口沿用 configSide 的权限校验，两个基础路径均可使用：

- `/api/configSide/wdt`
- `/api/configSide/gather-platform/logistics-config`

### 1. 重试 JST 打印以触发上游短信身份校验

`POST /api/configSide/wdt/retryJstPrintForSmsAuth?orderId=10001`

`orderId` 为必填的 MES 订单号，不是聚水潭渠道订单号。仅接受聚水潭聚单订单；测试环境禁止真实调用。

接口绕过身份锁，仍受操作互斥控制。存在面单时，复用 addPkg 中的复打逻辑，携带原运单号请求 `rePrint`，并校验返回运单号一致；没有面单时，复用 addPkg 首次打印和配置换仓逻辑。它实际请求聚水潭，由上游决定是否发送新短信并抛出验证码异常；不会通过本地伪造异常来发送短信。

需要验证码或仍锁定时，HTTP 423：

```json
{"code":423,"message":"聚水潭打印已锁定，请联系客服校验身份","data":null,"timestamp":1792036800000}
```

如果上游已授权、打印成功，返回 `ApiResponse<WdtLabelRecord>` 并保存面单记录，供后续 addPkg 使用；不创建包裹、不核销工件，也不自动解除身份锁。

操作正在执行时，HTTP 423，message 为“聚水潭正在打印或校验身份，请稍后重试”。

### 2. 提交收到的验证码并解锁

`POST /api/configSide/wdt/smsCodeAuth?smsCode=8203`

`smsCode` 必填，仅允许数字。调用 `WebSimulator.getLoggedInstance().obtainAuthSid(smsCode)`；只有正常完成后才删除身份锁。校验失败则保留原锁。

成功响应：

```json
{"code":200,"message":"success","data":"success","timestamp":1792036800000}
```

解锁不会自动重放失败的 addPkg 或人工复打请求，调用方需重新提交；待处理预打印任务在后续定时批次恢复。

## 验证

新增 JstPrintGuardTest，覆盖身份异常全局上锁、包装异常、普通异常不加锁、WDT 不受影响、客服绕过身份锁、校验成功解锁、校验失败保留锁及并发互斥；扩展 GatherPlatformLogisticsTest 验证普通复打被拦截及客服携带原运单号复打。测试均模拟上游，不发送真实短信或打印面单。

当前云环境缺少内部 shared 1.1.5、pangolin 0.1.0 构件，Maven 在依赖解析阶段失败，尚未执行测试与完整编译。
