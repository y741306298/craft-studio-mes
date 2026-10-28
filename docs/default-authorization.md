# 厂商端与配置端默认 Authorization

`/api/manufacturerSide/**` 和 `/api/configSide/**` 共用默认 Authorization。
请求头与配置的完整字符串精确匹配时，直接通过拦截器，不查询登录 token。

默认请求头：

```http
Authorization: Bearer craftstudio-mes-default
```

可通过 Spring Boot 配置覆盖：

```properties
mes.auth.default-authorization=Bearer your-custom-token
```

也可设置环境变量 `MES_AUTH_DEFAULT_AUTHORIZATION`，值为完整请求头内容（包含 `Bearer ` 前缀）。
配置为空或纯空白时关闭默认值放行。未匹配时继续原有登录 token 校验；原有免鉴权路径和 OPTIONS 处理保持不变。
此配置仅跳过接口拦截器鉴权，不生成用户身份或登录 token。
