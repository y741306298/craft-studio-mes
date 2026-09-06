package com.mes.interfaces.api.platform.manufacturerSide.delivery;

import com.mes.domain.shared.utils.JsonLogUtil;
import com.alibaba.fastjson.JSON;
import com.mes.application.command.delivery.AppDeliveryPkgService;
import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.command.delivery.vo.DeliveryPkgAddResultVO;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgActionRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgScopedRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgListRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgAllListRequest;
import com.mes.application.dto.req.delivery.ImageSearchRequest;
import com.mes.application.dto.resp.delivery.DeliveryPkgPiecesResponse;
import com.mes.application.dto.resp.delivery.DeliveryPkgListItemResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.mes.domain.delivery.deliveryPkg.enums.DeliveryPkgStatus;
import com.mes.domain.delivery.deliveryPkg.service.DeliveryPkgService;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRouteNode;
import com.mes.domain.delivery.deliveryRoute.entity.RouteNode;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteNodeRepository;
import com.mes.domain.delivery.deliveryRoute.vo.OrgInfo;
import com.mes.domain.delivery.deliveryRoute.service.DeliveryRouteService;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.vo.LogisticsCarrierInfo;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.infra.oss.ImageToImageSearchServiceImp;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsCloudPrintData;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsCloudPrintPlatform;
import io.micrometer.common.util.StringUtils;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manufacturerSide/deliveryPkg")
@RequiredArgsConstructor
@Slf4j
public class DeliveryPkgController {

    private final AppDeliveryPkgService appDeliveryPkgService;
    private final DeliveryPkgService deliveryPkgService;
    private final DeliveryRouteService deliveryRouteService;
    private final DeliveryRouteNodeRepository deliveryRouteNodeRepository;
    private final ProductionPieceService productionPieceService;
    private final OrderItemService orderItemService;
    private final OrderInfoService orderInfoService;
    @Autowired
    private ImageToImageSearchServiceImp imageSearch;


    @Value("${business.pkg-detail-url:http://121.40.134.45:8083}")
    private String pkgDetailBaseUrl;

    /**
     * 查询待打包零件全量列表
     */
    @PostMapping("/list")
    public ApiResponse<DeliveryPkgPiecesResponse> listTypesettingAndProductionPiecesByPkg(@RequestBody DeliveryPkgRequest request) {
        List<DeliveryPkgPieceVO> allItems = appDeliveryPkgService.listPendingPackagingPieces(request);
        int current = request == null || request.getCurrent() == null || request.getCurrent() < 1 ? 1 : request.getCurrent();
        int size = request == null || request.getSize() == null || request.getSize() < 1 ? 50 : Math.min(request.getSize(), 100);
        int fromIndex = Math.min((current - 1) * size, allItems.size());
        int toIndex = Math.min(fromIndex + size, allItems.size());
        List<DeliveryPkgPieceVO> items = new ArrayList<>(allItems.subList(fromIndex, toIndex));
        DeliveryPkgPiecesResponse response = new DeliveryPkgPiecesResponse(
                items,
                (long) allItems.size(),
                (long) current,
                appDeliveryPkgService.buildMaterialList(allItems),
                appDeliveryPkgService.buildSizeList(allItems),
                appDeliveryPkgService.buildProcessList(allItems),
                findOrgInfoByOrderId(request == null ? null : request.getOrderId())
        );
        return ApiResponse.success(response);
    }


    /**
     * 按 orderId / orderItemId 之一全量查询待打包零件列表。
     */
    @PostMapping("/listById")
    public ApiResponse<DeliveryPkgPiecesResponse> listPendingPackagingPiecesById(@RequestBody DeliveryPkgScopedRequest request) {
        List<DeliveryPkgPieceVO> items = appDeliveryPkgService.listPendingPackagingPiecesById(request);
        DeliveryPkgPiecesResponse response = new DeliveryPkgPiecesResponse(
                items,
                (long) items.size(),
                1L,
                appDeliveryPkgService.buildMaterialList(items),
                appDeliveryPkgService.buildSizeList(items),
                appDeliveryPkgService.buildProcessList(items),
                findOrgInfoByOrderId(request == null ? null : request.getOrderId())
        );
        return ApiResponse.success(response);
    }


