package com.mes.interfaces.api.platform.configSide.auth;

import com.mes.application.command.auth.AppConfigLoginService;
import com.mes.application.dto.req.auth.AddConfigUserRequest;
import com.mes.application.dto.req.auth.LoginRequest;
import com.mes.application.dto.resp.auth.LoginResponse;
import com.mes.domain.base.repository.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configSide/auth")
public class ConfigSideAuthController {

    private final AppConfigLoginService appConfigLoginService;

    public ConfigSideAuthController(AppConfigLoginService appConfigLoginService) {
        this.appConfigLoginService = appConfigLoginService;
    }

    /**
     * 配置端登录接口。
     *
     * @param request 登录请求参数
     * @return 登录结果
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(appConfigLoginService.login(request));
    }

    /**
     * 新增配置端登录账号。
     *
     * @param request 新增配置端用户参数
     * @return 操作结果
     */
    @PostMapping("/user/add")
    public ApiResponse<String> addConfigUser(@Valid @RequestBody AddConfigUserRequest request) {
        appConfigLoginService.addConfigUser(request);
        return ApiResponse.success("success");
    }

    /**
     * 根据 token 查询配置端用户ID。
     *
     * @param token 登录令牌
     * @return 配置端用户ID
     */
    @GetMapping("/token/configUserId")
    public ApiResponse<String> getConfigUserIdByToken(@RequestParam @NotBlank(message = "token不能为空") String token) {
        return ApiResponse.success(appConfigLoginService.getConfigUserIdByToken(token));
    }
}
