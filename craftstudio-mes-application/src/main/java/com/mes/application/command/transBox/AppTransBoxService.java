package com.mes.application.command.transBox;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.application.command.transBox.vo.StorageInResult;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageOperationRecord;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageSlot;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageTank;
import com.mes.domain.manufacturer.transBox.storageTank.service.StorageOperationRecordService;
import com.mes.domain.manufacturer.transBox.storageTank.service.StorageSlotService;
import com.mes.domain.manufacturer.transBox.storageTank.service.StorageTankService;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class AppTransBoxService {

    @Autowired
    private StorageTankService storageTankService;

    @Autowired
    private ProductionPieceService productionPieceService;

    @Autowired
    private StorageSlotService storageSlotService;

    @Autowired
    private StorageOperationRecordService storageOperationRecordService;

    /**
     * 将指定的 ProductionCell 按输入数量放入指定 ID 的 StorageTank 中
     *
     * @param cellId 生产单元 ID
     * @param storageTankId 储存柜 ID
     * @param quantity 入库数量
     * @param operatorId 操作人 ID
     * @param operatorName 操作人姓名
     * @return 创建的操作记录
     * @throws BusinessNotAllowException 当参数不合法或操作失败时抛出此异常
     */
    public StorageOperationRecord storeProductionCellToTank(
            String cellId,
            String storageTankId,
            Integer quantity,
            String operatorId,
            String operatorName) {

        if (StringUtils.isBlank(cellId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产单元 ID 不能为空");
        }
        if (StringUtils.isBlank(storageTankId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜 ID 不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "入库数量必须为正整数");
        }
        if (StringUtils.isBlank(operatorId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "操作人 ID 不能为空");
        }

        ProductionPiece productionPiece = productionPieceService.findById(cellId);
        if (productionPiece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不存在");
        }

        if (productionPiece.getQuantity() == null || productionPiece.getQuantity() < quantity) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件数量不足");
        }

        StorageTank storageTank = storageTankService.findById(storageTankId);
        if (storageTank == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜不存在");
        }

        StorageSlot availableSlot = storageTank.findAvailableSlot();
        if (availableSlot == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜没有可用储位");
        }

        availableSlot.storeItem(
                productionPiece.getProductionPieceId(),
                "PRODUCTION_CELL",
                quantity
        );

        if (storageTank.getUsedSlots() == null) {
            storageTank.setUsedSlots(0);
        }
        storageTank.setUsedSlots(storageTank.getUsedSlots() + 1);

        if (storageTank.getRemainingSlots() == null) {
            storageTank.setRemainingSlots(0);
        }
        storageTank.setRemainingSlots(storageTank.getRemainingSlots() - 1);

        if (storageTank.getCurrentCapacity() == null) {
            storageTank.setCurrentCapacity(0.0);
        }
        storageTank.setCurrentCapacity(storageTank.getCurrentCapacity() + quantity);

        storageTankService.updateStorageTank(storageTank);

        productionPiece.setStatus("IN_STORAGE");
        productionPieceService.updateProductionPiece(productionPiece);

        StorageOperationRecord record = new StorageOperationRecord();
        record.setStorageTankId(storageTank.getStorageTankId());
        record.setStorageTankName(storageTank.getStorageTankName());
        record.setSlotId(availableSlot.getSlotId());
        record.setOperationType(StorageOperationRecord.OPERATION_TYPE_IN);
        record.setProductionPieceId(productionPiece.getProductionPieceId());
        record.setProductionPieceType("PRODUCTION_CELL");
        record.setQuantity(quantity);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setStatus("SUCCESS");
        record.setRemarks("生产单元入库：" + cellId);

        return storageOperationRecordService.recordOperation(record);
    }

    /**
     * 将指定的 ProductionPiece 按输入数量放入指定 ID 的 StorageTank 中
     *
     * @param pieceId 生产工件 ID
     * @param storageTankId 储存柜 ID
     * @param quantity 入库数量
     * @param operatorId 操作人 ID
     * @param operatorName 操作人姓名
     * @return 创建的操作记录
     * @throws BusinessNotAllowException 当参数不合法或操作失败时抛出此异常
     */
    public StorageOperationRecord storeProductionPieceToTank(
            String pieceId,
            String storageTankId,
            Integer quantity,
            String operatorId,
            String operatorName) {

        if (StringUtils.isBlank(pieceId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件 ID 不能为空");
        }
        if (StringUtils.isBlank(storageTankId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜 ID 不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "入库数量必须为正整数");
        }
        if (StringUtils.isBlank(operatorId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "操作人 ID 不能为空");
        }

        ProductionPiece productionPiece = productionPieceService.findById(pieceId);
        if (productionPiece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不存在");
        }

        if (productionPiece.getQuantity() == null || productionPiece.getQuantity() < quantity) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件数量不足");
        }

        StorageTank storageTank = storageTankService.findById(storageTankId);
        if (storageTank == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜不存在");
        }

        StorageSlot availableSlot = storageTank.findAvailableSlot();
        if (availableSlot == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜没有可用储位");
        }

        availableSlot.storeItem(
                productionPiece.getProductionPieceId(),
                productionPiece.getProductionPieceType(),
                quantity
        );

        if (storageTank.getUsedSlots() == null) {
            storageTank.setUsedSlots(0);
        }
        storageTank.setUsedSlots(storageTank.getUsedSlots() + 1);

        if (storageTank.getRemainingSlots() == null) {
            storageTank.setRemainingSlots(0);
        }
        storageTank.setRemainingSlots(storageTank.getRemainingSlots() - 1);

        if (storageTank.getCurrentCapacity() == null) {
            storageTank.setCurrentCapacity(0.0);
        }
        storageTank.setCurrentCapacity(storageTank.getCurrentCapacity() + quantity);

        storageTankService.updateStorageTank(storageTank);

        productionPiece.setStatus("IN_STORAGE");
        productionPieceService.updateProductionPiece(productionPiece);

        StorageOperationRecord record = new StorageOperationRecord();
        record.setStorageTankId(storageTank.getStorageTankId());
        record.setStorageTankName(storageTank.getStorageTankName());
        record.setSlotId(availableSlot.getSlotId());
        record.setOperationType(StorageOperationRecord.OPERATION_TYPE_IN);
        record.setProductionPieceId(productionPiece.getProductionPieceId());
        record.setProductionPieceType(productionPiece.getProductionPieceType());
        record.setQuantity(quantity);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setStatus("SUCCESS");
        record.setRemarks("生产工件入库");

        return storageOperationRecordService.recordOperation(record);
    }

    /**
     * 从 StorageTank 中取出指定数量的物品
     *
     * @param storageTankId 储存柜 ID
     * @param slotId 储位 ID
     * @param quantity 出库数量
     * @param operatorId 操作人 ID
     * @param operatorName 操作人姓名
     * @return 创建的操作记录
     * @throws BusinessNotAllowException 当参数不合法或操作失败时抛出此异常
     */
    public StorageOperationRecord retrieveFromTank(
            String storageTankId,
            String slotId,
            Integer quantity,
            String operatorId,
            String operatorName) {

        if (StringUtils.isBlank(storageTankId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜 ID 不能为空");
        }
        if (StringUtils.isBlank(slotId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储位 ID 不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "出库数量必须为正整数");
        }
        if (StringUtils.isBlank(operatorId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "操作人 ID 不能为空");
        }

        StorageTank storageTank = storageTankService.findById(storageTankId);
        if (storageTank == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜不存在");
        }

        StorageSlot slot = storageTank.getStorageSlots().stream()
                .filter(s -> slotId.equals(s.getSlotId()))
                .findFirst()
                .orElseThrow(() -> new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储位不存在"));

        if (!slot.isOccupied()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储位未被占用，无法出库");
        }

        if (slot.getQuantity() < quantity) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储位物品数量不足");
        }

        Integer remainingQuantity = slot.getQuantity() - quantity;
        if (remainingQuantity > 0) {
            slot.setQuantity(remainingQuantity);
        } else {
            slot.retrieveItem();

            if (storageTank.getUsedSlots() != null && storageTank.getUsedSlots() > 0) {
                storageTank.setUsedSlots(storageTank.getUsedSlots() - 1);
            }
            if (storageTank.getRemainingSlots() != null) {
                storageTank.setRemainingSlots(storageTank.getRemainingSlots() + 1);
            }
        }

        if (storageTank.getCurrentCapacity() != null && storageTank.getCurrentCapacity() >= quantity) {
            storageTank.setCurrentCapacity(storageTank.getCurrentCapacity() - quantity);
        }

        storageTankService.updateStorageTank(storageTank);

        StorageOperationRecord record = new StorageOperationRecord();
        record.setStorageTankId(storageTank.getStorageTankId());
        record.setStorageTankName(storageTank.getStorageTankName());
        record.setSlotId(slotId);
        record.setOperationType(StorageOperationRecord.OPERATION_TYPE_OUT);
        record.setProductionPieceId(slot.getProductionPieceId());
        record.setProductionPieceType(slot.getProductionPieceType());
        record.setQuantity(quantity);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setStatus("SUCCESS");
        record.setRemarks("生产工件出库");

        return storageOperationRecordService.recordOperation(record);
    }

    /**
     * 将生产零件放入空闲的储存柜格子中
     * 
     * @param productionPieceId 生产工件 ID
     * @param quantity 存放数量
     * @param manufacturerId 制造商 ID（用于筛选储存柜）
     * @return 入库结果
     */
    public StorageInResult storeProductionPiece(String productionPieceId, Integer quantity, String manufacturerId) {
        if (StringUtils.isBlank(productionPieceId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件 ID 不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "存放数量必须大于 0");
        }

        // 1. 获取生产工件信息
        ProductionPiece piece = productionPieceService.findById(productionPieceId);
        if (piece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不存在：" + productionPieceId);
        }

        // 2. 查找可用的储存柜（按制造商筛选）
        List<StorageTank> availableTanks = findAvailableStorageTanks(manufacturerId);
        if (availableTanks.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "没有可用的储存柜");
        }

        // 3. 遍历储存柜，找到合适的储位
        StorageTank selectedTank = null;
        StorageSlot selectedSlot = null;
        
        for (StorageTank tank : availableTanks) {
            if (!tank.hasAvailableSlot()) {
                continue;
            }

            StorageSlot slot = tank.findAvailableSlot();
            if (slot != null) {
                selectedTank = tank;
                selectedSlot = slot;
                break;
            }
        }

        if (selectedTank == null || selectedSlot == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "所有储存柜均无可用储位");
        }

        // 4. 分配储位并存储
        selectedSlot.setProductionPieceId(productionPieceId);
        selectedSlot.setProductionPieceType(piece.getProductionPieceType());
        selectedSlot.setQuantity(quantity);
        selectedSlot.setStatus("OCCUPIED");
        selectedSlot.setStorageTime(new Date());

        // 更新储存柜的使用情况
        updateStorageTankStatus(selectedTank);

        // 5. 记录操作日志
        StorageOperationRecord record = createInOperationRecord(selectedTank, selectedSlot, piece, quantity);
        storageOperationRecordService.recordOperation(record);

        // 6. 构建返回结果
        StorageInResult result = new StorageInResult();
        result.setSuccess(true);
        result.setMessage("入库成功");
        result.setStorageTankId(selectedTank.getStorageTankId());
        result.setStorageTankName(selectedTank.getStorageTankName());
        result.setSlotId(selectedSlot.getSlotId());
        result.setSlotCode(selectedSlot.getSlotCode());
        result.setProductionPieceId(productionPieceId);
        result.setQuantity(quantity);
        result.setRecordId(record.getRecordId());

        return result;
    }

    /**
     * 批量将生产零件放入空闲的储存柜格子中
     * 
     * @param productionPieceIds 生产工件 ID 列表
     * @param manufacturerId 制造商 ID
     * @return 入库结果列表
     */
    public List<StorageInResult> batchStoreProductionPieces(List<String> productionPieceIds, String manufacturerId) {
        List<StorageInResult> results = new ArrayList<>();

        for (String productionPieceId : productionPieceIds) {
            try {
                StorageInResult result = storeProductionPiece(productionPieceId, 1, manufacturerId);
                results.add(result);
            } catch (Exception e) {
                System.err.println("处理生产工件 " + productionPieceId + " 失败：" + e.getMessage());
            }
        }

        return results;
    }

    /**
     * 推荐储存柜格子：返回两类推荐结果（已有同零件的格子 + 新格子）
     * 
     * @param productionPieceId 生产工件 ID
     * @param manufacturerId 制造商 ID
     * @return 推荐的格子列表
     */
    public SlotRecommendResult recommendSlot(String productionPieceId, String manufacturerId) {
        if (StringUtils.isBlank(productionPieceId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件 ID 不能为空");
        }

        // 1. 获取生产工件信息
        ProductionPiece piece = productionPieceService.findById(productionPieceId);
        if (piece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不存在：" + productionPieceId);
        }

        // 2. 查找所有可用的储存柜
        List<StorageTank> availableTanks = findAvailableStorageTanks(manufacturerId);
        if (availableTanks.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "没有可用的储存柜");
        }

        List<RecommendedSlotInfo> recommendedSlots = new ArrayList<>();

        // 3. 查找已有同零件存放过的格子
        List<StorageSlotInfo> samePieceSlots = findAllSlotsWithSameProductionPiece(availableTanks, productionPieceId);
        for (StorageSlotInfo slotInfo : samePieceSlots) {
            RecommendedSlotInfo recInfo = new RecommendedSlotInfo();
            recInfo.setSlotType("SAME_PRODUCTION_PIECE");
            recInfo.setDescription("存放过同种零件的格子");
            recInfo.setStorageTankId(slotInfo.getTankId());
            recInfo.setStorageTankName(slotInfo.getTankName());
            recInfo.setSlotId(slotInfo.getSlotId());
            recInfo.setSlotCode(slotInfo.getSlotCode());
            recInfo.setExistingQuantity(slotInfo.getQuantity());
            recInfo.setPriority(1);
            recommendedSlots.add(recInfo);
        }

        // 4. 查找同类型零件存放过的格子
        List<StorageSlotInfo> sameTypeSlots = findAllSlotsWithSameProductionPieceType(availableTanks, piece.getProductionPieceType());
        for (StorageSlotInfo slotInfo : sameTypeSlots) {
            RecommendedSlotInfo recInfo = new RecommendedSlotInfo();
            recInfo.setSlotType("SAME_PRODUCTION_PIECE_TYPE");
            recInfo.setDescription("存放过同类型零件的格子");
            recInfo.setStorageTankId(slotInfo.getTankId());
            recInfo.setStorageTankName(slotInfo.getTankName());
            recInfo.setSlotId(slotInfo.getSlotId());
            recInfo.setSlotCode(slotInfo.getSlotCode());
            recInfo.setExistingQuantity(slotInfo.getQuantity());
            recInfo.setPriority(2);
            recommendedSlots.add(recInfo);
        }

        // 5. 查找新的空格子
        List<StorageSlotInfo> newSlots = findAllAvailableSlots(availableTanks);
        for (StorageSlotInfo slotInfo : newSlots) {
            RecommendedSlotInfo recInfo = new RecommendedSlotInfo();
            recInfo.setSlotType("NEW_SLOT");
            recInfo.setDescription("新的空格子");
            recInfo.setStorageTankId(slotInfo.getTankId());
            recInfo.setStorageTankName(slotInfo.getTankName());
            recInfo.setSlotId(slotInfo.getSlotId());
            recInfo.setSlotCode(slotInfo.getSlotCode());
            recInfo.setExistingQuantity(0);
            recInfo.setPriority(3);
            recommendedSlots.add(recInfo);
        }

        // 6. 构建返回结果
        SlotRecommendResult result = new SlotRecommendResult();
        result.setRecommended(!recommendedSlots.isEmpty());
        result.setTotalRecommended(recommendedSlots.size());
        result.setRecommendedSlots(recommendedSlots);
        
        if (recommendedSlots.isEmpty()) {
            result.setMessage("没有可用的储存柜格子");
        } else {
            result.setMessage("共推荐 " + recommendedSlots.size() + " 个格子");
        }
        
        return result;
    }

    /**
     * 将生产零件放入指定的储存柜格子中
     * 
     * @param productionPieceId 生产工件 ID
     * @param quantity 存放数量
     * @param storageTankId 储存柜 ID
     * @param slotId 储位 ID
     * @return 入库结果
     */
    public StorageInResult storeProductionPiece(String productionPieceId, Integer quantity, 
                                                String storageTankId, String slotId) {
        if (StringUtils.isBlank(productionPieceId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件 ID 不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "存放数量必须大于 0");
        }
        if (StringUtils.isBlank(storageTankId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜 ID 不能为空");
        }
        if (StringUtils.isBlank(slotId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储位 ID 不能为空");
        }

        // 1. 获取生产工件信息
        ProductionPiece piece = productionPieceService.findById(productionPieceId);
        if (piece == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "生产工件不存在：" + productionPieceId);
        }

        // 2. 获取储存柜信息
        StorageTank selectedTank = storageTankService.findById(storageTankId);
        if (selectedTank == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储存柜不存在：" + storageTankId);
        }

        // 3. 获取储位信息
        StorageSlot selectedSlot = findSlotById(selectedTank, slotId);
        if (selectedSlot == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储位不存在：" + slotId);
        }

        // 4. 检查储位是否可用
        if ("OCCUPIED".equals(selectedSlot.getStatus())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "储位已被占用：" + slotId);
        }

        // 5. 分配储位并存储
        selectedSlot.setProductionPieceId(productionPieceId);
        selectedSlot.setProductionPieceType(piece.getProductionPieceType());
        selectedSlot.setQuantity(quantity);
        selectedSlot.setStatus("OCCUPIED");
        selectedSlot.setStorageTime(new Date());

        // 更新储存柜的使用情况
        updateStorageTankStatus(selectedTank);

        // 6. 记录操作日志
        StorageOperationRecord record = createInOperationRecord(selectedTank, selectedSlot, piece, quantity);
        storageOperationRecordService.recordOperation(record);

        // 7. 构建返回结果
        StorageInResult result = new StorageInResult();
        result.setSuccess(true);
        result.setMessage("入库成功");
        result.setStorageTankId(selectedTank.getStorageTankId());
        result.setStorageTankName(selectedTank.getStorageTankName());
        result.setSlotId(selectedSlot.getSlotId());
        result.setSlotCode(selectedSlot.getSlotCode());
        result.setProductionPieceId(productionPieceId);
        result.setQuantity(quantity);
        result.setRecordId(record.getRecordId());

        return result;
    }

    /**
     * 查找可用的储存柜
     * 
     * @param manufacturerId 制造商 ID
     * @return 可用储存柜列表
     */
    private List<StorageTank> findAvailableStorageTanks(String manufacturerId) {
        // TODO: 这里应该调用查询接口获取储存柜列表
        // 目前 StorageTankService 缺少按条件查询的方法，暂时返回空列表
        // 实际使用时需要添加查询方法
        
        List<StorageTank> allTanks = new ArrayList<>();
        // 假设通过某种方式获取所有储存柜
        // 需要根据 manufacturerId 过滤，并且状态为 ACTIVE
        
        return allTanks;
    }

    /**
     * 查找所有已有同零件存放过的格子
     * 
     * @param tanks 储存柜列表
     * @param productionPieceId 生产工件 ID
     * @return 匹配的储位信息列表
     */
    private List<StorageSlotInfo> findAllSlotsWithSameProductionPiece(List<StorageTank> tanks, String productionPieceId) {
        List<StorageSlotInfo> result = new ArrayList<>();
        
        for (StorageTank tank : tanks) {
            if (tank.getStorageSlots() == null) {
                continue;
            }
            
            for (StorageSlot slot : tank.getStorageSlots()) {
                if ("OCCUPIED".equals(slot.getStatus()) && 
                    productionPieceId.equals(slot.getProductionPieceId())) {
                    StorageSlotInfo info = new StorageSlotInfo();
                    info.setTankId(tank.getStorageTankId());
                    info.setTankName(tank.getStorageTankName());
                    info.setSlotId(slot.getSlotId());
                    info.setSlotCode(slot.getSlotCode());
                    info.setQuantity(slot.getQuantity());
                    result.add(info);
                }
            }
        }
        
        return result;
    }

    /**
     * 查找所有同类型零件存放过的格子
     * 
     * @param tanks 储存柜列表
     * @param productionPieceType 生产工件类型
     * @return 匹配的储位信息列表
     */
    private List<StorageSlotInfo> findAllSlotsWithSameProductionPieceType(List<StorageTank> tanks, String productionPieceType) {
        List<StorageSlotInfo> result = new ArrayList<>();
        
        for (StorageTank tank : tanks) {
            if (tank.getStorageSlots() == null) {
                continue;
            }
            
            for (StorageSlot slot : tank.getStorageSlots()) {
                if ("OCCUPIED".equals(slot.getStatus()) && 
                    productionPieceType.equals(slot.getProductionPieceType())) {
                    StorageSlotInfo info = new StorageSlotInfo();
                    info.setTankId(tank.getStorageTankId());
                    info.setTankName(tank.getStorageTankName());
                    info.setSlotId(slot.getSlotId());
                    info.setSlotCode(slot.getSlotCode());
                    info.setQuantity(slot.getQuantity());
                    result.add(info);
                }
            }
        }
        
        return result;
    }

    /**
     * 查找所有可用的空格子
     * 
     * @param tanks 储存柜列表
     * @return 可用储位信息列表
     */
    private List<StorageSlotInfo> findAllAvailableSlots(List<StorageTank> tanks) {
        List<StorageSlotInfo> result = new ArrayList<>();
        
        for (StorageTank tank : tanks) {
            if (tank.getStorageSlots() == null) {
                continue;
            }
            
            for (StorageSlot slot : tank.getStorageSlots()) {
                if ("AVAILABLE".equals(slot.getStatus())) {
                    StorageSlotInfo info = new StorageSlotInfo();
                    info.setTankId(tank.getStorageTankId());
                    info.setTankName(tank.getStorageTankName());
                    info.setSlotId(slot.getSlotId());
                    info.setSlotCode(slot.getSlotCode());
                    info.setQuantity(0);
                    result.add(info);
                }
            }
        }
        
        return result;
    }

    /**
     * 更新储存柜状态
     * 
     * @param tank 储存柜
     */
    private void updateStorageTankStatus(StorageTank tank) {
        int usedCount = 0;
        int remainingCount = 0;

        if (tank.getStorageSlots() != null) {
            for (StorageSlot slot : tank.getStorageSlots()) {
                if ("OCCUPIED".equals(slot.getStatus())) {
                    usedCount++;
                } else if ("AVAILABLE".equals(slot.getStatus())) {
                    remainingCount++;
                }
            }
        }

        tank.setUsedSlots(usedCount);
        tank.setRemainingSlots(remainingCount);
        
        storageTankService.updateStorageTank(tank);
    }

    /**
     * 创建入库操作记录
     * 
     * @param tank 储存柜
     * @param slot 储位
     * @param piece 生产工件
     * @param quantity 数量
     * @return 操作记录
     */
    private StorageOperationRecord createInOperationRecord(StorageTank tank, StorageSlot slot, 
                                                           ProductionPiece piece, Integer quantity) {
        StorageOperationRecord record = new StorageOperationRecord();
        record.setStorageTankId(tank.getStorageTankId());
        record.setStorageTankName(tank.getStorageTankName());
        record.setSlotId(slot.getSlotId());
        record.setOperationType(StorageOperationRecord.OPERATION_TYPE_IN);
        record.setProductionPieceId(piece.getId());
        record.setProductionPieceType(piece.getProductionPieceType());
        record.setQuantity(quantity);
        record.setStatus("SUCCESS");
        record.setRemarks("生产工件入库");
        record.setOperationTime(new Date());
        
        return record;
    }

    /**
     * 根据 ID 查找储位
     * 
     * @param tank 储存柜
     * @param slotId 储位 ID
     * @return 储位
     */
    private StorageSlot findSlotById(StorageTank tank, String slotId) {
        if (tank.getStorageSlots() == null) {
            return null;
        }
        
        for (StorageSlot slot : tank.getStorageSlots()) {
            if (slotId.equals(slot.getSlotId())) {
                return slot;
            }
        }
        
        return null;
    }

    /**
     * 储位信息类（内部使用）
     */
    private static class StorageSlotInfo {
        private String tankId;
        private String tankName;
        private String slotId;
        private String slotCode;
        private Integer quantity;

        public String getTankId() {
            return tankId;
        }

        public void setTankId(String tankId) {
            this.tankId = tankId;
        }

        public String getTankName() {
            return tankName;
        }

        public void setTankName(String tankName) {
            this.tankName = tankName;
        }

        public String getSlotId() {
            return slotId;
        }

        public void setSlotId(String slotId) {
            this.slotId = slotId;
        }

        public String getSlotCode() {
            return slotCode;
        }

        public void setSlotCode(String slotCode) {
            this.slotCode = slotCode;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    /**
     * 推荐结果类
     */
    public static class SlotRecommendResult {
        private boolean recommended;
        private String message;
        private Integer totalRecommended;
        private List<RecommendedSlotInfo> recommendedSlots;

        public boolean isRecommended() {
            return recommended;
        }

        public void setRecommended(boolean recommended) {
            this.recommended = recommended;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getTotalRecommended() {
            return totalRecommended;
        }

        public void setTotalRecommended(Integer totalRecommended) {
            this.totalRecommended = totalRecommended;
        }

        public List<RecommendedSlotInfo> getRecommendedSlots() {
            return recommendedSlots;
        }

        public void setRecommendedSlots(List<RecommendedSlotInfo> recommendedSlots) {
            this.recommendedSlots = recommendedSlots;
        }
    }

    /**
     * 推荐的格子信息类
     */
    public static class RecommendedSlotInfo {
        private String slotType;
        private String description;
        private String storageTankId;
        private String storageTankName;
        private String slotId;
        private String slotCode;
        private Integer existingQuantity;
        private Integer priority;

        public String getSlotType() {
            return slotType;
        }

        public void setSlotType(String slotType) {
            this.slotType = slotType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStorageTankId() {
            return storageTankId;
        }

        public void setStorageTankId(String storageTankId) {
            this.storageTankId = storageTankId;
        }

        public String getStorageTankName() {
            return storageTankName;
        }

        public void setStorageTankName(String storageTankName) {
            this.storageTankName = storageTankName;
        }

        public String getSlotId() {
            return slotId;
        }

        public void setSlotId(String slotId) {
            this.slotId = slotId;
        }

        public String getSlotCode() {
            return slotCode;
        }

        public void setSlotCode(String slotCode) {
            this.slotCode = slotCode;
        }

        public Integer getExistingQuantity() {
            return existingQuantity;
        }

        public void setExistingQuantity(Integer existingQuantity) {
            this.existingQuantity = existingQuantity;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }
    }
}
