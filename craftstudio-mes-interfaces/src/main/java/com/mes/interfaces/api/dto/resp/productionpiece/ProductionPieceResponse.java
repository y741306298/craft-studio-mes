package com.mes.interfaces.api.dto.resp.productionpiece;

import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import lombok.Data;

import java.util.Date;

@Data
public class ProductionPieceResponse {

    private String id;

    private String productionPieceId;

    private String orderItemId;

    private String procedureFlowId;

    private String status;

    private String statusDescription;

    private String productionPieceType;

    private Integer quantity;

    private String templateCode;

    private String positionType;

    private String positionCode;

    private Date createTime;

    private Date updateTime;

    public static ProductionPieceResponse from(ProductionPiece piece) {
        if (piece == null) {
            return null;
        }

        ProductionPieceResponse response = new ProductionPieceResponse();
        response.setId(piece.getId());
        response.setProductionPieceId(piece.getProductionPieceId());
        response.setOrderItemId(piece.getOrderItemId());
        response.setProcedureFlowId(piece.getProcedureFlowId());
        response.setStatus(piece.getStatus());

        if (piece.getStatus() != null) {
            try {
                ProductionPieceStatus statusEnum = ProductionPieceStatus.getByCode(piece.getStatus());
                if (statusEnum != null) {
                    response.setStatusDescription(statusEnum.getDescription());
                }
            } catch (Exception e) {
                response.setStatusDescription(piece.getStatus());
            }
        }

        response.setProductionPieceType(piece.getProductionPieceType());
        response.setQuantity(piece.getQuantity());
        response.setTemplateCode(piece.getTemplateCode());
        response.setPositionType(piece.getPositionType());
        response.setPositionCode(piece.getPositionCode());
        response.setCreateTime(piece.getCreateTime());
        response.setUpdateTime(piece.getUpdateTime());

        return response;
    }
}
