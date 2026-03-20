package com.mes.domain.delivery.deliveryPkg.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.delivery.deliveryPkg.enums.DeliveryPkgStatus;
import com.mes.domain.delivery.deliveryPkg.vo.DeliveryPkgItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 物流包裹实体
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeliveryPkg extends BaseEntity {

    private String deliveryPkgId;                 // 包裹 ID
    private String deliveryPkgCode;               // 包裹编码
    private List<DeliveryPkgItem> deliveryPkgItems; // 包裹物品列表
    private DeliveryPkgStatus deliveryPkgStatus;  // 包裹状态
    
    private String recipientName;                 // 收件人姓名
    private String recipientPhone;                // 收件人电话
    private String recipientAddress;              // 收件地址
    private String province;                      // 省
    private String city;                          // 市
    private String district;                      // 区
    
    private String senderName;                    // 寄件人姓名
    private String senderPhone;                   // 寄件人电话
    private String senderAddress;                 // 寄件地址
    
    private Double weight;                        // 包裹重量（kg）
    private Double volume;                        // 包裹体积（m³）
    
    private String deliveryWay;                   // 配送方式
    private String trackingNumber;                // 运单号
    
    private Date packingStartTime;                // 打包开始时间
    private Date packingEndTime;                  // 打包完成时间
    private Date deliveryTime;                    // 发货时间
    
    private String remarks;                       // 备注信息

    /**
     * 添加包裹物品
     */
    public void addItem(DeliveryPkgItem item) {
        if (this.deliveryPkgItems == null) {
            this.deliveryPkgItems = new java.util.ArrayList<>();
        }
        this.deliveryPkgItems.add(item);
    }

    /**
     * 计算总数量
     */
    public int getTotalQuantity() {
        if (this.deliveryPkgItems == null || this.deliveryPkgItems.isEmpty()) {
            return 0;
        }
        return this.deliveryPkgItems.stream()
                .mapToInt(DeliveryPkgItem::getQuantity)
                .sum();
    }

    /**
     * 判断是否可以打包
     */
    public boolean canPack() {
        return this.deliveryPkgStatus != null && this.deliveryPkgStatus.canPack();
    }

    /**
     * 判断是否可以发货
     */
    public boolean canDeliver() {
        return this.deliveryPkgStatus != null && this.deliveryPkgStatus.canDeliver();
    }
}
