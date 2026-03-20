package com.mes.application.command.productionPiece;

import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageOperationRecord;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageSlot;
import com.mes.domain.manufacturer.transBox.storageTank.entity.StorageTank;
import com.mes.domain.manufacturer.transBox.storageTank.service.StorageOperationRecordService;
import com.mes.domain.manufacturer.transBox.storageTank.service.StorageTankService;
import com.mes.domain.shared.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AppTransBoxService {

    @Autowired
    private ProductionPieceService productionPieceService;

    @Autowired
    private StorageTankService storageTankService;

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
            throw new BusinessNotAllowException("生产单元 ID 不能为空");
        }
        if (StringUtils.isBlank(storageTankId)) {
            throw new BusinessNotAllowException("储存柜 ID 不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessNotAllowException("入库数量必须为正整数");
        }
        if (StringUtils.isBlank(operatorId)) {
            throw new BusinessNotAllowException("操作人 ID 不能为空");
        }
        
        ProductionPiece productionPiece = productionPieceService.findById(cellId);
        if (productionPiece == null) {
            throw new BusinessNotAllowException("生产工件不存在");
        }
        
        if (productionPiece.getQuantity() == null || productionPiece.getQuantity() < quantity) {
            throw new BusinessNotAllowException("生产工件数量不足");
        }
        
        StorageTank storageTank = storageTankService.findById(storageTankId);
        if (storageTank == null) {
            throw new BusinessNotAllowException("储存柜不存在");
        }
        
        StorageSlot availableSlot = storageTank.findAvailableSlot();
        if (availableSlot == null) {
            throw new BusinessNotAllowException("储存柜没有可用储位");
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
            throw new BusinessNotAllowException("生产工件 ID 不能为空");
        }
        if (StringUtils.isBlank(storageTankId)) {
            throw new BusinessNotAllowException("储存柜 ID 不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessNotAllowException("入库数量必须为正整数");
        }
        if (StringUtils.isBlank(operatorId)) {
            throw new BusinessNotAllowException("操作人 ID 不能为空");
        }
        
        ProductionPiece productionPiece = productionPieceService.findById(pieceId);
        if (productionPiece == null) {
            throw new BusinessNotAllowException("生产工件不存在");
        }
        
        if (productionPiece.getQuantity() == null || productionPiece.getQuantity() < quantity) {
            throw new BusinessNotAllowException("生产工件数量不足");
        }
        
        StorageTank storageTank = storageTankService.findById(storageTankId);
        if (storageTank == null) {
            throw new BusinessNotAllowException("储存柜不存在");
        }
        
        StorageSlot availableSlot = storageTank.findAvailableSlot();
        if (availableSlot == null) {
            throw new BusinessNotAllowException("储存柜没有可用储位");
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
            throw new BusinessNotAllowException("储存柜 ID 不能为空");
        }
        if (StringUtils.isBlank(slotId)) {
            throw new BusinessNotAllowException("储位 ID 不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessNotAllowException("出库数量必须为正整数");
        }
        if (StringUtils.isBlank(operatorId)) {
            throw new BusinessNotAllowException("操作人 ID 不能为空");
        }
        
        StorageTank storageTank = storageTankService.findById(storageTankId);
        if (storageTank == null) {
            throw new BusinessNotAllowException("储存柜不存在");
        }
        
        StorageSlot slot = storageTank.getStorageSlots().stream()
            .filter(s -> slotId.equals(s.getSlotId()))
            .findFirst()
            .orElseThrow(() -> new BusinessNotAllowException("储位不存在"));
        
        if (!slot.isOccupied()) {
            throw new BusinessNotAllowException("储位未被占用，无法出库");
        }
        
        if (slot.getQuantity() < quantity) {
            throw new BusinessNotAllowException("储位物品数量不足");
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
}