    private OrgInfo findOrgInfoByOrderId(String orderId) {
        if (StringUtils.isBlank(orderId)) {
            return null;
        }
        OrderInfo orderInfo = orderInfoService.findByOrderId(orderId);
        return orderInfo == null ? null : orderInfo.getOrgInfo();
    }


    @PostMapping("/pkgList")
    public PagedApiResponse<DeliveryPkgListItemResponse> listDeliveryPkgs(@Valid @RequestBody DeliveryPkgListRequest request) {
        DeliveryPkgStatus status = parseStatus(request.getStatus());

        List<DeliveryPkg> items = deliveryPkgService.queryByConditions(
                status,
                request.getManufacturerMetaId(),
                request.getOrgName(),
                request.getOrderId(),
                request.getRecipientName(),
                request.getRecipientPhone(),
                request.getKuaidiNum(),
                request.getCreateDateStart(),
                request.getCreateDateEnd(),
                request.getCurrent(),
                request.getSize()
        );
        List<DeliveryPkgListItemResponse> responseItems = buildDeliveryPkgListItemResponses(items);
        long total = deliveryPkgService.countByConditions(
                status,
                request.getManufacturerMetaId(),
                request.getOrgName(),
                request.getOrderId(),
                request.getRecipientName(),
                request.getRecipientPhone(),
                request.getKuaidiNum(),
                request.getCreateDateStart(),
                request.getCreateDateEnd()
        );
        return PagedApiResponse.success(responseItems, request.getCurrent(), request.getSize(), total);
    }

    /**
     * 根据查询条件返回全部包裹，不应用分页参数。
     */
    @PostMapping("/pkgListAll")
    public ApiResponse<List<DeliveryPkgListItemResponse>> listAllDeliveryPkgs(
            @Valid @RequestBody DeliveryPkgAllListRequest request) {
        List<DeliveryPkg> items = deliveryPkgService.queryAllByConditions(
                parseStatus(request.getStatus()), request.getManufacturerMetaId(), request.getOrgName(),
                request.getOrderId(),
                request.getRecipientName(), request.getRecipientPhone(), request.getKuaidiNum(),
                request.getCreateTimeStart(), request.getCreateTimeEnd());
        return ApiResponse.success(buildDeliveryPkgListItemResponses(items));
    }

    private List<DeliveryPkgListItemResponse> buildDeliveryPkgListItemResponses(List<DeliveryPkg> items) {
        Map<String, DeliveryRoute> routesById = deliveryRouteService.findByIds(items.stream()
                .map(DeliveryPkg::getRouteId).filter(StringUtils::isNotBlank).collect(Collectors.toSet()));
        LinkedHashSet<String> pieceIds = items.stream()
                .filter(Objects::nonNull)
                .flatMap(item -> item.getDeliveryPkgItems() == null
                        ? java.util.stream.Stream.empty() : item.getDeliveryPkgItems().stream())
                .filter(Objects::nonNull)
                .filter(item -> item.getProductionPieceId() != null)
                .flatMap(ids -> ids.getProductionPieceId().stream())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, ProductionPiece> piecesById = indexProductionPieces(
                productionPieceService.findByProductionPieceIds(pieceIds));
        LinkedHashSet<String> orderItemIds = items.stream()
                .filter(Objects::nonNull)
                .flatMap(item -> item.getDeliveryPkgItems() == null
                        ? java.util.stream.Stream.empty() : item.getDeliveryPkgItems().stream())
                .filter(Objects::nonNull)
                .map(item -> resolveOrderItemId(item.getOrderItemId(), item.getProductionPieceId(), piecesById))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, OrderItem> orderItemsById = indexOrderItems(orderItemService.findByOrderItemIds(orderItemIds));
        Map<String, OrderInfo> orderInfosById = orderInfoService.findByOrderIds(items.stream()
                        .map(DeliveryPkg::getOrderId).filter(StringUtils::isNotBlank).collect(Collectors.toSet()))
                .stream().filter(Objects::nonNull).filter(order -> StringUtils.isNotBlank(order.getOrderId()))
                .collect(Collectors.toMap(OrderInfo::getOrderId, order -> order, (first, ignored) -> first));

        items.forEach(item -> item.setRouteDesc(buildRouteDesc(item, routesById.get(item.getRouteId()))));
        return items.stream()
                .map(item -> buildDeliveryPkgListItemResponse(item, piecesById, orderItemsById,
                        orderInfosById.get(item.getOrderId())))
                .collect(Collectors.toList());
    }

