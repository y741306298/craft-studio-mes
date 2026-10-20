# deliveryToken 电子面单账号配置接口

更新日期：2026-10-20（北京时间）。

## 检查结果

检查现有全部 Controller 后，未发现直接新增或更新 MES `deliveryToken` 集合的接口。
现有 `deliveryNet/config` 转发的是产品中心物流配置，不会调用 MES 的 `DeliveryTokenService`。
本次新增配置端接口，复用现有领域服务和 MongoDB 存储。

## 保存配置

`POST /api/configSide/delivery/deliveryToken/config`

请求头：

```http
Authorization: Bearer <配置端登录返回的token>
Content-Type: application/json
```

沿用 `ConfigSideAuthInterceptor`；须使用配置端登录凭据。
此处的登录 token 与电子面单账号配置 `deliveryToken` 是不同概念。

### 保存规则

- 按 `manufacturerMetaId + carrierId` 查找，存在则覆盖配置，不存在则新增。
- 更新保留记录 ID 和创建时间；请求不接受数据库 ID、创建时间或更新时间。
- 这是完整配置保存，不是局部更新。未传的可选字段会被清空，更新时应提交完整配置。
- 工厂 ID 和承运商 ID 首尾空白会移除；账号、密码和密钥内容保持原样。
- 成功仅返回记录 ID，不回传电子面单密码和密钥。
- 沿用既有仓储查找及保存方式，没有新增唯一索引。请勿对同一组合并发首次配置；已有重复数据应先核查，接口不会自动合并。

### 请求示例

```json
{
  "manufacturerMetaId": "factory-id",
  "carrierId": "carrier-id",
  "partnerId": "monthly-account",
  "kuaidicom": "shunfeng",
  "partnerKey": "account-password",
  "partnerSecret": "account-secret",
  "partnerName": "account-name",
  "siid": "outlet-id",
  "net": "outlet-name",
  "tempId": "main-template-id",
  "expType": "标准快递",
  "payType": "月结"
}
```

示例值须替换为快递公司或快递100要求的实际值。不同快递公司的账号与网点要求不同。

### 字段

| 字段 | 必填 | 含义 |
| --- | --- | --- |
| manufacturerMetaId | 是 | 工厂 ID；须与下单使用的工厂 ID 一致 |
| carrierId | 是 | 承运商 ID；不是快递公司的编码 |
| partnerId | 是 | 月结账号 |
| kuaidicom | 是 | 快递公司编码，如 shunfeng |
| partnerKey | 否 | 电子面单密码 |
| partnerSecret | 否 | 电子面单密钥 |
| partnerName | 否 | 电子面单客户账户名称 |
| printType | 否 | 打印类型 |
| siid | 否 | 默认网点 ID |
| net | 否 | 收件网点名称 |
| code | 否 | 电子面单承载编号 |
| checkMan | 否 | 电子面单承载快递员名 |
| tbNet | 否 | 网点名称或编号 |
| kuaidiWay | 否 | 快递方式 |
| tempId | 否 | 主单模板 |
| childTempId | 否 | 子单模板 |
| backTempId | 否 | 回单模板 |
| expType | 否 | 运输方式 |
| payType | 否 | 支付方式 |

以上配置字段均为字符串。可选不表示所有快递公司都可以省略：承运商必需的信息仍须填写。

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": "saved-delivery-token-id",
  "timestamp": 1792454400000
}
```

缺少必填字段按项目参数校验规则返回错误；未登录或配置端登录失效返回 HTTP 401。
保存成功仅表示配置已入库，不代表已向快递100验证账号有效性。

### curl

```bash
curl -X POST 'http://localhost:8081/api/configSide/delivery/deliveryToken/config' \
  -H 'Authorization: Bearer <config-side-token>' \
  -H 'Content-Type: application/json' \
  --data-binary @delivery-token.json
```

将完整请求放入本地 `delivery-token.json`，不要将真实密码或密钥提交到版本库。

## 生效路径

`AppDeliveryPkgService` 按承运商和工厂调用
`DeliveryTokenRepository.findByCarrierIdAndManufacturerMetaId` 读取配置，再组装快递100下单参数。
保存后后续读取会使用新配置，不需重启。实际选择了指定网点配置时，`siid` 可被覆盖。
本接口只保存账号配置，不触发面单重下、补生成生产零件或重新提交预下单任务。

## 验证

新增 `DeliveryTokenServiceTest`，覆盖首次配置、更新时保留 ID/创建时间、必填字段缺失拒绝写入。
在具备项目要求的 JDK 22、Maven 和依赖的环境执行：

```bash
mvn -pl craftstudio-mes-domain -am -Dtest=DeliveryTokenServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

本次执行环境没有 Maven，且只有 JDK 21，因此未运行编译和单元测试；已完成静态检查。
