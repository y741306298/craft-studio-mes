package com.mes.interfaces.api.platform.manufacturerSide.storageTank;

import com.mes.application.command.auth.AppLoginService;
import com.mes.application.command.transBox.AppStorageTankService;
import com.mes.application.dto.req.storageTank.StorageTankRequest;
import com.mes.application.dto.req.storageTank.StorageTankListRequest;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageTank;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/manufacturerSide/storageTank")
@RequiredArgsConstructor
public class StorageTankController {
    private final AppStorageTankService service;
    private final AppLoginService loginService;

    @Value("${mes.auth.default-authorization:Bearer craftstudio-mes-default}")
    private String defaultAuthorization;

    @Value("${mes.auth.default-manufacturer-id:69f956c00ff1ad90a9611464}")
    private String defaultManufacturerId;

    @PostMapping("/list")
    public PagedApiResponse<StorageTank> list(@RequestHeader("Authorization") String authorization,
                                            @RequestBody StorageTankListRequest request) {
        return service.list(manufacturer(authorization), request);
    }

    @GetMapping("/{id}")
    public ApiResponse<StorageTank> get(@RequestHeader("Authorization") String authorization, @PathVariable String id) {
        return ApiResponse.success(service.get(manufacturer(authorization), id));
    }

    @PostMapping("/add")
    public ApiResponse<StorageTank> add(@RequestHeader("Authorization") String authorization,
                                      @RequestBody StorageTankRequest request) {
        return ApiResponse.success(service.add(manufacturer(authorization), request));
    }

    @PostMapping("/edit")
    public ApiResponse<StorageTank> edit(@RequestHeader("Authorization") String authorization,
                                       @RequestBody StorageTankRequest request) {
        return ApiResponse.success(service.edit(manufacturer(authorization), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@RequestHeader("Authorization") String authorization, @PathVariable String id) {
        service.delete(manufacturer(authorization), id);
        return ApiResponse.success();
    }

    private String manufacturer(String authorization) {
        if (defaultAuthorization != null && !defaultAuthorization.isBlank()
                && defaultAuthorization.equals(authorization)) {
            if (defaultManufacturerId == null || defaultManufacturerId.isBlank()) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.unauthorized, "默认制造商未配置");
            }
            return defaultManufacturerId;
        }
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.unauthorized, "请先登录");
        }
        return loginService.getManufacturerMetaIdByToken(authorization.substring(7).trim());
    }
}
