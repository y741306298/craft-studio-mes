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
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteNodeRepository;
import com.mes.domain.delivery.deliveryRoute.service.DeliveryRouteService;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.geo.world.repository.WorldRepository;
import com.piliofpala.craftstudio.shared.domain.geo.world.vo.World;
import io.micrometer.common.util.StringUtils;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/manufacturerSide/deliveryPkg")
@RequiredArgsConstructor
public class DeliveryPkgController {

    private final AppDeliveryPkgService appDeliveryPkgService;
    private final DeliveryPkgService deliveryPkgService;
    private final DeliveryRouteService deliveryRouteService;
    private final DeliveryRouteNodeRepository deliveryRouteNodeRepository;

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
        DeliveryPkg deliveryPkg = appDeliveryPkgService.addPkg(request);

        DeliveryPkgAddResultVO result = new DeliveryPkgAddResultVO();
        result.setPkgId(deliveryPkg.getDeliveryPkgId());
        result.setRecipientName(deliveryPkg.getRecipientName());
        result.setRecipientMobile(deliveryPkg.getRecipientPhone());
        result.setRecipientAddress(deliveryPkg.getRecipientAddress());
        result.setWidth("70.00");
        result.setHeight("90.00");
        
        DeliveryPkgAddResultVO.QrCodeInfo qrCode = new DeliveryPkgAddResultVO.QrCodeInfo();
        qrCode.setFormat("base64-png");
        qrCode.setWidth(30.00);
        qrCode.setHeight(30.00);
        qrCode.setContent("https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/qr.jpeg");
        result.setQrCode(qrCode);

        DeliveryPkgAddResultVO.BarCodeInfo barCode = new DeliveryPkgAddResultVO.BarCodeInfo();
        barCode.setFormat("base64-png");
        barCode.setWidth(70.00);
        barCode.setHeight(25.00);
        barCode.setContent("https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/line.jpg");
        result.setBarCode(barCode);

        String routeDesc = "";
        if (StringUtils.isNotBlank(deliveryPkg.getRouteId()) && StringUtils.isNotBlank(deliveryPkg.getRouteNodeId())) {
            DeliveryRoute deliveryRoute = deliveryRouteService.findByRouteId(deliveryPkg.getRouteId());
            DeliveryRouteNode routeNode = deliveryRouteNodeRepository.findByRouteNodeId(deliveryPkg.getRouteNodeId());
            if (deliveryRoute != null && routeNode != null) {
                routeDesc = deliveryRoute.getRouteName() + ":" + routeNode.getDistrictName() + "-" + routeNode.getDestDistrictName();
            }
        }
        result.setRouteDesc(routeDesc);
        result.setRemark("这是一个备注");

        return ApiResponse.success(result);
    }

    @PostMapping("/validatePieces")
    public ApiResponse<Boolean> validatePieces(@RequestBody List<DeliveryPkgPieceVO> pieces) {
        if (pieces == null || pieces.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "打包项不能为空");
        }

        String orderId = pieces.get(0).getOrderId();
        for (DeliveryPkgPieceVO piece : pieces) {
            if (!Objects.equals(orderId, piece.getOrderId())) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "打包项不是来自同一订单，不能一起打包");
            }
        }

        Object logisticsCarrierInfo = pieces.get(0).getLogisticsCarrierInfo();
        for (DeliveryPkgPieceVO piece : pieces) {
            if (!Objects.equals(logisticsCarrierInfo, piece.getLogisticsCarrierInfo())) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "打包项物流方式不一致，不能一起打包");
            }
        }

        return ApiResponse.success(Boolean.TRUE);
    }
}
