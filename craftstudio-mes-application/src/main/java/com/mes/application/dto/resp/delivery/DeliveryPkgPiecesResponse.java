package com.mes.application.dto.resp.delivery;

import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
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
    private Long total;
    private Long current;
    private Long size;

    public DeliveryPkgPiecesResponse(List<DeliveryPkgPieceVO> items,
                                     List<String> materialList,
                                     List<Double> sizeList,
                                     List<String> processList) {
        this(items, materialList, sizeList, processList,
                items == null ? 0L : (long) items.size(), 1L,
                items == null ? 0L : (long) items.size());
    }
}
