package com.mes.interfaces.api.platform.configSide.auth;

import com.mes.application.command.auth.AppLoginService;
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

    private final AppLoginService appLoginService;

    public ConfigSideAuthController(AppLoginService appLoginService) {
        this.appLoginService = appLoginService;
    }

    /**
     * 配置端登录接口。
     *
     * @param request 登录请求参数
     * @return 登录结果
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(appLoginService.login(request));
    }

    /**
     * 根据 token 查询用户所属工厂ID。
     *
     * @param token 登录令牌
     * @return 用户所属工厂ID
     */
    @GetMapping("/token/manufacturerMetaId")
    public ApiResponse<String> getManufacturerMetaIdByToken(@RequestParam @NotBlank(message = "token不能为空") String token) {
        return ApiResponse.success(appLoginService.getManufacturerMetaIdByToken(token));
    }
}
