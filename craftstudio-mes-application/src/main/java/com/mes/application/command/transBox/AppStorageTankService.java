package com.mes.application.command.transBox;

import com.mes.application.dto.req.storageTank.StorageTankRequest;
import com.mes.application.dto.req.storageTank.StorageTankListRequest;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageTank;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageSlot;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageTankRepository;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageSlotRepository;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AppStorageTankService {
    private final StorageTankRepository tankRepository;
    private final StorageSlotRepository slotRepository;

    public PagedApiResponse<StorageTank> list(String manufacturerId, StorageTankListRequest request) {
        requireText(manufacturerId, "制造商 ID 不能为空");
        if (request.getCurrent() < 1 || request.getSize() < 1 || request.getSize() > 100) {
            throw invalid("页码必须大于零，每页大小必须在 1-100 之间");
        }
        Map<String, Object> filters = new HashMap<>();
        filters.put("manufacturerId", manufacturerId);
        if (hasText(request.getStorageTankName())) filters.put("storageTankName_like", request.getStorageTankName());
        if (hasText(request.getStorageTankCode())) filters.put("storageTankCode", request.getStorageTankCode());
        if (hasText(request.getStatus())) filters.put("status", request.getStatus());
        return PagedApiResponse.success(tankRepository.filterList(request.getCurrent(), request.getSize(), filters),
                request.getCurrent(), request.getSize(), tankRepository.filterTotal(filters));
    }

    public StorageTank get(String manufacturerId, String id) {
        requireText(manufacturerId, "制造商 ID 不能为空");
        requireText(id, "ID 不能为空");
        StorageTank tank = tankRepository.findById(id);
        if (tank == null || !manufacturerId.equals(tank.getManufacturerId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.notFound, "储存柜不存在");
        }
        requireText(tank.getStorageTankId(), "储存柜缺少业务 ID，请先修复历史数据");
        return tank;
    }

    @Transactional
    public StorageTank add(String manufacturerId, StorageTankRequest request) {
        requireText(manufacturerId, "制造商 ID 不能为空");
        if (hasText(request.getId())) throw invalid("新增时不能指定 ID");
        StorageTank tank = new StorageTank();
        tank.setStorageTankId(IdGenerator.generateId("TANK"));
        tank.setManufacturerId(manufacturerId);
        apply(tank, request, true);
        syncSlots(tank, List.of());
        return tankRepository.add(tank);
    }

    @Transactional
    public StorageTank edit(String manufacturerId, StorageTankRequest request) {
        StorageTank tank = get(manufacturerId, request.getId());
        List<StorageSlot> oldSlots = standaloneSlots(tank.getStorageTankId());
        apply(tank, request, false);
        syncSlots(tank, oldSlots);
        tankRepository.update(tank);
        return tank;
    }

    @Transactional
    public void delete(String manufacturerId, String id) {
        StorageTank tank = get(manufacturerId, id);
        List<StorageSlot> standalone = standaloneSlots(tank.getStorageTankId());
        if (slots(tank).stream().anyMatch(this::containsItems)
                || standalone.stream().anyMatch(this::containsItems)
                || (tank.getCurrentCapacity() != null && tank.getCurrentCapacity() > 0)) {
            throw invalid("储存柜仍有库存，不能删除");
        }
        standalone.forEach(slotRepository::delete);
        tankRepository.delete(tank);
    }

    private void apply(StorageTank tank, StorageTankRequest request, boolean creating) {
        requireText(request.getStorageTankName(), "储存柜名称不能为空");
        requireText(request.getStorageTankCode(), "储存柜编码不能为空");
        String status = request.getStatus() == null ? (creating ? "ACTIVE" : tank.getStatus()) : request.getStatus();
        if (!Set.of("ACTIVE", "INACTIVE", "MAINTENANCE").contains(status == null ? "" : status)) {
            throw invalid("储存柜状态必须为 ACTIVE、INACTIVE 或 MAINTENANCE");
        }
        if (request.getTotalSlots() != null && request.getTotalSlots() < 0) {
            throw invalid("储位总数量不能为负数");
        }
        tank.setStorageTankName(request.getStorageTankName());
        tank.setStorageTankCode(request.getStorageTankCode());
        tank.setStorageTankType(request.getStorageTankType());
        tank.setLocation(request.getLocation());
        tank.setStatus(status);
        tank.setMovable(request.getMovable() != null ? request.getMovable() : Boolean.TRUE.equals(tank.getMovable()));
        tank.setDescription(request.getDescription());
        tank.setManagerId(request.getManagerId());
        tank.setManagerName(request.getManagerName());
        if (request.getTotalSlots() != null) resizeSlots(tank, request.getTotalSlots());
        if (tank.getStorageSlots() == null) tank.setStorageSlots(new ArrayList<>());
        tank.setTotalSlots(tank.getStorageSlots().size());
        tank.setUsedSlots(tank.getUsedSlotCount());
        tank.setRemainingSlots(tank.getRemainingSlotCount());
    }

    /** 总数是目标数量：增加时追加默认空位，减少时从末尾移除空位，保留占用储位。 */
    private void resizeSlots(StorageTank tank, int total) {
        List<StorageSlot> result = new ArrayList<>(slots(tank));
        if (result.stream().filter(this::containsItems).count() > total) {
            throw invalid("储位总数量不能小于已有库存的储位数量");
        }
        for (int index = result.size() - 1; result.size() > total && index >= 0; index--) {
            if (!containsItems(result.get(index))) result.remove(index);
        }
        Set<String> codes = new HashSet<>();
        long nextOrder = 1;
        for (StorageSlot slot : result) {
            codes.add(slot.getSlotCode());
            if (slot.getSlotOrder() != null) nextOrder = Math.max(nextOrder, (long) slot.getSlotOrder() + 1);
        }
        while (result.size() < total) {
            if (nextOrder > Integer.MAX_VALUE) throw invalid("储位顺序号已达到上限");
            String code = "SLOT_" + nextOrder;
            if (codes.add(code)) {
                StorageSlot slot = new StorageSlot();
                slot.setSlotId(IdGenerator.generateId("SLOT"));
                slot.setStorageTankId(tank.getStorageTankId());
                slot.setSlotCode(code);
                slot.setSlotOrder((int) nextOrder);
                slot.setStatus("AVAILABLE");
                slot.setQuantity(0);
                result.add(slot);
            }
            nextOrder++;
        }
        tank.setStorageSlots(result);
    }

    /** 柜内储位为本管理接口的聚合数据源，同时同步独立集合供原储位服务查询。 */
    private void syncSlots(StorageTank tank, List<StorageSlot> oldSlots) {
        Map<String, StorageSlot> oldById = new HashMap<>();
        oldSlots.forEach(slot -> oldById.put(slot.getSlotId(), slot));
        Set<String> retained = new HashSet<>();
        for (StorageSlot slot : slots(tank)) retained.add(slot.getSlotId());
        // 先验证再写入，避免独立集合中有库存的储位被静默删除或覆盖。
        for (StorageSlot old : oldSlots) {
            StorageSlot updated = slots(tank).stream().filter(s -> Objects.equals(s.getSlotId(), old.getSlotId())).findFirst().orElse(null);
            boolean inventoryDiffers = updated != null && (containsItems(old) || containsItems(updated))
                    && (!Objects.equals(old.getStatus(), updated.getStatus())
                    || !Objects.equals(old.getProductionPieceId(), updated.getProductionPieceId())
                    || !Objects.equals(old.getProductionPieceType(), updated.getProductionPieceType())
                    || !Objects.equals(old.getQuantity(), updated.getQuantity()));
            if ((updated == null && containsItems(old)) || inventoryDiffers) {
                throw invalid("储位库存数据不一致，请先核对出入库数据");
            }
        }
        for (StorageSlot slot : slots(tank)) {
            StorageSlot old = oldById.get(slot.getSlotId());
            if (old == null) {
                slot.setId(null);
                StorageSlot saved = slotRepository.add(slot);
                slot.setId(saved.getId());
                slot.setCreateTime(saved.getCreateTime());
                slot.setUpdateTime(saved.getUpdateTime());
            } else {
                slot.setId(old.getId());
                slot.setCreateTime(old.getCreateTime());
                slotRepository.update(slot);
            }
        }
        oldSlots.stream().filter(slot -> !retained.contains(slot.getSlotId())).forEach(slotRepository::delete);
    }

    private List<StorageSlot> standaloneSlots(String tankId) {
        List<StorageSlot> result = new ArrayList<>();
        for (int page = 1; ; page++) {
            List<StorageSlot> batch = slotRepository.filterList(page, 100, Map.of("storageTankId", tankId));
            result.addAll(batch);
            if (batch.size() < 100) return result;
        }
    }

    private List<StorageSlot> slots(StorageTank tank) {
        return tank.getStorageSlots() == null ? List.of() : tank.getStorageSlots();
    }

    private boolean containsItems(StorageSlot slot) {
        return slot.isOccupied() || hasText(slot.getProductionPieceId()) || (slot.getQuantity() != null && slot.getQuantity() > 0);
    }

    private void requireText(String value, String message) {
        if (!hasText(value)) throw invalid(message);
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private BusinessNotAllowException invalid(String message) {
        return new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, message);
    }
}
