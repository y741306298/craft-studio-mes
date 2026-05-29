package com.mes.application.command.typesetting.layout;

import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.service.TypesettingService;
import com.mes.domain.manufacturer.typesetting.vo.TypesettingSourceCell;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the common order id represented by all production pieces in a QR layout.
 */
@Slf4j
@Component
public class QrLayoutOrderIdResolver {

    private final OrderItemService orderItemService;
    private final TypesettingService typesettingService;

    public QrLayoutOrderIdResolver(OrderItemService orderItemService, TypesettingService typesettingService) {
        this.orderItemService = orderItemService;
        this.typesettingService = typesettingService;
    }

    /**
     * Returns the common orderId when every distinct production piece in the current layout
     * and nested plate cells belongs to the same order; otherwise returns {@code null}.
     */
    public String resolveCommonOrderId(TypesettingInfo info) {
        Map<String, String> productionPieceOrderItemIds = new LinkedHashMap<>();
        collectProductionPieceOrderItemIds(info, productionPieceOrderItemIds, new HashSet<>());
        if (productionPieceOrderItemIds.isEmpty()) {
            return null;
        }

        Set<String> orderIds = new LinkedHashSet<>();
        for (String orderItemId : productionPieceOrderItemIds.values()) {
            if (StringUtils.isBlank(orderItemId)) {
                return null;
            }
            OrderItem orderItem = findOrderItem(orderItemId);
            if (orderItem == null || StringUtils.isBlank(orderItem.getOrderId())) {
                return null;
            }
            orderIds.add(orderItem.getOrderId());
            if (orderIds.size() > 1) {
                return null;
            }
        }
        return orderIds.iterator().next();
    }

    private void collectProductionPieceOrderItemIds(TypesettingInfo info,
                                                    Map<String, String> productionPieceOrderItemIds,
                                                    Set<String> visitedTypesettingIds) {
        if (info == null || info.getTypesettingCells() == null || info.getTypesettingCells().isEmpty()) {
            return;
        }
        String visitKey = StringUtils.isNotBlank(info.getId()) ? info.getId() : info.getTypesettingId();
        if (StringUtils.isNotBlank(visitKey) && !visitedTypesettingIds.add(visitKey)) {
            return;
        }

        List<TypesettingSourceCell> cells = info.getTypesettingCells();
        for (TypesettingSourceCell cell : cells) {
            if (cell == null || StringUtils.isBlank(cell.getSourceType()) || StringUtils.isBlank(cell.getSourceId())) {
                continue;
            }
            if (TypesettingSourceType.PART.getCode().equals(cell.getSourceType())) {
                putProductionPieceOrderItemId(productionPieceOrderItemIds, cell);
                continue;
            }
            if (!TypesettingSourceType.TYPESETTING.getCode().equals(cell.getSourceType())) {
                continue;
            }
            TypesettingInfo nestedInfo = findTypesetting(cell.getSourceId());
            collectProductionPieceOrderItemIds(nestedInfo, productionPieceOrderItemIds, visitedTypesettingIds);
        }
    }

    private void putProductionPieceOrderItemId(Map<String, String> productionPieceOrderItemIds,
                                               TypesettingSourceCell cell) {
        String existingOrderItemId = productionPieceOrderItemIds.get(cell.getSourceId());
        if (StringUtils.isBlank(existingOrderItemId) && StringUtils.isNotBlank(cell.getOrderItemId())) {
            productionPieceOrderItemIds.put(cell.getSourceId(), cell.getOrderItemId());
            return;
        }
        productionPieceOrderItemIds.putIfAbsent(cell.getSourceId(), cell.getOrderItemId());
    }

    private TypesettingInfo findTypesetting(String sourceId) {
        if (StringUtils.isBlank(sourceId)) {
            return null;
        }
        try {
            TypesettingInfo info = typesettingService.findById(sourceId);
            if (info != null) {
                return info;
            }
        } catch (Exception e) {
            log.warn("根据 ID 查询印版失败，尝试按 typesettingId 查询: sourceId={}, error={}", sourceId, e.getMessage());
        }
        try {
            return typesettingService.findTypesettingByTypesettingId(sourceId);
        } catch (Exception e) {
            log.warn("根据 typesettingId 查询印版失败: sourceId={}, error={}", sourceId, e.getMessage());
            return null;
        }
    }

    private OrderItem findOrderItem(String orderItemId) {
        try {
            OrderItem orderItem = orderItemService.findByOrderItemId(orderItemId);
            if (orderItem != null) {
                return orderItem;
            }
        } catch (Exception e) {
            log.warn("根据 orderItemId 查询订单项失败，尝试按 ID 查询: orderItemId={}, error={}", orderItemId, e.getMessage());
        }
        try {
            return orderItemService.findById(orderItemId);
        } catch (Exception e) {
            log.warn("根据 ID 查询订单项失败: orderItemId={}, error={}", orderItemId, e.getMessage());
            return null;
        }
    }
}
