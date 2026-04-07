package com.mes.application.command.delivery;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;
import com.mes.domain.delivery.deliveryPkg.service.DeliveryTokenService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 电子面单令牌应用服务
 */
@Service
public class AppDeliveryTokenService {

    @Autowired
    private DeliveryTokenService domainDeliveryTokenService;

    /**
     * 分页查询电子面单令牌配置
     * @param partnerId 月结账号（可选）
     * @param query 分页查询参数
     * @return 分页结果
     */
    public PagedResult<DeliveryToken> findDeliveryTokens(String partnerId, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        List<DeliveryToken> items;
        long total;

        if (StringUtils.isNotBlank(partnerId)) {
            // 根据月结账号查询
            DeliveryToken token = domainDeliveryTokenService.findByPartnerId(partnerId);
            items = token != null ? List.of(token) : List.of();
            total = items.size();
        } else {
            items = domainDeliveryTokenService.list((int) query.getCurrent(), query.getSize());
            total = domainDeliveryTokenService.total();
        }

        return new PagedResult<>(items, total, query.getSize(), query.getCurrent());
    }

    /**
     * 添加电子面单令牌配置
     * @param token 令牌配置
     * @return 保存后的令牌配置
     */
    public DeliveryToken addDeliveryToken(DeliveryToken token) {
        if (token == null) {
            throw new IllegalArgumentException("令牌配置不能为空");
        }
        return domainDeliveryTokenService.addDeliveryToken(token);
    }

    /**
     * 更新电子面单令牌配置
     * @param token 令牌配置
     */
    public void updateDeliveryToken(DeliveryToken token) {
        if (token == null) {
            throw new IllegalArgumentException("令牌配置不能为空");
        }
        if (StringUtils.isBlank(token.getId())) {
            throw new IllegalArgumentException("令牌ID不能为空");
        }
        domainDeliveryTokenService.updateDeliveryToken(token);
    }

    /**
     * 删除电子面单令牌配置
     * @param id 令牌ID
     */
    public void deleteDeliveryToken(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID不能为空");
        }
        domainDeliveryTokenService.deleteDeliveryToken(id);
    }

    /**
     * 根据ID查询电子面单令牌配置
     * @param id 令牌ID
     * @return 令牌配置
     */
    public DeliveryToken findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID不能为空");
        }
        return domainDeliveryTokenService.findById(id);
    }
}
