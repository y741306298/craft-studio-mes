package com.mes.application.command.transBox;

import com.mes.application.dto.req.storageTank.StorageTankRequest;
import com.mes.application.dto.req.storageTank.StorageTankListRequest;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageTank;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageSlot;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageTankRepository;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageSlotRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppStorageTankServiceTest {
    StorageTankRepository tanks = mock(StorageTankRepository.class);
    StorageSlotRepository slots = mock(StorageSlotRepository.class);
    AppStorageTankService service = new AppStorageTankService(tanks, slots);

    @BeforeEach
    void setup() {
        when(slots.filterList(anyLong(), anyInt(), anyMap())).thenReturn(List.of());
        when(slots.add(any())).thenAnswer(invocation -> {
            StorageSlot slot = invocation.getArgument(0);
            slot.setId("db-" + slot.getSlotId());
            slot.setCreateTime(new Date());
            return slot;
        });
        when(tanks.add(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void addGeneratesRequestedNumberOfDefaultSlotsWithoutCapacityOrEnvironment() {
        StorageTankRequest request = request();
        request.setTotalSlots(3);
        StorageTank saved = service.add("M1", request);
        assertEquals("M1", saved.getManufacturerId());
        assertTrue(saved.getStorageTankId().startsWith("TANK"));
        assertEquals(3, saved.getTotalSlots());
        assertEquals(0, saved.getUsedSlots());
        assertEquals(3, saved.getRemainingSlots());
        assertNull(saved.getMaxCapacity());
        assertNull(saved.getCurrentCapacity());
        assertNull(saved.getCapacityUnit());
        assertNull(saved.getTemperatureRange());
        assertNull(saved.getHumidityRange());
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            StorageSlot slot = saved.getStorageSlots().get(i);
            assertTrue(ids.add(slot.getSlotId()));
            assertEquals(saved.getStorageTankId(), slot.getStorageTankId());
            assertNotNull(slot.getId());
            assertEquals("SLOT_" + (i + 1), slot.getSlotCode());
            assertEquals(i + 1, slot.getSlotOrder());
            assertEquals("AVAILABLE", slot.getStatus());
            assertEquals(0, slot.getQuantity());
            assertNull(slot.getProductionPieceId());
            assertNull(slot.getWeight());
            assertNull(slot.getVolume());
            assertNull(slot.getTemperature());
            assertNull(slot.getHumidity());
        }
        verify(slots, times(3)).add(any());
    }

    @Test
    void omittedAndZeroCountCreateAnEmptyTank() {
        for (Integer count : Arrays.asList(null, 0)) {
            StorageTankRequest request = request();
            request.setTotalSlots(count);
            StorageTank saved = service.add("M1", request);
            assertTrue(saved.getStorageSlots().isEmpty());
            assertEquals(0, saved.getTotalSlots());
            assertEquals(0, saved.getUsedSlots());
            assertEquals(0, saved.getRemainingSlots());
        }
        verify(slots, never()).add(any());
    }

    @Test
    void editAddsDefaultSlotsWithoutChangingExistingInventoryOrLegacyCapacity() {
        StorageTank tank = existing();
        StorageSlot occupied = slot("S1", "db1", true);
        occupied.setSlotCode("SLOT_1");
        occupied.setSlotOrder(1);
        occupied.setProductionPieceId("P1");
        occupied.setQuantity(3);
        tank.setStorageSlots(List.of(occupied));
        tank.setMaxCapacity(100.0);
        tank.setTemperatureRange("10~20℃");
        when(slots.filterList(anyLong(), anyInt(), anyMap())).thenReturn(List.of(occupied));
        StorageTankRequest request = request();
        request.setId("T1");
        request.setTotalSlots(3);
        StorageTank edited = service.edit("M1", request);
        assertEquals("business-T1", edited.getStorageTankId());
        assertEquals(new Date(123), edited.getCreateTime());
        assertSame(occupied, edited.getStorageSlots().getFirst());
        assertEquals("P1", occupied.getProductionPieceId());
        assertEquals(3, occupied.getQuantity());
        assertEquals("OCCUPIED", occupied.getStatus());
        assertEquals(3, edited.getTotalSlots());
        assertEquals(1, edited.getUsedSlots());
        assertEquals(2, edited.getRemainingSlots());
        assertEquals(100.0, edited.getMaxCapacity());
        assertEquals("10~20℃", edited.getTemperatureRange());
        verify(slots).update(occupied);
        verify(slots, times(2)).add(any());
        verify(tanks).update(tank);
    }

    @Test
    void editCountRemovesEmptySlotsFromEndAndKeepsOccupiedSlots() {
        StorageTank tank = existing();
        StorageSlot empty = slot("S1", "db1", false);
        StorageSlot occupied = slot("S2", "db2", true);
        tank.setStorageSlots(List.of(empty, occupied));
        when(slots.filterList(anyLong(), anyInt(), anyMap())).thenReturn(List.of(empty, occupied));
        StorageTankRequest request = request();
        request.setId("T1");
        request.setTotalSlots(1);
        StorageTank edited = service.edit("M1", request);
        assertEquals(List.of(occupied), edited.getStorageSlots());
        verify(slots).delete(empty);
        verify(slots, never()).delete(occupied);
    }

    @Test
    void omittedCountPreservesExistingSlotsAndZeroRemovesEmptySlots() {
        StorageTank tank = existing();
        tank.setStorageSlots(List.of(slot("S1", "db1", false)));
        StorageTankRequest request = request();
        request.setId("T1");
        assertEquals(1, service.edit("M1", request).getTotalSlots());
        request.setTotalSlots(0);
        assertEquals(0, service.edit("M1", request).getTotalSlots());
    }

    @Test
    void refusesReductionBelowOccupiedSlotCountBeforeWriting() {
        existing().setStorageSlots(List.of(slot("S1", "db1", true)));
        StorageTankRequest request = request();
        request.setId("T1");
        request.setTotalSlots(0);
        assertThrows(BusinessNotAllowException.class, () -> service.edit("M1", request));
        verify(slots, never()).delete(any());
        verify(tanks, never()).update(any());
    }

    @Test
    void negativeCountIsRejectedBeforeWriting() {
        StorageTankRequest request = request();
        request.setTotalSlots(-1);
        assertThrows(BusinessNotAllowException.class, () -> service.add("M1", request));
        existing();
        request.setId("T1");
        assertThrows(BusinessNotAllowException.class, () -> service.edit("M1", request));
        verify(slots, never()).add(any());
        verify(tanks, never()).add(any());
        verify(tanks, never()).update(any());
    }

    @Test
    void tenantCannotReadEditOrDeleteOtherManufacturersTank() {
        existing();
        assertThrows(BusinessNotAllowException.class, () -> service.get("M2", "T1"));
        StorageTankRequest request = request();
        request.setId("T1");
        assertThrows(BusinessNotAllowException.class, () -> service.edit("M2", request));
        assertThrows(BusinessNotAllowException.class, () -> service.delete("M2", "T1"));
        verify(tanks, never()).delete(any());
    }

    @Test
    void listUsesExactTenantAndSameFiltersForCountAndItems() {
        StorageTankListRequest request = new StorageTankListRequest();
        request.setStorageTankName("柜[1]");
        service.list("M1", request);
        Map<String, Object> expected = Map.of("manufacturerId", "M1", "storageTankName_like", "柜[1]");
        verify(tanks).filterList(1, 20, expected);
        verify(tanks).filterTotal(expected);
        request.setCurrent(0);
        assertThrows(BusinessNotAllowException.class, () -> service.list("M1", request));
    }

    @Test
    void deleteSoftDeletesStandaloneSlotsAndTank() {
        StorageTank tank = existing();
        StorageSlot empty = slot("S1", "db1", false);
        when(slots.filterList(anyLong(), anyInt(), anyMap())).thenReturn(List.of(empty));
        service.delete("M1", "T1");
        verify(slots).delete(empty);
        verify(tanks).delete(tank);
    }

    @Test
    void refusesDeletionOrOverwriteWhenStandaloneInventoryDisagrees() {
        StorageTank tank = existing();
        StorageSlot occupied = slot("S1", "db1", true);
        when(slots.filterList(anyLong(), anyInt(), anyMap())).thenReturn(List.of(occupied));
        assertThrows(BusinessNotAllowException.class, () -> service.delete("M1", "T1"));
        StorageTankRequest request = request();
        request.setId("T1");
        request.setTotalSlots(0);
        assertThrows(BusinessNotAllowException.class, () -> service.edit("M1", request));
        verify(tanks, never()).delete(tank);
        verify(slots, never()).delete(any());
    }

    private StorageTank existing() {
        StorageTank tank = new StorageTank();
        tank.setId("T1");
        tank.setStorageTankId("business-T1");
        tank.setManufacturerId("M1");
        tank.setStatus("ACTIVE");
        tank.setCreateTime(new Date(123));
        when(tanks.findById("T1")).thenReturn(tank);
        return tank;
    }

    private StorageTankRequest request() {
        StorageTankRequest request = new StorageTankRequest();
        request.setStorageTankName("储存柜");
        request.setStorageTankCode("C1");
        return request;
    }

    private StorageSlot slot(String id, String dbId, boolean occupied) {
        StorageSlot slot = new StorageSlot();
        slot.setSlotId(id);
        slot.setId(dbId);
        slot.setStatus(occupied ? "OCCUPIED" : "AVAILABLE");
        return slot;
    }
}
