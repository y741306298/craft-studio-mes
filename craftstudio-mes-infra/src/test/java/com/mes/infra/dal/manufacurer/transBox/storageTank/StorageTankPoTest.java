package com.mes.infra.dal.manufacurer.transBox.storageTank;

import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageTank;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageSlot;
import com.mes.infra.base.BasePO;
import com.mes.infra.dal.manufacurer.transBox.storageTank.po.StorageTankPo;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StorageTankPoTest {
    @Test
    void roundTripPreservesTenantCapacityEnvironmentAndNestedSlotInventory() {
        StorageTank tank = new StorageTank();
        tank.setManufacturerId("M1");
        tank.setStorageTankId("T1");
        tank.setMaxCapacity(100.0);
        tank.setCurrentCapacity(10.0);
        tank.setCapacityUnit("kg");
        tank.setTemperatureRange("10~20℃");
        tank.setHumidityRange("40~60%RH");
        StorageSlot slot = new StorageSlot();
        slot.setSlotId("S1");
        slot.setStorageTankId("T1");
        slot.setStatus("OCCUPIED");
        slot.setProductionPieceId("P1");
        slot.setQuantity(2);
        slot.setWeight(10.0);
        slot.setTemperature("15℃");
        tank.setStorageSlots(List.of(slot));
        StorageTank restored = BasePO.fromDO(tank, StorageTankPo.class).toDO();
        assertEquals(tank, restored);
    }
}
