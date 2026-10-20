package com.mes.interfaces.api.platform.configSide.delivery;

import com.mes.application.command.delivery.req.DeliveryTokenConfigRequest;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryPkg.service.DeliveryTokenService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 配置端电子面单账号配置，沿用 ConfigSideAuthInterceptor 登录校验。 */
@RestController
@RequestMapping("/api/configSide/delivery/deliveryToken")
public class DeliveryTokenController {
    private final DeliveryTokenService deliveryTokenService;

    public DeliveryTokenController(DeliveryTokenService deliveryTokenService) {
        this.deliveryTokenService = deliveryTokenService;
    }

    /** 按工厂和承运商保存完整配置，仅返回记录 ID。 */
    @PostMapping("/config")
    public ApiResponse<String> config(@Valid @RequestBody DeliveryTokenConfigRequest request) {
        return ApiResponse.success(deliveryTokenService.configureDeliveryToken(request.toDomainEntity()).getId());
    }
}