    @PostMapping("/pkgDetail")
    public ApiResponse<DeliveryPkgListItemResponse> queryDeliveryPkgByPkgId(@RequestBody DeliveryPkgActionRequest request) {
        if (request == null || StringUtils.isBlank(request.getDeliveryPkgId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "pkgId参数不能为空");
        }
        DeliveryPkg deliveryPkg = appDeliveryPkgService.findByDeliveryPkgId(request.getDeliveryPkgId().trim());
        return ApiResponse.success(buildDeliveryPkgListItemResponses(List.of(deliveryPkg)).get(0));
    }

    private DeliveryPkgListItemResponse buildDeliveryPkgListItemResponse(DeliveryPkg deliveryPkg,
            Map<String, ProductionPiece> piecesById, Map<String, OrderItem> orderItemsById, OrderInfo orderInfo) {
        DeliveryPkgListItemResponse response = new DeliveryPkgListItemResponse();
        BeanUtils.copyProperties(deliveryPkg, response);
        response.setCarrierId(deliveryPkg.getCarrierId());
        response.setCarrierName(deliveryPkg.getCarrierName());
        LogisticsCarrierInfo logisticsCarrierInfo = new LogisticsCarrierInfo();
        logisticsCarrierInfo.setCarrierId(deliveryPkg.getCarrierId());
        logisticsCarrierInfo.setCarrierName(deliveryPkg.getCarrierName());
        logisticsCarrierInfo.setPresetType(deliveryPkg.getPresetType());
        response.setLogisticsCarrierInfo(logisticsCarrierInfo);
        response.setLogisticsCloudPrintData(deliveryPkg.getLogisticsCloudPrintData());
        response.setOrderInfo(orderInfo);

        List<DeliveryPkgListItemResponse.DeliveryPkgItemDetail> details = new ArrayList<>();
        if (deliveryPkg.getDeliveryPkgItems() != null) {
            deliveryPkg.getDeliveryPkgItems().forEach(item -> {
                DeliveryPkgListItemResponse.DeliveryPkgItemDetail detail = new DeliveryPkgListItemResponse.DeliveryPkgItemDetail();
                detail.setOrderItemId(item.getOrderItemId());
                detail.setProductionPieceId(item.getProductionPieceId());
                detail.setQuantity(item.getQuantity());
                detail.setPreviewUrl(item.getPreviewUrl());

                String pieceId = item.getProductionPieceId() == null || item.getProductionPieceId().isEmpty()
                        ? null : item.getProductionPieceId().get(0);

                if (StringUtils.isNotBlank(pieceId)) {
                    ProductionPiece productionPiece = piecesById.get(pieceId);
                    if (productionPiece != null) {
                        detail.setMaterialConfig(productionPiece.getMaterialConfig());
                        detail.setProcessingFlow(productionPiece.getProcessingFlow());
                        detail.setWidth(scaleToTwoDecimal(productionPiece.getWidth()));
                        detail.setHeight(scaleToTwoDecimal(productionPiece.getHeight()));
                        if (StringUtils.isBlank(detail.getOrderItemId())) {
                            detail.setOrderItemId(productionPiece.getOrderItemId());
                        }
                    }
                }

                if (StringUtils.isNotBlank(detail.getOrderItemId())) {
                    String orderItemId = detail.getOrderItemId().trim();
                    detail.setOrderItemId(orderItemId);
                    OrderItem orderItem = orderItemsById.get(orderItemId);
                    if (orderItem != null) {
                        detail.setOrderId(orderItem.getOrderId());
                    }
                }

                details.add(detail);
            });
        }
        response.setDeliveryPkgItems(details);
        return response;
    }

