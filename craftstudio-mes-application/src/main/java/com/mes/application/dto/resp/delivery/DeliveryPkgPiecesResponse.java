package com.mes.application.dto.resp.delivery;

import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
public class DeliveryPkgPiecesResponse {
    private List<DeliveryPkgPieceVO> items;
    private List<String> materialList;
    private List<Double> sizeList;
    private List<ProcessingFlowOption> processList;
    private OrgInfo orgInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingFlowOption {
        private String processName;
        private String accessoryName;
    }
}

