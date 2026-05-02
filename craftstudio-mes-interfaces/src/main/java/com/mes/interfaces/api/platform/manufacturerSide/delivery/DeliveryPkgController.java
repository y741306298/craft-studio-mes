package com.mes.interfaces.api.platform.manufacturerSide.delivery;

import com.mes.application.command.delivery.AppDeliveryPkgService;
import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.command.delivery.vo.DeliveryPkgAddResultVO;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgListRequest;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.mes.domain.delivery.deliveryPkg.enums.DeliveryPkgStatus;
import com.mes.domain.delivery.deliveryPkg.service.DeliveryPkgService;
import io.micrometer.common.util.StringUtils;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
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
    private final DeliveryPkgService deliveryPkgService;

    /**
     * 查询待打包零件全量列表
     */
    @PostMapping("/list")
    public PagedApiResponse<DeliveryPkgPieceVO> listTypesettingAndProductionPieces(@RequestBody DeliveryPkgRequest request) {
        List<DeliveryPkgPieceVO> items = appDeliveryPkgService.listPendingPackagingPieces(request.getManufacturerMetaId());
        return PagedApiResponse.success(items, 1, items.size(), items.size());
    }


    @PostMapping("/pkgList")
    public PagedApiResponse<DeliveryPkg> listDeliveryPkgs(@Valid @RequestBody DeliveryPkgListRequest request) {
        DeliveryPkgStatus status = parseStatus(request.getStatus());

        List<DeliveryPkg> items = deliveryPkgService.queryByConditions(
                status,
                request.getManufacturerMetaId(),
                request.getRecipientName(),
                request.getTrackingNumber(),
                request.getCurrent(),
                request.getSize()
        );
        long total = deliveryPkgService.countByConditions(
                status,
                request.getManufacturerMetaId(),
                request.getRecipientName(),
                request.getTrackingNumber()
        );
        return PagedApiResponse.success(items, request.getCurrent(), request.getSize(), total);
    }

    private DeliveryPkgStatus parseStatus(String statusValue) {
        if (StringUtils.isBlank(statusValue)) {
            return null;
        }
        for (DeliveryPkgStatus status : DeliveryPkgStatus.values()) {
            if (status.name().equalsIgnoreCase(statusValue) || status.getDescription().equals(statusValue)) {
                return status;
            }
        }
        throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "status参数无效");
    }

    @PostMapping("/add")
    public ApiResponse<DeliveryPkgAddResultVO> addPkg(@RequestBody DeliveryPkgAddRequest request) {
        DeliveryPkgAddResultVO result = new DeliveryPkgAddResultVO();
        result.setPkgId("PKG202605020001");
        result.setRecipientName("张三");
        result.setRecipientMobile("13800138000");
        result.setRecipientAddress("上海市浦东新区世纪大道100号A座1201室");
        
        DeliveryPkgAddResultVO.QrCodeInfo qrCode = new DeliveryPkgAddResultVO.QrCodeInfo();
        qrCode.setFormat("base64-png");
        qrCode.setContent("https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/mark/69de4119091f6f6f5199bb68/LAYOUT1777537459918BD5308/b090224e-84e4-4292-8e17-12e3982b6904.png");
        result.setQrCode(qrCode);
        
        result.setRouteDesc("路线A:南昌市红谷滩区-九江市修水县");
        result.setRemark("这是一个备注");
        
//        appDeliveryPkgService.addPkg(request);
        return ApiResponse.success(result);
    }
}
