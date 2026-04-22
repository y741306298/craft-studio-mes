package com.mes.interfaces.api.platform.auth;

import com.mes.application.command.auth.AppLoginService;
import com.mes.application.dto.req.auth.AddUserRequest;
import com.mes.application.dto.req.auth.LoginRequest;
import com.mes.application.dto.resp.auth.LoginResponse;
import com.mes.domain.base.repository.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AppLoginService appLoginService;

    /**
     * 登录接口文档
     * <p>
     * URL: {@code POST /api/auth/login}
     * </p>
     * <p>
     * 请求体字段：
     * <ul>
     *     <li>account: 登录账号（必填）</li>
     *     <li>password: 登录密码（必填）</li>
     * </ul>
     * 返回字段：
     * <ul>
     *     <li>token: 登录令牌</li>
     *     <li>manufacturerMetaId: 用户所属工厂ID</li>
     *     <li>tokenExpireAt: 令牌过期时间（默认3天）</li>
     * </ul>
     * </p>
     *
     * @param request 登录请求参数
     * @return 登录结果
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(appLoginService.login(request));
    }

    /**
     * 添加用户接口文档
     * <p>
     * URL: {@code POST /api/auth/user/add}
     * </p>
     * <p>
     * 请求体字段：
     * <ul>
     *     <li>account: 账号（必填）</li>
     *     <li>password: 密码（必填）</li>
     *     <li>manufacturerMetaId: 工厂ID（必填）</li>
     *     <li>name: 用户名称（可选）</li>
     *     <li>phone: 用户手机号（可选）</li>
     * </ul>
     * 返回字段：
     * <ul>
     *     <li>data: success</li>
     * </ul>
     * </p>
     *
     * @param request 新增用户参数
     * @return 操作结果
     */
    @PostMapping("/user/add")
    public ApiResponse<String> addUser(@Valid @RequestBody AddUserRequest request) {
        appLoginService.addUser(request);
        return ApiResponse.success("success");
    }
}
