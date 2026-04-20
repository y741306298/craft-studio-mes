package com.mes.application.command.manufacturerMeta;

import com.mes.application.command.api.ProductCoreApiService;
import com.mes.application.dto.resp.manufacturerMeta.ManufacturerSimpleResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.device.repository.DeviceInfoRepository;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerMeta;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerProductionLineMeta;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerWorkshopMeta;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerDeviceCfgRepository;
import com.mes.domain.manufacturer.manufacturerMeta.repository.ManufacturerMetaRepository;
import com.mes.domain.manufacturer.manufacturerMeta.service.ManufacturerMetaService;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.entity.ManufacturerMtsProductCfg;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.service.ManufacturerMtsProductCfgService;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.entity.ManufacturerProcessPriceCfg;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.service.ManufacturerProcessPriceCfgService;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageSlot;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageTank;
import com.mes.domain.manufacturer.transBox.storageTank.repository.StorageTankRepository;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppManufacturerMetaService {

    @Autowired
    private ManufacturerMetaService domainManufacturerMetaService;

    @Autowired
    private ManufacturerMetaRepository manufacturerMetaRepository;
    
    @Autowired
    private DeviceInfoRepository deviceInfoRepository;

    @Autowired
    private StorageTankRepository storageTankRepository;

    @Autowired
    private MockExternalApiService mockExternalApiService;

    @Autowired
    private ManufacturerMtsProductCfgService manufacturerMtsProductCfgService;

    @Autowired
    private ManufacturerProcessPriceCfgService manufacturerProcessPriceCfgService;
    
    @Autowired
    private AppManufacturerDeviceCfgService appDeviceCfgService;

    @Autowired
    private ManufacturerDeviceCfgRepository manufacturerDeviceCfgRepository;

    @Autowired
    private ProductCoreApiService productCoreApiService;

    public PagedResult<ManufacturerMeta> findManufacturerMetas(String name, String manufacturerType, PagedQuery query){
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        List<ManufacturerMeta> items;
        long total;

        if (StringUtils.isBlank(name) && StringUtils.isBlank(manufacturerType)) {
            items = manufacturerMetaRepository.list(query.getCurrent(), query.getSize());
            total = manufacturerMetaRepository.total();
        } else {
            items = domainManufacturerMetaService.findManufacturerMetasByConditions(name, manufacturerType, (int) query.getCurrent(), query.getSize());
            total = domainManufacturerMetaService.getTotalCount(name, manufacturerType);
        }

        return new PagedResult<ManufacturerMeta>(items, total, query.getSize(), query.getCurrent());
    }

    public void addManufacturerMeta(ManufacturerMeta command){
        if (command == null) {
            throw new IllegalArgumentException("制造商元数据不能为空");
        }
        if (command.getAddress() == null || command.getConsignee() == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "地址和收货人不能为空");
        }

        String terminalRegionCode = command.getAddress().getTerminalRegionCode();
        String detailAddress = command.getAddress().getDetailAddress();
        String consigneeName = command.getConsignee().getName();
        String consigneePhone = command.getConsignee().getPhone();
        if (StringUtils.isBlank(terminalRegionCode) || StringUtils.isBlank(detailAddress) ||
                StringUtils.isBlank(consigneeName) || StringUtils.isBlank(consigneePhone)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "地址格式不对");
        }
        ManufacturerMeta savedManufacturer = domainManufacturerMetaService.addManufacturerMeta(command);
        
        registerToProductCenter(savedManufacturer);
        
        createDefaultStorageTank(savedManufacturer);
        
        saveManufacturerConfigsFromTemplate(savedManufacturer);
    }
    
    private void registerToProductCenter(ManufacturerMeta manufacturerMeta) {
        if (manufacturerMeta.getAddress() == null || manufacturerMeta.getConsignee() == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "地址和收货人不能为空");
        }

        String terminalRegionCode = manufacturerMeta.getAddress().getTerminalRegionCode();
        String detailAddress = manufacturerMeta.getAddress().getDetailAddress();
        String consigneeName = manufacturerMeta.getConsignee().getName();
        String consigneePhone = manufacturerMeta.getConsignee().getPhone();
        if (StringUtils.isBlank(terminalRegionCode) || StringUtils.isBlank(detailAddress) ||
                StringUtils.isBlank(consigneeName) || StringUtils.isBlank(consigneePhone)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "地址和收货人不能为空");
        }

        
        String rmfId = productCoreApiService.registerManufacturer(
                manufacturerMeta.getName(),
                terminalRegionCode,
                detailAddress,
                consigneeName,
                consigneePhone
        );

        manufacturerMeta.setManufacturerMetaId(rmfId);
        manufacturerMetaRepository.update(manufacturerMeta);
    }
    
    private void saveManufacturerConfigsFromTemplate(ManufacturerMeta manufacturerMeta) {
        String manufacturerTempId = manufacturerMeta.getManufacturerTempId();
        if (StringUtils.isBlank(manufacturerTempId)) {
            return;
        }
        
        List<ManufacturerMtsProductCfg> productCfgs = mockExternalApiService.getManufacturerMtsProductsByTempId(manufacturerTempId);
        for (ManufacturerMtsProductCfg productCfg : productCfgs) {
            productCfg.setManufacturerId(manufacturerMeta.getId());
        }
        manufacturerMtsProductCfgService.batchSave(productCfgs);
        
        List<ManufacturerProcessPriceCfg> processPriceCfgs = mockExternalApiService.getManufacturerProcessPricesByTempId(manufacturerTempId);
        for (ManufacturerProcessPriceCfg processPriceCfg : processPriceCfgs) {
            processPriceCfg.setManufacturerId(manufacturerMeta.getId());
        }
        manufacturerProcessPriceCfgService.batchSave(processPriceCfgs);
    }
    
    private void createDefaultStorageTank(ManufacturerMeta manufacturerMeta) {
        String manufacturerId = manufacturerMeta.getId();
        if (StringUtils.isBlank(manufacturerId)) {
            return;
        }
        
        StorageTank storageTank = new StorageTank();
        storageTank.setManufacturerId(manufacturerId);
        storageTank.setStorageTankName("默认储存柜");
        storageTank.setStorageTankCode("STORAGE_DEFAULT_" + System.currentTimeMillis());
        storageTank.setStorageTankType("DEFAULT");
        storageTank.setLocation("默认位置");
        storageTank.setStatus("ACTIVE");
        storageTank.setMovable(false);
        
        List<StorageSlot> storageSlots = new ArrayList<>();
        int slotIndex = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                StorageSlot slot = new StorageSlot();
                slot.setSlotId(UUID.randomUUID().toString());
                slot.setSlotCode("SLOT_" + row + "_" + col);
                slot.setStorageTankId(storageTank.getStorageTankId());
                slot.setSlotOrder(slotIndex++);
                slot.setStatus("AVAILABLE");
                storageSlots.add(slot);
            }
        }
        
        storageTank.setStorageSlots(storageSlots);
        storageTank.setTotalSlots(9);
        storageTank.setUsedSlots(0);
        storageTank.setRemainingSlots(9);
        storageTank.setMaxCapacity(1000.0);
        storageTank.setCurrentCapacity(0.0);
        storageTank.setCapacityUnit("kg");
        storageTank.setDescription("系统自动创建的默认 3*3 储存柜");
        
        storageTankRepository.add(storageTank);
    }
    
    public void updateManufacturerMeta(ManufacturerMeta command){
        if (command == null) {
            throw new IllegalArgumentException("制造商元数据不能为空");
        }
        if (StringUtils.isBlank(command.getManufacturerMetaId())) {
            throw new IllegalArgumentException("制造商 ID 不能为空");
        }
        domainManufacturerMetaService.updateManufacturerMeta(command);
    }
    
    public void deleteManufacturerMeta(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        
        ManufacturerMeta manufacturerMeta = findById(id);
        if (manufacturerMeta == null) {
            throw new IllegalArgumentException("制造商不存在：" + id);
        }
        
        String manufacturerMetaId = manufacturerMeta.getManufacturerMetaId();
        if (StringUtils.isNotBlank(manufacturerMetaId)) {
            PagedQuery query = new PagedQuery(1, 10000);
            PagedResult<ManufacturerDeviceCfg> deviceResult = appDeviceCfgService.findDeviceCfgsByManufacturerId(manufacturerMetaId, query);
            
            for (ManufacturerDeviceCfg deviceCfg : deviceResult.items()) {
                manufacturerDeviceCfgRepository.delete(deviceCfg);
            }
        }
        
        domainManufacturerMetaService.deleteManufacturerMeta(id);
    }
    
    public ManufacturerMeta findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        return domainManufacturerMetaService.findById(id);
    }

    /**
     * 分页查询制造商元数据（包含设备数量）
     * @param name 制造商名称
     * @param manufacturerType 制造商类型
     * @param query 分页参数
     * @return 分页结果，包含设备数量信息
     */
    public PagedResult<ManufacturerMetaWithDeviceCount> findManufacturerMetasWithDeviceCount(String name, String manufacturerType, PagedQuery query){
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        List<ManufacturerMeta> items;
        long total;

        if (StringUtils.isBlank(name) && StringUtils.isBlank(manufacturerType)) {
            items = manufacturerMetaRepository.list(query.getCurrent(), query.getSize());
            total = manufacturerMetaRepository.total();
        } else {
            items = domainManufacturerMetaService.findManufacturerMetasByConditions(name, manufacturerType, (int) query.getCurrent(), query.getSize());
            total = domainManufacturerMetaService.getTotalCount(name, manufacturerType);
        }

        // 为每个制造商查询设备数量
        List<ManufacturerMetaWithDeviceCount> itemsWithDeviceCount = new ArrayList<>();
        for (ManufacturerMeta meta : items) {
            ManufacturerMetaWithDeviceCount metaWithDeviceCount = new ManufacturerMetaWithDeviceCount(meta);

            // 查询设备数量
            if (meta.getManufacturerMetaId() != null) {
                PagedQuery deviceQuery = new PagedQuery(1, 1);
                PagedResult<ManufacturerDeviceCfg> deviceResult = appDeviceCfgService.findDeviceCfgsByManufacturerId(meta.getManufacturerMetaId(), deviceQuery);
                metaWithDeviceCount.setDeviceCount((int) deviceResult.total());
            } else {
                metaWithDeviceCount.setDeviceCount(0);
            }

            itemsWithDeviceCount.add(metaWithDeviceCount);
        }

        return new PagedResult<>(itemsWithDeviceCount, total, query.getSize(), query.getCurrent());
    }

    /**
     * 根据 manufacturerMetaId 查找制造商
     * @param manufacturerMetaId 制造商业务 ID
     * @return 制造商实体
     */
    public ManufacturerMeta findByManufacturerMetaId(String manufacturerMetaId) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new IllegalArgumentException("制造商 ID 不能为空");
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("manufacturerMetaId", manufacturerMetaId);
        List<ManufacturerMeta> results = manufacturerMetaRepository.filterList(1, 1, filters);

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 根据制造商类型全量查询工厂列表（不分页）
     * @param manufacturerType 制造商类型
     * @return 简化工厂信息列表
     */
    public List<ManufacturerSimpleResponse> findByManufacturerType(String manufacturerType) {
        if (StringUtils.isBlank(manufacturerType)) {
            throw new IllegalArgumentException("制造商类型不能为空");
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("manufacturerMetaType", manufacturerType);
        
        List<ManufacturerMeta> items = manufacturerMetaRepository.filterList(1, 10000, filters);
        
        return items.stream()
                .map(meta -> {
                    ManufacturerSimpleResponse response = new ManufacturerSimpleResponse();
                    response.setManufacturerType(manufacturerType);
                    response.setId(meta.getId());
                    response.setManufacturerMetaId(meta.getManufacturerMetaId());
                    response.setName(meta.getName());
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * 为工厂添加车间信息
     * @param manufacturerMetaId 制造商 ID
     * @param newWorkshopMetas 新车间实体列表
     */
    public void addWorkshopsForManufacturer(String manufacturerMetaId, List<ManufacturerWorkshopMeta> newWorkshopMetas) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new IllegalArgumentException("制造商 ID 不能为空");
        }
        
        if (newWorkshopMetas == null || newWorkshopMetas.isEmpty()) {
            throw new IllegalArgumentException("车间列表不能为空");
        }
        
        // 先查询现有制造商信息
        ManufacturerMeta existingMeta = findByManufacturerMetaId(manufacturerMetaId);
        if (existingMeta == null) {
            throw new IllegalArgumentException("制造商不存在：" + manufacturerMetaId);
        }
        
        // 为每个新车间生成 ID 和设置状态
        for (ManufacturerWorkshopMeta workshopMeta : newWorkshopMetas) {
            // 生成车间 ID
            String workshopId = IdGenerator.generateId("WORKSHOP");
            workshopMeta.setWorkshopId(workshopId);
            
            // 设置车间状态为 NORMAL
            workshopMeta.setStatus(CfgStatus.NORMAL.getCode());
            
            // 为每个车间的生产线生成 ID 和设置状态
            if (workshopMeta.getManufacturerProductionLineMetas() != null) {
                for (ManufacturerProductionLineMeta productionLine : workshopMeta.getManufacturerProductionLineMetas()) {
                    // 生成生产线 ID
                    String productionLineId = IdGenerator.generateId("LINE");
                    productionLine.setProductionLineId(productionLineId);
                    
                    // 设置生产线状态为 NORMAL
                    productionLine.setStatus(CfgStatus.NORMAL.getCode());
                }
            }
        }
        
        // 合并现有车间和新车间
        List<ManufacturerWorkshopMeta> allWorkshopMetas = new ArrayList<>();
        if (existingMeta.getManufacturerWorkshopMetas() != null) {
            allWorkshopMetas.addAll(existingMeta.getManufacturerWorkshopMetas());
        }
        allWorkshopMetas.addAll(newWorkshopMetas);
        
        // 更新制造商的车间信息
        existingMeta.setManufacturerWorkshopMetas(allWorkshopMetas);
        updateManufacturerMeta(existingMeta);
    }
    
    /**
     * 制造商元数据包装类，包含设备数量
     */
    public static class ManufacturerMetaWithDeviceCount {
        private ManufacturerMeta manufacturerMeta;
        private int deviceCount;

        public ManufacturerMetaWithDeviceCount(ManufacturerMeta manufacturerMeta) {
            this.manufacturerMeta = manufacturerMeta;
            this.deviceCount = 0;
        }
        
        public ManufacturerMeta getManufacturerMeta() {
            return manufacturerMeta;
        }
        
        public int getDeviceCount() {
            return deviceCount;
        }

        public void setDeviceCount(int deviceCount) {
            this.deviceCount = deviceCount;
        }
    }
}