    private Map<String, ProductionPiece> indexProductionPieces(List<ProductionPiece> pieces) {
        Map<String, ProductionPiece> result = new java.util.HashMap<>();
        for (ProductionPiece piece : pieces) {
            if (piece == null) continue;
            if (StringUtils.isNotBlank(piece.getId())) result.putIfAbsent(piece.getId(), piece);
            if (StringUtils.isNotBlank(piece.getProductionPieceId())) {
                result.putIfAbsent(piece.getProductionPieceId(), piece);
            }
        }
        return result;
    }

    private Map<String, OrderItem> indexOrderItems(List<OrderItem> orderItems) {
        Map<String, OrderItem> result = new java.util.HashMap<>();
        for (OrderItem item : orderItems) {
            if (item == null) continue;
            if (StringUtils.isNotBlank(item.getId())) result.putIfAbsent(item.getId(), item);
            if (StringUtils.isNotBlank(item.getOrderItemId())) result.putIfAbsent(item.getOrderItemId(), item);
        }
        return result;
    }

    private String resolveOrderItemId(String orderItemId, List<String> pieceIds,
            Map<String, ProductionPiece> piecesById) {
        if (StringUtils.isNotBlank(orderItemId)) {
            return orderItemId.trim();
        }
        if (pieceIds == null || pieceIds.isEmpty()) {
            return null;
        }
        ProductionPiece piece = piecesById.get(pieceIds.get(0));
        return piece == null ? null : piece.getOrderItemId();
    }

    private Double scaleToTwoDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
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
        public ApiResponse<Object> addPkg(@RequestBody DeliveryPkgAddRequest request) {
        log.info("========== addPkg 入参开始 ==========");
        log.info("response: " + JsonLogUtil.toJSONString(request));
        log.info("========== addPkg 入参结束 ==========");
        DeliveryPkg deliveryPkg = appDeliveryPkgService.addPkg(request);
        return ApiResponse.success(buildAddResult(deliveryPkg));
    }

    /** 手动注销待打包零件，不触发任何面单下单或打印。 */
    @PostMapping("/manual-cancel")
    public ApiResponse<Object> manualCancel(@RequestBody DeliveryPkgAddRequest request) {
        DeliveryPkg deliveryPkg = appDeliveryPkgService.manualCancel(request);
        return ApiResponse.success(buildAddResult(deliveryPkg));
    }

    @PostMapping("/reprint")
    public ApiResponse<Object> reprint(@RequestBody DeliveryPkgActionRequest request) {
        DeliveryPkg deliveryPkg = appDeliveryPkgService.findByDeliveryPkgId(request.getDeliveryPkgId());
        if ("CUSTOM".equalsIgnoreCase(deliveryPkg.getPresetType())) {
            return ApiResponse.success(buildAddResult(deliveryPkg));
        }
        return ApiResponse.success(appDeliveryPkgService.reprintKuaidi100Label(request));
    }

