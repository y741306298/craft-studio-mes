package com.mes.domain.delivery.deliveryPkg.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.mes.domain.delivery.deliveryPkg.enums.DeliveryPkgStatus;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryPkgRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 包裹服务类
 */
@Service
public class DeliveryPkgService {

    @Autowired
    private DeliveryPkgRepository deliveryPkgRepository;

    /**
     * 创建包裹
     */
    public DeliveryPkg createDeliveryPkg(DeliveryPkg deliveryPkg) {
        if (deliveryPkg == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "包裹信息不能为空");
        }

        if (StringUtils.isBlank(deliveryPkg.getRecipientName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "收件人姓名不能为空");
        }

        if (StringUtils.isBlank(deliveryPkg.getRecipientPhone())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "收件人电话不能为空");
        }

        if (StringUtils.isBlank(deliveryPkg.getRecipientAddress())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "收件地址不能为空");
        }

        if (deliveryPkg.getDeliveryPkgItems() == null || deliveryPkg.getDeliveryPkgItems().isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "包裹物品不能为空");
        }

        deliveryPkg.setDeliveryPkgStatus(DeliveryPkgStatus.PENDING_PACKING);
        deliveryPkg.setCreateTime(new Date());
        deliveryPkg.setUpdateTime(new Date());

        return deliveryPkgRepository.add(deliveryPkg);
    }

    /**
     * 根据 ID 查询包裹
     */
    public DeliveryPkg findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "包裹 ID 不能为空");
        }
        return deliveryPkgRepository.findById(id);
    }

    /**
     * 根据包裹编码查询
     */
    public List<DeliveryPkg> findByDeliveryPkgCode(String deliveryPkgCode) {
        if (StringUtils.isBlank(deliveryPkgCode)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "包裹编码不能为空");
        }
        return deliveryPkgRepository.findByDeliveryPkgCode(deliveryPkgCode);
    }

    /**
     * 根据运单号查询
     */
    public List<DeliveryPkg> findByTrackingNumber(String trackingNumber) {
        if (StringUtils.isBlank(trackingNumber)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "运单号不能为空");
        }
        return deliveryPkgRepository.findByTrackingNumber(trackingNumber);
    }

    /**
     * 更新包裹信息
     */
    public DeliveryPkg updateDeliveryPkg(DeliveryPkg deliveryPkg) {
        if (deliveryPkg == null || StringUtils.isBlank(deliveryPkg.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "包裹信息或 ID 不能为空");
        }

        DeliveryPkg existingPkg = findById(deliveryPkg.getId());
        if (existingPkg == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "包裹不存在：" + deliveryPkg.getId());
        }

        deliveryPkg.setUpdateTime(new Date());
        deliveryPkgRepository.update(deliveryPkg);
        return deliveryPkg;
    }

    /**
     * 删除包裹
     */
    public void deleteDeliveryPkg(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "包裹 ID 不能为空");
        }

        DeliveryPkg deliveryPkg = findById(id);
        if (deliveryPkg == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "包裹不存在：" + id);
        }

        if (deliveryPkg.getDeliveryPkgStatus() == DeliveryPkgStatus.DELIVERED) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "已发货的包裹不能删除");
        }

        deliveryPkgRepository.delete(deliveryPkg);
    }

    /**
     * 开始打包
     */
    public void startPacking(String id) {
        DeliveryPkg deliveryPkg = findById(id);
        if (deliveryPkg == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "包裹不存在：" + id);
        }

        if (!deliveryPkg.getDeliveryPkgStatus().canStartPacking()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前状态不能开始打包，当前状态：" +
                    deliveryPkg.getDeliveryPkgStatus().getDescription());
        }

        deliveryPkg.setDeliveryPkgStatus(DeliveryPkgStatus.PACKING);
        deliveryPkg.setPackingStartTime(new Date());
        deliveryPkg.setUpdateTime(new Date());
        deliveryPkgRepository.update(deliveryPkg);
    }

    /**
     * 完成打包
     */
    public void completePacking(String id) {
        DeliveryPkg deliveryPkg = findById(id);
        if (deliveryPkg == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "包裹不存在：" + id);
        }

        if (!deliveryPkg.getDeliveryPkgStatus().canCompletePacking()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前状态不能完成打包，当前状态：" +
                    deliveryPkg.getDeliveryPkgStatus().getDescription());
        }

        deliveryPkg.setDeliveryPkgStatus(DeliveryPkgStatus.PENDING_DELIVERY);
        deliveryPkg.setPackingEndTime(new Date());
        deliveryPkg.setUpdateTime(new Date());
        deliveryPkgRepository.update(deliveryPkg);
    }

    /**
     * 确认发货
     */
    public void confirmDelivery(String id, String trackingNumber) {
        DeliveryPkg deliveryPkg = findById(id);
        if (deliveryPkg == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "包裹不存在：" + id);
        }

        if (!deliveryPkg.getDeliveryPkgStatus().canDeliver()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "当前状态不能发货，当前状态：" +
                    deliveryPkg.getDeliveryPkgStatus().getDescription());
        }

        if (StringUtils.isBlank(trackingNumber)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "运单号不能为空");
        }

        deliveryPkg.setDeliveryPkgStatus(DeliveryPkgStatus.DELIVERED);
        deliveryPkg.setTrackingNumber(trackingNumber);
        deliveryPkg.setDeliveryTime(new Date());
        deliveryPkg.setUpdateTime(new Date());
        deliveryPkgRepository.update(deliveryPkg);
    }

    /**
     * 查询待打包的包裹
     */
    public List<DeliveryPkg> findPendingPacking() {
        return deliveryPkgRepository.findPendingPacking();
    }

    /**
     * 查询已发货的包裹
     */
    public List<DeliveryPkg> findDelivered() {
        return deliveryPkgRepository.findDelivered();
    }

    /**
     * 根据多条件查询包裹
     */
    public List<DeliveryPkg> queryByConditions(
            DeliveryPkgStatus status,
            String recipientName,
            String trackingNumber,
            long current,
            int size) {

        Map<String, Object> filters = new java.util.HashMap<>();

        if (status != null) {
            filters.put("deliveryPkgStatus", status);
        }

        if (StringUtils.isNotBlank(recipientName)) {
            filters.put("recipientName", recipientName);
        }

        if (StringUtils.isNotBlank(trackingNumber)) {
            filters.put("trackingNumber", trackingNumber);
        }

        return deliveryPkgRepository.filterList(current, size, filters);
    }

    /**
     * 统计包裹数量
     */
    public long countByConditions(DeliveryPkgStatus status) {
        if (status == null) {
            return deliveryPkgRepository.total();
        }

        Map<String, Object> filters = new java.util.HashMap<>();
        filters.put("deliveryPkgStatus", status);
        return deliveryPkgRepository.filterTotal(filters);
    }
}
