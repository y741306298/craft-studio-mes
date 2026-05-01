package com.mes.interfaces.api.platform.manufacturerSide.delivery;

import com.mes.application.command.delivery.AppDeliveryPkgService;
import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgRequest;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.domain.base.repository.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manufacturerSide/deliveryPkg")
@RequiredArgsConstructor
public class DeliveryPkgController {

    private final AppDeliveryPkgService appDeliveryPkgService;

    /**
     * 查询待打包零件全量列表
     */
    @PostMapping("/list")
    public PagedApiResponse<DeliveryPkgPieceVO> listTypesettingAndProductionPieces(@RequestBody DeliveryPkgRequest request) {
        List<DeliveryPkgPieceVO> items = appDeliveryPkgService.listPendingPackagingPieces(request.getManufacturerMetaId());
        return PagedApiResponse.success(items, 1, items.size(), items.size());
    }

    @PostMapping("/add")
    public ApiResponse<String> addPkg(@RequestBody DeliveryPkgAddRequest request) {
        appDeliveryPkgService.addPkg(request);
        return ApiResponse.success("success");
    }
}
