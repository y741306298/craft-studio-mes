package com.mes.domain.delivery.deliveryPkg.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryPkg.entity.DeliverySiid;
import com.mes.domain.delivery.deliveryPkg.repository.DeliverySiidRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class DeliverySiidService {

    @Autowired
    private DeliverySiidRepository deliverySiidRepository;

    /**
     * 添加SIID
     */
    public DeliverySiid addDeliverySiid(DeliverySiid deliverySiid) {
        if (deliverySiid == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "SIID信息不能为空");
        }
        if (StringUtils.isBlank(deliverySiid.getSiid())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "SIID不能为空");
        }
        if (StringUtils.isBlank(deliverySiid.getName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "名称不能为空");
        }
        if (StringUtils.isBlank(deliverySiid.getUserId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "用户ID不能为空");
        }

        DeliverySiid existing = deliverySiidRepository.findBySiid(deliverySiid.getSiid());
        if (existing != null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "SIID已存在");
        }
        ensureSingleDefault(deliverySiid);

        return deliverySiidRepository.add(deliverySiid);
    }

    /**
     * 更新SIID
     */
    public void updateDeliverySiid(DeliverySiid deliverySiid) {
        if (deliverySiid == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "SIID信息不能为空");
        }
        if (StringUtils.isBlank(deliverySiid.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }
        ensureSingleDefault(deliverySiid);

        deliverySiidRepository.update(deliverySiid);
    }

    /**
     * 删除SIID
     */
    public void deleteDeliverySiid(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }

        DeliverySiid deliverySiid = deliverySiidRepository.findById(id);
        if (deliverySiid != null) {
            deliverySiidRepository.delete(deliverySiid);
        }
    }

    /**
     * 根据ID查询
     */
    public DeliverySiid findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }
        return deliverySiidRepository.findById(id);
    }

    /**
     * 根据SIID查询
     */
    public DeliverySiid findBySiid(String siid) {
        if (StringUtils.isBlank(siid)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "SIID不能为空");
        }
        return deliverySiidRepository.findBySiid(siid);
    }

    /**
     * 根据用户ID查询列表
     */
    public List<DeliverySiid> findByUserId(String userId) {
        if (StringUtils.isBlank(userId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "用户ID不能为空");
        }
        return deliverySiidRepository.findByUserId(userId);
    }

    /**
     * 分页查询
     */
    public List<DeliverySiid> list(int current, int size) {
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在1-100之间");
        }
        return deliverySiidRepository.list(current, size);
    }

    /**
     * 总数
     */
    public long total() {
        return deliverySiidRepository.total();
    }

    private void ensureSingleDefault(DeliverySiid deliverySiid) {
        if (!Boolean.TRUE.equals(deliverySiid.getIsDefault())) {
            return;
        }
        if (StringUtils.isBlank(deliverySiid.getManufacturerMetaId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "默认SIID必须指定工厂ID");
        }
        List<DeliverySiid> sameFactoryList = deliverySiidRepository.findByManufacturerMetaId(deliverySiid.getManufacturerMetaId());
        for (DeliverySiid item : sameFactoryList) {
            if (Boolean.TRUE.equals(item.getIsDefault()) && !Objects.equals(item.getId(), deliverySiid.getId())) {
                item.setIsDefault(false);
                deliverySiidRepository.update(item);
            }
        }
    }
}