    /** 聚单平台面单复打，抖音面单会重新获取云打印数据。 */
    @PostMapping("/gather-platform/reprint")
    public ApiResponse<Object> gatherPlatformReprint(@RequestBody DeliveryPkgActionRequest request) {
        DeliveryPkgAddResultVO deliveryPkgAddResultVO = buildAddResult(appDeliveryPkgService.reprintGatherPlatformLabel(request));
//        LogisticsCloudPrintData logisticsCloudPrintData = new LogisticsCloudPrintData();
//        logisticsCloudPrintData.setCloudPrintPlatform(LogisticsCloudPrintPlatform.CAINIAO);
//        logisticsCloudPrintData.setPrintData("{\"encryptedData\":\"AES:rU904rj6UH2oqfSUb43+Z199vDU2GZtH5/LVIEq0ppUJuwjq7qZ58JRvDHOQ/f2QhqdsWiYYFqp6z96+fPLupxENx4gZUSA/zYJbbdwevCzeI+22t7hONi0e8k4pytGn0cs+O/SQI1why/dhaLeykWnnlGU0C0H6rLb9Myr8u1LJAYfQ7SsjVBRaWlssLiH1kWSfGOTZIOkh5WTnbdxPi08HJV+ZEEBVCoPXVHJDpcK1R5+aEFHTaaPA8hdfslZFbDRsR2HRygU1aH9gkxrtOJOgL2h8xN4FxaScOFAueeM02fE0M6R5OW3/tG2db/uSO1LeFpLhsvAAavywxPYKKAqAWH/W/1cfllrQ8zk3ZeOZj+EAnpCyMNgmN0mEaeYUT6ZPB62QmecrMlnnX9yriq5dCRcGrXJ8uFw+zvzju4CK17MaFUJD/HACd+8PZ5t1P2fCfdiTD7g/0sJEH3AhlX889OFflzf5KaoBVcTynld9kmjs7l+TlyGYw7pxAD48S1LzyJ7o9JC/geZOxde6EWmx1py6vb0vuV4Nb8hTa/cVUGmyCFZQErNvVx6P2cm+MMFC1tsyafmHjlHQPyLQWIW1fQRC0H/3kPaw825rVcWNN7ZsOwZtldnkfXVahmBSMTZsLQfQChZLS8qHuP0hlcorKQaAcwlvE6vg0SrcDwFU30Mr+6Xfc2x91YMuEf+NqP+XxC98+FsZJzSYzHVC9hEGJxcuaKs2hUklrJEUPuv4+GAtLY1pmA21qR84rwXHbMcqlO+F+PMcP0JLh4A4qncqCUJQjni/JZZE2p4HqWsBmhtPiEM+gkCh44DSWfnu3i08RKer10WZ1lREgXGZNM+2YkZmfjABTPeUNFHp8fJCRzlrjgPfvX8m5bXIS/jyo2KnZdOd1AGH6tYZySR7yLVenM10Pt71e243+SSjc2sMXljW/Y02bfhuy4oAGTeePNeW9BWpUVqTbaIuKLDfcusYp97e+jK2IXFQ+M69riXJVYgZE43H+tBjFcX+IFtrL7G9hVc3Bih6zGqLpVfdVccZBjWaOTxQsqOSgDIB4j+njTfHM0OWxdo3xpGk4Tge5W436RUzvId3Z2uXjueGP3yIDrc63m6F5wE/S6OE65M0OFi1tLQn4KctuAzDWfFkb/lp0XnUkSUOtrShvNyb5283lbmbopUcmpRu6QvBcm1TZSSJBtveTw25OJpN3MGijVHQ0dPQFE7dLxhx+OArvGL80LeNBecEkp8395F0DZqyLocIEU1WCHV6SJ6ZzXiZc0sbMksXpPOiyPBiXlC4yGYWFwm1nP4C3i49ZGfxEpkaqhCQXgclXoajkPfWd7klf9BrtCuLkSFMb6JPBXbOTOGDIgsgJWYMLTF0Zy2IixyhrhomIN/TJrk8z0l4gsnnZd14OND8TLmH06lBnbO0rBVWE7BPhBYZwblmWze8h6TdQlFj0MUgX3XsCD9DGuqk8FI6Zany40vbfkI1wO2JGV8pw5tBbHsstgFGUtcw2SgAZLf2iPqTFiqVDPxvqn0suwki7ofWst33joTVPurWOEV0F8AnScsafEQJS9SKEEILUxPnKxk6XsmQQ1mkVhao8s8Et2H5W23cQ9kA3SwoWzlNRqOBhHPBrbBrHoOiXNFdqF0EQ+j08pCCtE2KZWEppD1M5/qnHgc8+1O/Ghj8lVFPYdG7mHQ2DyauPi0wdcJ9CsmHYup9OipoKbqMcPXkxwOkJBfcHx4UD2qumPRPqAkTECeltBufGZZaKuo26g+0aamnq99ImEIWfJSfimBgpOf8mqhI/1b/xsvUm4salPWra6kHlrZv47zGpQGFxQCV1Fxzhzwq79Cta+vdAlaf7I5RH3AdW/XB8Cuft9MRvQ0JZtlIejhlgcDQmAc5dl1DlfwgwLOtGaLEW9NowWmM+imVMlE0rYOhNXJhcH4S1LUDMc2PvnIcQjFWRcq1k/G0HUkJVg3oX8pTpnAAlhmtCF5AWGFUMLENJYUPj53BLsOfgTvso//05bXcSsLeGjKEhuihB5rfVC2H/NZoeZlmr/2ivyquIL/hq6BFMGyVHp+fNzPCckOPhPAvs2PKbHvsgq4XpRYXpemUUv6HGAtjNQzDYABb3WhPfTfUt7zO6hyv8OdjsUQzpJXn4iBiSZjEq1cx/SmTblCaM0RCsE2EZHw8G4jWCU+1z+MJEHjjNw1DfyPyX0fGrlQ7l6TUOZ+sB9VGbiane+TblE1KDw7emVp6k5wHI4oXa/WWCR66cA==\",\"signature\":\"MD:qqzuKvFz9MTlEMV3+vJFqw==\",\"templateURL\":\"http://cloudprint.cainiao.com/template/standard/300336\",\"ver\":\"waybill_print_secret_version_1\"}");
//        logisticsCloudPrintData.setCustomTemplateUrl("https://static-resources.wdtdata.com/production/yxm/ywyl/template/1/id1785394068083");
//        logisticsCloudPrintData.setStandardTemplateUrl("https://cloudprint.cainiao.com/template/standard/300336/89");
//        deliveryPkgAddResultVO.setLogisticsCloudPrintData(logisticsCloudPrintData);
        return ApiResponse.success(deliveryPkgAddResultVO);
    }

