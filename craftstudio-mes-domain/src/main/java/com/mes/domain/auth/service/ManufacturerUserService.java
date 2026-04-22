package com.mes.domain.auth.service;

import com.mes.domain.auth.entity.ManufacturerUser;
import com.mes.domain.auth.repository.ManufacturerUserRepository;
import com.mes.domain.base.repository.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManufacturerUserService {

    @Autowired
    private ManufacturerUserRepository manufacturerUserRepository;

    public ManufacturerUser add(ManufacturerUser user) {
        if (user == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "用户不能为空");
        }
        if (StringUtils.isBlank(user.getAccount()) || StringUtils.isBlank(user.getPassword())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "账号或密码不能为空");
        }
        if (manufacturerUserRepository.findByAccount(user.getAccount()) != null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "账号已存在");
        }
        return manufacturerUserRepository.add(user);
    }

    public ManufacturerUser findByAccount(String account) {
        if (StringUtils.isBlank(account)) {
            return null;
        }
        return manufacturerUserRepository.findByAccount(account);
    }
}
