package com.mes.application.dto.resp.delivery;

import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class DeliveryPkgPiecesResponse {
    private List<DeliveryPkgPieceVO> items;
    private Long total;
    private Long current;
    private List<String> materialList;
    private List<Double> sizeList;
    private List<String> processList;

    public DeliveryPkgPiecesResponse(List<DeliveryPkgPieceVO> items,
                                     List<String> materialList,
                                     List<Double> sizeList,
                                     List<String> processList) {
        this(items, items == null ? 0L : (long) items.size(), 1L, materialList, sizeList, processList);
    }

    public DeliveryPkgPiecesResponse(List<DeliveryPkgPieceVO> items,
                                     Long total,
                                     Long current,
                                     List<String> materialList,
                                     List<Double> sizeList,
                                     List<String> processList) {
        this.items = items;
        this.total = total;
        this.current = current;
        this.materialList = materialList;
        this.sizeList = sizeList;
        this.processList = processList;
    }
}
