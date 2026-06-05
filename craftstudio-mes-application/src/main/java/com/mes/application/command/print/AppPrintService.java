package com.mes.application.command.print;

import com.mes.application.command.print.vo.PendingPrintTypesettingVO;
import com.mes.application.command.print.vo.PrintReportResult;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.manufacturerMeta.entity.ManufacturerDeviceCfg;
import com.mes.domain.manufacturer.manufacturerMeta.service.ManufacturerDeviceCfgService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingPrintTaskService;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppPrintService {

    @Autowired
    private TypesettingService typesettingService;

    @Autowired
    private ProductionPieceService productionPieceService;

    @Autowired
    private TypesettingPrintTaskService typesettingPrintTaskService;

    @Autowired
    private ManufacturerDeviceCfgService manufacturerDeviceCfgService;

    public PagedResult<PendingPrintTypesettingVO> findPendingPrintTypesetting(String manufacturerMetaId, String deviceCfgId, Date startTime, Date endTime, String status, int current, int size) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "manufacturerMetaId 不能为空");
        }
        if (current < 1) {
            current = 1;
        }
        if (size < 1 || size > 100) {
            size = 20;
        }

        String deviceCode = null;
        if (StringUtils.isNotBlank(deviceCfgId)) {
            ManufacturerDeviceCfg deviceCfg = manufacturerDeviceCfgService.findById(deviceCfgId);
            if (deviceCfg != null) {
                deviceCode = deviceCfg.getDeviceCode();
            }
        }

        Set<String> allowedStatuses = new HashSet<>(Arrays.asList(
                TypesettingStatus.PRINTING.getCode(),
                TypesettingStatus.PRINTING_IN_PROGRESS.getCode(),
                TypesettingStatus.COMPLETED.getCode()
        ));
        if (StringUtils.isNotBlank(status) && !allowedStatuses.contains(status)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams,
                    "status 仅支持 printing / printing_in_progress / completed");
        }

        List<TypesettingInfo> mergedItems = new ArrayList<>();
        if (StringUtils.isBlank(status)) {
            List<TypesettingInfo> pendingPrintItems = typesettingService.findTypesettingByConditions(
                    manufacturerMetaId,
                    TypesettingStatus.PRINTING.getCode(),
                    null,
                    null,
                    startTime,
                    endTime,
                    deviceCode,
                    1,
                    Integer.MAX_VALUE
            );
            List<TypesettingInfo> printingInProgressItems = typesettingService.findTypesettingByConditions(
                    manufacturerMetaId,
                    TypesettingStatus.PRINTING_IN_PROGRESS.getCode(),
                    null,
                    null,
                    startTime,
                    endTime,
                    deviceCode,
                    1,
                    Integer.MAX_VALUE
            );
            mergedItems.addAll(pendingPrintItems == null ? Collections.emptyList() : pendingPrintItems);
            mergedItems.addAll(printingInProgressItems == null ? Collections.emptyList() : printingInProgressItems);
        } else {
            List<TypesettingInfo> statusItems = typesettingService.findTypesettingByConditions(
                    manufacturerMetaId,
                    status,
                    null,
                    null,
                    startTime,
                    endTime,
                    deviceCode,
                    1,
                    Integer.MAX_VALUE
            );
            mergedItems.addAll(statusItems == null ? Collections.emptyList() : statusItems);
        }
        Map<String, TypesettingInfo> uniqueMap = new LinkedHashMap<>();
        for (TypesettingInfo item : mergedItems) {
            if (item != null && StringUtils.isNotBlank(item.getId())) {
                uniqueMap.put(item.getId(), item);
            }
        }
        List<TypesettingInfo> uniqueItems = uniqueMap.values().stream()
                .sorted(Comparator
                        .comparing((TypesettingInfo item) -> Boolean.TRUE.equals(item.getIsUrgent()))
                        .reversed()
                        .thenComparing(TypesettingInfo::getCreateTime,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        long total = uniqueItems.size();
        int fromIndex = Math.max((current - 1) * size, 0);
        int toIndex = Math.min(fromIndex + size, uniqueItems.size());
        List<TypesettingInfo> items = fromIndex >= uniqueItems.size()
                ? Collections.emptyList()
                : uniqueItems.subList(fromIndex, toIndex);

        List<PendingPrintTypesettingVO> resultItems = new ArrayList<>();
        for (TypesettingInfo item : items) {
            resultItems.add(PendingPrintTypesettingVO.from(item));
        }

        return new PagedResult<>(resultItems, total, resultItems.size(), current);
    }

    public TypesettingPrintTask findPrintTaskById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "打印任务ID不能为空");
        }

        TypesettingPrintTask task = typesettingPrintTaskService.findById(id);
        if (task == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "打印任务不存在：" + id);
        }

        return task;
    }

    public PrintReportResult reportPrinting(TypesettingInfo request) {
        if (request == null || StringUtils.isBlank(request.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版信息 ID 不能为空");
        }

        TypesettingInfo dbInfo = typesettingService.findById(request.getId());
        if (dbInfo == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版信息不存在：" + request.getId());
        }

        if (StringUtils.isNotBlank(request.getRemark())) {
            dbInfo.setRemark(request.getRemark());
        }
        Integer reportQuantity = request.getQuantity();
        if (reportQuantity != null) {
            if (reportQuantity < 0) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "报备数量不能小于0");
            }
            Integer currentLeaveQuantity = dbInfo.getLeaveQuantity() == null ? 0 : dbInfo.getLeaveQuantity();
            dbInfo.setLeaveQuantity(Math.max(currentLeaveQuantity - reportQuantity, 0));
        }
        if (dbInfo.getLeaveQuantity() != null && dbInfo.getLeaveQuantity() == 0) {
            dbInfo.setStatus(TypesettingStatus.COMPLETED.getCode());
        }
        typesettingService.updateTypesetting(dbInfo);

        boolean canComplete = reportQuantity != null
                && reportQuantity >= 0
                && (dbInfo.getLeaveQuantity() == null || dbInfo.getLeaveQuantity() <= 0);

        int transferCount = 0;
        if (reportQuantity != null && reportQuantity > 0 && dbInfo.getTypesettingCells() != null) {
            Map<String, Integer> pieceQuantityMap = new LinkedHashMap<>();
            accumulateProductionPieceQuantities(dbInfo.getTypesettingCells(), reportQuantity, pieceQuantityMap, new HashSet<>());
            for (Map.Entry<String, Integer> entry : pieceQuantityMap.entrySet()) {
                String productionPieceId = entry.getKey();
                Integer transferQuantity = entry.getValue();
                if (StringUtils.isBlank(productionPieceId) || transferQuantity == null || transferQuantity <= 0) {
                    continue;
                }

                ProductionPiece piece = productionPieceService.findById(productionPieceId);
                if (piece == null || piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
                    continue;
                }

                String printingNodeId = findNodeIdByName(piece.getProcedureFlow().getNodes(), "打印中");
                String pendingPackingNodeId = findNodeIdByName(piece.getProcedureFlow().getNodes(), "待打包");
                if (StringUtils.isBlank(printingNodeId) || StringUtils.isBlank(pendingPackingNodeId)) {
                    continue;
                }

                productionPieceService.transferPieceQuantityBetweenNodes(
                        piece.getId(),
                        printingNodeId,
                        pendingPackingNodeId,
                        transferQuantity
                );
                transferCount++;
            }
        }

        return new PrintReportResult(canComplete, transferCount);
    }

    public void startTypesettingPrintById(String typesettingId) {
        if (StringUtils.isBlank(typesettingId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版信息 ID 不能为空");
        }

        TypesettingInfo dbInfo = typesettingService.findById(typesettingId);
        if (dbInfo == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版信息不存在：" + typesettingId);
        }
        if (!TypesettingStatus.PRINTING.getCode().equals(dbInfo.getStatus())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "只有待打印状态可以开始打印");
        }

        dbInfo.setStatus(TypesettingStatus.PRINTING_IN_PROGRESS.getCode());
        typesettingService.updateTypesetting(dbInfo);
    }

    public void releaseLayout(List<String> typesettingIds) {
        if (typesettingIds == null || typesettingIds.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版ID列表不能为空");
        }

        Map<String, Integer> productionPieceRollbackQuantity = new LinkedHashMap<>();
        for (String typesettingId : typesettingIds) {
            if (StringUtils.isBlank(typesettingId)) {
                continue;
            }
            TypesettingInfo info = typesettingService.findById(typesettingId);
            if (info == null) {
                continue;
            }
            List<TypesettingSourceCell> usedCells = info.getTypesettingCells();
            if (usedCells != null) {
                for (TypesettingSourceCell usedCell : usedCells) {
                    if (usedCell == null || !TypesettingSourceType.PART.getCode().equals(usedCell.getSourceType())) {
                        continue;
                    }
                    int usedQuantity = usedCell.getQuantity() == null || usedCell.getQuantity() <= 0 ? 1 : usedCell.getQuantity();
                    productionPieceRollbackQuantity.merge(usedCell.getSourceId(), usedQuantity, Integer::sum);
                }
            }
            typesettingService.deleteTypesetting(info.getId());
        }

        for (Map.Entry<String, Integer> entry : productionPieceRollbackQuantity.entrySet()) {
            ProductionPiece piece = productionPieceService.findById(entry.getKey());
            if (piece == null || StringUtils.isBlank(piece.getId())) {
                continue;
            }
            productionPieceService.transferPieceQuantityBetweenNodes(
                    piece.getId(),
                    "NODE_PRINTING",
                    "NODE_TYPESETTING",
                    entry.getValue()
            );
        }
    }

    public void redo(TypesettingInfo request) {
        reprint(request);
    }

    public void reprint(TypesettingInfo request) {
        if (request == null || StringUtils.isBlank(request.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版信息 ID 不能为空");
        }
        Integer redoQuantity = request.getQuantity();
        if (redoQuantity == null || redoQuantity <= 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "重打数量必须大于0");
        }

        TypesettingInfo dbInfo = typesettingService.findById(request.getId());
        if (dbInfo == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版信息不存在：" + request.getId());
        }
        int currentLeave = dbInfo.getLeaveQuantity() == null ? 0 : dbInfo.getLeaveQuantity();
        dbInfo.setLeaveQuantity(currentLeave + redoQuantity);
        if (currentLeave == 0 && TypesettingStatus.COMPLETED.getCode().equals(dbInfo.getStatus())) {
            dbInfo.setStatus(TypesettingStatus.PRINTING.getCode());
        }
        typesettingService.updateTypesetting(dbInfo);
    }

    private String findNodeIdByName(List<ProcedureFlowNode> nodes, String nodeName) {
        for (ProcedureFlowNode node : nodes) {
            if (node != null && nodeName.equals(node.getNodeName())) {
                return node.getNodeId();
            }
        }
        return null;
    }

    private void accumulateProductionPieceQuantities(
            List<com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell> cells,
            int parentMultiplier,
            Map<String, Integer> pieceQuantityMap,
            Set<String> visitedTypesettingKeys) {
        if (cells == null || cells.isEmpty()) {
            return;
        }

        for (com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell cell : cells) {
            if (cell == null || StringUtils.isBlank(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
                continue;
            }
            int cellQuantity = (cell.getQuantity() == null || cell.getQuantity() <= 0 ? 1 : cell.getQuantity());
            int totalQuantity = parentMultiplier * cellQuantity;

            if (TypesettingSourceType.PART.getCode().equals(cell.getSourceType())) {
                pieceQuantityMap.merge(cell.getSourceId(), totalQuantity, Integer::sum);
                continue;
            }

            if (!TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
                continue;
            }

            String visitedKey = cell.getSourceType() + ":" + cell.getSourceId();
            if (visitedTypesettingKeys.contains(visitedKey)) {
                continue;
            }
            visitedTypesettingKeys.add(visitedKey);

            List<TypesettingInfo> nestedInfos = typesettingService.findTypesettingListByTypesettingId(cell.getSourceId());
            if (nestedInfos == null || nestedInfos.isEmpty()) {
                TypesettingInfo nestedById = typesettingService.findById(cell.getSourceId());
                nestedInfos = nestedById == null ? Collections.emptyList() : new ArrayList<>(Collections.singletonList(nestedById));
            }

            for (TypesettingInfo nestedInfo : nestedInfos) {
                if (nestedInfo == null || nestedInfo.getTypesettingCells() == null || nestedInfo.getTypesettingCells().isEmpty()) {
                    continue;
                }
                accumulateProductionPieceQuantities(nestedInfo.getTypesettingCells(), totalQuantity, pieceQuantityMap, visitedTypesettingKeys);
            }
            visitedTypesettingKeys.remove(visitedKey);
        }
    }
}