    @PostMapping("/release")
    public ApiResponse<Boolean> release(@RequestBody DeliveryPkgActionRequest request) {
        appDeliveryPkgService.releasePkg(request.getDeliveryPkgId());
        return ApiResponse.success(Boolean.TRUE);
    }

    private DeliveryPkgAddResultVO buildAddResult(DeliveryPkg deliveryPkg) {
        DeliveryPkgAddResultVO result = new DeliveryPkgAddResultVO();
        result.setPkgId(deliveryPkg.getDeliveryPkgId());
        result.setCarrierId(deliveryPkg.getCarrierId());
        result.setCarrierName(deliveryPkg.getCarrierName());
        result.setPresetType(deliveryPkg.getPresetType());
        result.setRecipientName(deliveryPkg.getRecipientName());
        result.setRecipientMobile(deliveryPkg.getRecipientPhone());
        result.setRecipientAddress(deliveryPkg.getRecipientAddress());
        result.setWidth("70.00");
        result.setHeight("90.00");

        DeliveryPkgAddResultVO.QrCodeInfo qrCode = new DeliveryPkgAddResultVO.QrCodeInfo();
        qrCode.setFormat("base64-png");
        qrCode.setWidth(30.00);
        qrCode.setHeight(30.00);
        String qrTargetUrl = buildPkgDetailUrl(deliveryPkg.getDeliveryPkgId());
        qrCode.setContent(generateQrCodeBase64(qrTargetUrl));
        result.setQrCode(qrCode);

        DeliveryPkgAddResultVO.BarCodeInfo barCode = new DeliveryPkgAddResultVO.BarCodeInfo();
        barCode.setFormat("base64-png");
        barCode.setWidth(70.00);
        barCode.setHeight(25.00);
        barCode.setContent("https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/line.jpg");
        result.setBarCode(barCode);

        String routeDesc = "";
        routeDesc = buildRouteDesc(deliveryPkg);
        result.setRouteDesc(routeDesc);
        result.setRemark(deliveryPkg.getRemarks());
        result.setOrgInfo(resolveOrgInfo(deliveryPkg));
        result.setLogisticsCloudPrintData(deliveryPkg.getLogisticsCloudPrintData());

        return result;
    }

    private OrgInfo resolveOrgInfo(DeliveryPkg deliveryPkg) {
        if (deliveryPkg == null) {
            return null;
        }
        if (deliveryPkg.getOrgInfo() != null) {
            return deliveryPkg.getOrgInfo();
        }
        return findOrgInfoByOrderId(deliveryPkg.getOrderId());
    }

    private String buildPkgDetailUrl(String pkgId) {
        return UriComponentsBuilder
                .fromUriString(pkgDetailBaseUrl)
                .queryParam("deliveryPkgId", pkgId)
                .build()
                .toUriString();
    }

