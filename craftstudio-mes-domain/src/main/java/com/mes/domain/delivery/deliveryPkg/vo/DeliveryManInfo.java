package com.mes.domain.delivery.deliveryPkg.vo;

import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.geo.world.repository.WorldRepository;
import com.piliofpala.craftstudio.shared.domain.geo.world.vo.World;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Data
public class DeliveryManInfo {

    private String name;

    private String mobile;

    private String tel;

    private String printAddr;

    private String company;

    @Component
    public static class WorldContextHolder implements ApplicationContextAware {
        private static ApplicationContext applicationContext;

        @Override
        public void setApplicationContext(ApplicationContext context) throws BeansException {
            applicationContext = context;
        }

        public static World getWorld() {
            if (applicationContext == null) {
                return null;
            }
            WorldRepository worldRepository = applicationContext.getBean(WorldRepository.class);
            return worldRepository.loadWorld();
        }
    }

    public static DeliveryManInfo fromDeliveryMan(DeliveryMan deliveryMan){
        DeliveryManInfo deliveryManInfo = new DeliveryManInfo();
        deliveryManInfo.setName(deliveryMan.getName());
        deliveryManInfo.setMobile(deliveryMan.getMobile());
        deliveryManInfo.setTel(deliveryMan.getTel());
        deliveryManInfo.setPrintAddr(deliveryMan.getPrintAddr());
        deliveryManInfo.setCompany(deliveryMan.getManufacturerMetaId());
        return deliveryManInfo;
    }

    public static DeliveryManInfo fromOrderCustomer(OrderCustomer orderCustomer){
        DeliveryManInfo deliveryManInfo = new DeliveryManInfo();
        deliveryManInfo.setName(orderCustomer.getCustomerName());
        deliveryManInfo.setMobile(orderCustomer.getCustomerPhone());
        deliveryManInfo.setTel(orderCustomer.getCustomerPhone());
        
        Address address = orderCustomer.getAddress();
        if (address != null) {
            World world = WorldContextHolder.getWorld();
            deliveryManInfo.setPrintAddr(address.buildFullAddressString(world));
        } else {
            deliveryManInfo.setPrintAddr(null);
        }
        
        deliveryManInfo.setCompany(null);
        return deliveryManInfo;
    }

    public static DeliveryMan toDeliveryMan(DeliveryManInfo deliveryManInfo){
        DeliveryMan deliveryMan = new DeliveryMan();
        deliveryMan.setName(deliveryManInfo.getName());
        deliveryMan.setMobile(deliveryManInfo.getMobile());
        deliveryMan.setTel(deliveryManInfo.getTel());
        deliveryMan.setPrintAddr(deliveryManInfo.getPrintAddr());
        deliveryMan.setManufacturerMetaId(deliveryManInfo.getCompany());
        return deliveryMan;
    }

}
