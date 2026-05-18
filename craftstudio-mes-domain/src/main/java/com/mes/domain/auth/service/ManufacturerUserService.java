package com.mes.domain.auth.service;

import com.mes.domain.auth.entity.ManufacturerUser;
import com.mes.domain.auth.repository.ManufacturerUserRepository;
import com.mes.domain.base.repository.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<ManufacturerUser> listByManufacturerMetaId(String manufacturerMetaId, String phone, long current, int size) {
        if (StringUtils.isBlank(manufacturerMetaId) || current <= 0 || size <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "分页参数或manufacturerMetaId不正确");
        }
        return manufacturerUserRepository.listByManufacturerMetaId(manufacturerMetaId, phone, current, size);
    }

    public long totalByManufacturerMetaId(String manufacturerMetaId, String phone) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "manufacturerMetaId不能为空");
        }
        return manufacturerUserRepository.totalByManufacturerMetaId(manufacturerMetaId, phone);
    }

    public void deleteById(String id) {
        ManufacturerUser user = manufacturerUserRepository.findById(id);
        if (user == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "用户不存在");
        }
        manufacturerUserRepository.delete(user);
    }

    public void updatePassword(String id, String password) {
        if (StringUtils.isBlank(id) || StringUtils.isBlank(password)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "用户ID或密码不能为空");
        }
        ManufacturerUser user = manufacturerUserRepository.findById(id);
        if (user == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "用户不存在");
        }
        user.setPassword(password);
        manufacturerUserRepository.update(user);
    }

    /**
     * 更新用户信息（只更新非null字段）
     * @param id 用户ID
     * @param name 用户名称
     * @param phone 手机号
     * @param isAdmin 是否管理员
     */
    public void updateUser(String id, String name, String phone, Boolean isAdmin) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "用户ID不能为空");
        }
        
        ManufacturerUser user = manufacturerUserRepository.findById(id);
        if (user == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "用户不存在");
        }
        
        // 只更新非null的字段
        boolean hasUpdate = false;
        
        if (name != null) {
            user.setName(name);
            hasUpdate = true;
        }
        
        if (phone != null) {
            user.setPhone(phone);
            hasUpdate = true;
        }
        
        if (isAdmin != null) {
            user.setIsAdmin(isAdmin);
            hasUpdate = true;
        }
        
        if (hasUpdate) {
            manufacturerUserRepository.update(user);
        }
    }
}