    private String generateQrCodeBase64(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] bytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    private String buildRouteDesc(DeliveryPkg deliveryPkg) {
        if (StringUtils.isBlank(deliveryPkg.getRouteId()) || StringUtils.isBlank(deliveryPkg.getRouteNodeId())) {
            return "未定义路线";
        }
        DeliveryRoute deliveryRoute = deliveryRouteService.findById(deliveryPkg.getRouteId());
        return buildRouteDesc(deliveryPkg, deliveryRoute);
    }

    private String buildRouteDesc(DeliveryPkg deliveryPkg, DeliveryRoute deliveryRoute) {
        if (deliveryPkg == null || StringUtils.isBlank(deliveryPkg.getRouteId())
                || StringUtils.isBlank(deliveryPkg.getRouteNodeId())) {
            return "未定义路线";
        }
        if (deliveryRoute == null) {
            return "未定义路线";
        }
        String destDistrictName = null;
        for (RouteNode routeNode : deliveryRoute.getRouteNodes()) {
            if (routeNode.getId().equals(deliveryPkg.getRouteNodeId())) {
                destDistrictName = routeNode.getName();
            }
        }

        return deliveryRoute.getRouteName() + "-" + destDistrictName;
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

    /**
     * 将oss下的图片变为向量并保存
     * @param prefix
     * @return
     */
    @GetMapping("/testScanAndIndexImages")
    public ApiResponse<String> testScanAndIndexImages(@RequestParam(required = false) String prefix) {
        try {
            System.out.println("Starting to scan and index images from OSS...");
            String scanPrefix = prefix != null ? prefix : "pieceImg/";
            int count = imageSearch.scanAndIndexImagesFromOSS(scanPrefix);
            return ApiResponse.success("Successfully indexed " + count + " images");
        } catch (Exception e) {
            System.err.println("Failed to scan and index images: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError, "Failed: " + e.getMessage());
        }
    }

    @PostMapping("/EndToEndImageSearch")
    public ApiResponse<DeliveryPkgPiecesResponse> EndToEndImageSearch(@RequestBody ImageSearchRequest request) {
        try {
            System.out.println("Step 1: Generating embedding for query image base64");
            float[] queryVector = imageSearch.generateImageEmbeddingByBase64(request.getQueryImageBase64());
            System.out.println("Query vector generated, dimension: " + queryVector.length);

            Instant startAt = parseStartTime(request.getStartTime());

            System.out.println("Step 2: Searching for similar images in DashVector...");
            String filter = String.format("manufacturerMetaId = '%s'", request.getManufacturerMetaId());
            if (startAt != null) {
                filter += String.format(" and uploadedAt > '%s'", startAt.toString());
            }

            Integer topK = request.getTopK() != null ? request.getTopK() : 50;
            List<ImageToImageSearchServiceImp.ImageSearchResult> results =
                    imageSearch.searchSimilarImages(queryVector, topK, filter);

            List<DeliveryPkgPieceVO> pieceVOS = results.stream()
                    .map(result -> {
                        String productionPieceId = result.getProductionPieceId();
                        if (StringUtils.isBlank(productionPieceId)) {
                            return null;
                        }
                        DeliveryPkgPieceVO vo = appDeliveryPkgService.findPendingPackagingPieceById(productionPieceId);
                        if (vo != null) {
                            vo.setScore(result.getScore());
                        }
                        return vo;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            DeliveryPkgPiecesResponse response = new DeliveryPkgPiecesResponse(
                    pieceVOS,
                    (long) pieceVOS.size(),
                    1L,
                    appDeliveryPkgService.buildMaterialList(pieceVOS),
                    appDeliveryPkgService.buildSizeList(pieceVOS),
                    appDeliveryPkgService.buildProcessList(pieceVOS),
                    null
            );

            System.out.println("Search completed, found " + pieceVOS.size() + " packaging-ready pieces");
            return ApiResponse.success(response);

        } catch (BusinessNotAllowException e) {
            return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, e.getMessage());
        } catch (Exception e) {
            System.err.println("End-to-end test failed: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError, "Failed: " + e.getMessage());
        }
    }


    private Instant parseStartTime(Date startTime) {
        if (startTime == null) {
            return null;
        }
        return startTime.toInstant();
    }

}
