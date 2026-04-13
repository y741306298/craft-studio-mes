package com.mes.domain.delivery.deliveryPkg.service;

import com.mes.application.dto.resp.ApiResponse;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryTokenRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 电子面单令牌服务
 */
@Service
public class DeliveryTokenService {

    @Autowired
    private DeliveryTokenRepository deliveryTokenRepository;

    /**
     * 添加电子面单令牌配置
     * @param token 令牌配置
     * @return 保存后的令牌配置
     */
    public DeliveryToken addDeliveryToken(DeliveryToken token) {
        if (token == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "令牌配置不能为空");
        }
        if (StringUtils.isBlank(token.getPartnerId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "月结账号不能为空");
        }

        return deliveryTokenRepository.add(token);
    }

    /**
     * 更新电子面单令牌配置
     * @param token 令牌配置
     */
    public void updateDeliveryToken(DeliveryToken token) {
        if (token == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "令牌配置不能为空");
        }
        if (StringUtils.isBlank(token.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "令牌ID不能为空");
        }
        if (StringUtils.isBlank(token.getPartnerId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "月结账号不能为空");
        }

        deliveryTokenRepository.update(token);
    }

    /**
     * 删除电子面单令牌配置
     * @param id 令牌ID
     */
    public void deleteDeliveryToken(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }

        DeliveryToken token = deliveryTokenRepository.findById(id);
        if (token != null) {
            deliveryTokenRepository.delete(token);
        }
    }

    /**
     * 根据ID查询电子面单令牌配置
     * @param id 令牌ID
     * @return 令牌配置
     */
    public DeliveryToken findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }
        return deliveryTokenRepository.findById(id);
    }

    /**
     * 分页查询电子面单令牌配置列表
     * @param current 当前页码
     * @param size 每页大小
     * @return 令牌配置列表
     */
    public List<DeliveryToken> list(int current, int size) {
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在1-100之间");
        }
        return deliveryTokenRepository.list(current, size);
    }

    /**
     * 获取总数
     * @return 总数
     */
    public long total() {
        return deliveryTokenRepository.total();
    }

    /**
     * 根据月结账号查询令牌配置
     * @param partnerId 月结账号
     * @return 令牌配置
     */
    public DeliveryToken findByPartnerId(String partnerId) {
        if (StringUtils.isBlank(partnerId)) {
            return null;
        }

        List<DeliveryToken> tokens = deliveryTokenRepository.list(1, 100);
        return tokens.stream()
                .filter(token -> partnerId.equals(token.getPartnerId()))
                .findFirst()
                .orElse(null);
    }
}
