package com.mes.domain.auth.service;

import com.mes.domain.auth.entity.ConfigUser;
import com.mes.domain.auth.repository.ConfigUserRepository;
import com.mes.domain.base.repository.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigUserService {

    @Autowired
    private ConfigUserRepository configUserRepository;

    public ConfigUser add(ConfigUser user) {
        if (user == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "用户不能为空");
        }
        if (StringUtils.isBlank(user.getAccount()) || StringUtils.isBlank(user.getPassword())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "账号或密码不能为空");
        }
        if (configUserRepository.findByAccount(user.getAccount()) != null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "账号已存在");
        }
        if (StringUtils.isNotBlank(user.getPhone()) && configUserRepository.findByPhone(user.getPhone()) != null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "该手机号已经注册，无法再次注册");
        }
        return configUserRepository.add(user);
    }

    public ConfigUser findByAccount(String account) {
        if (StringUtils.isBlank(account)) {
            return null;
        }
        return configUserRepository.findByAccount(account);
    }
}
