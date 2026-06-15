package com.mes.application.dto.resp.delivery;

import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DeliveryPkgPiecesResponse {
    private List<DeliveryPkgPieceVO> items;
    private List<String> materialList;
    private List<Double> sizeList;
    private List<String> processList;
    private OrgInfo orgInfo;
}

