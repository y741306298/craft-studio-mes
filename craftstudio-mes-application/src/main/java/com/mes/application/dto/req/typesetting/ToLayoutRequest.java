package com.mes.application.dto.req.typesetting;

import com.mes.application.dto.req.base.ApiRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.stream.Collectors;

/** {@code /toLayout} 的精简入参，保持现有 JSON 字段名不变。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ToLayoutRequest extends ApiRequest {

    private String manufacturerMetaId;
    private List<ToLayoutCellRequest> typesettingCells;
    private List<LayoutConfirmRequest.ContainerInfo> containers;
    private String layoutMode;

    @Override
    public boolean isValid() {
        return manufacturerMetaId != null && !manufacturerMetaId.isBlank()
                && layoutMode != null && !layoutMode.isBlank()
                && typesettingCells != null && !typesettingCells.isEmpty()
                && typesettingCells.stream().allMatch(cell -> cell != null && cell.isValid())
                && (containers == null || containers.stream().allMatch(container -> container != null
                        && container.getWidth() != null && container.getWidth() > 0
                        && container.getHeight() != null && container.getHeight() > 0));
    }

    @Override
    public String getValidationMessage() {
        return "工厂ID、排版方式和至少一个有效来源（sourceType、sourceId、正数quantity）不能为空，容器宽高必须为正数";
    }

    public LayoutConfirmRequest toLayoutConfirmRequest() {
        LayoutConfirmRequest request = new LayoutConfirmRequest();
        request.setManufacturerMetaId(manufacturerMetaId);
        request.setTypesettingCells(typesettingCells == null ? null : typesettingCells.stream()
                .map(ToLayoutCellRequest::toTypesettingProductionPiece)
                .collect(Collectors.toList()));
        request.setContainers(containers);
        request.setLayoutMode(layoutMode);
        return request;
    }
}
