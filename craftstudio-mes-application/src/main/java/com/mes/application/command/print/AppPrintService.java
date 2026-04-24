package com.mes.application.command.print;

import com.mes.application.command.print.vo.PrintReportResult;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
public class AppPrintService {

    @Autowired
    private TypesettingService typesettingService;

    @Autowired
    private ProductionPieceService productionPieceService;

    public PagedResult<TypesettingInfo> findPendingPrintTypesetting(String manufacturerMetaId, int current, int size) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "manufacturerMetaId 不能为空");
        }
        if (current < 1) {
            current = 1;
        }
        if (size < 1 || size > 100) {
            size = 20;
        }

        List<TypesettingInfo> items = typesettingService.findTypesettingByConditions(
                manufacturerMetaId,
                TypesettingStatus.PRINTING.getCode(),
                null,
                null,
                current,
                size
        );

        long total = typesettingService.countTypesettingByConditions(
                manufacturerMetaId,
                TypesettingStatus.PRINTING.getCode(),
                null,
                null
        );

        return new PagedResult<>(items, total, items.size(), current);
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
        if (request.getQuantity() != null) {
            dbInfo.setQuantity(request.getQuantity());
        }
        if (request.getLeaveQuantity() != null) {
            dbInfo.setLeaveQuantity(request.getLeaveQuantity());
        }
        typesettingService.updateTypesetting(dbInfo);

        boolean canComplete = request.getQuantity() != null
                && request.getLeaveQuantity() != null
                && request.getLeaveQuantity() <= 0
                && request.getQuantity() >= 0;

        int transferCount = 0;
        if (canComplete && dbInfo.getTypesettingCells() != null) {
            Map<String, Integer> pieceQuantityMap = new LinkedHashMap<>();
            accumulateProductionPieceQuantities(dbInfo.getTypesettingCells(), 1, pieceQuantityMap, new HashSet<>());
            for (Map.Entry<String, Integer> entry : pieceQuantityMap.entrySet()) {
                String productionPieceId = entry.getKey();
                Integer transferQuantity = entry.getValue();
                if (StringUtils.isBlank(productionPieceId) || transferQuantity == null || transferQuantity <= 0) {
                    continue;
                }

                ProductionPiece piece = productionPieceService.findByProductionPieceId(productionPieceId);
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
