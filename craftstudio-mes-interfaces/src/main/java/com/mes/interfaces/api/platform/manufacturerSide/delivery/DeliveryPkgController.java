package com.mes.interfaces.api.platform.manufacturerSide.delivery;

import com.mes.application.command.delivery.AppDeliveryPkgService;
import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.command.delivery.vo.DeliveryPkgAddResultVO;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgActionRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgListRequest;
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
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteNodeRepository;
import com.mes.domain.delivery.deliveryRoute.service.DeliveryRouteService;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.infra.oss.ImageToImageSearchServiceImp;
import io.micrometer.common.util.StringUtils;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manufacturerSide/deliveryPkg")
@RequiredArgsConstructor
public class DeliveryPkgController {

    private final AppDeliveryPkgService appDeliveryPkgService;
    private final DeliveryPkgService deliveryPkgService;
    private final DeliveryRouteService deliveryRouteService;
    private final DeliveryRouteNodeRepository deliveryRouteNodeRepository;
    private final ProductionPieceService productionPieceService;
    private final OrderItemService orderItemService;
    @Autowired
    private ImageToImageSearchServiceImp imageSearch;

    /**
     * 查询待打包零件全量列表
     */
    @PostMapping("/list")
    public ApiResponse<DeliveryPkgPiecesResponse> listTypesettingAndProductionPieces(@RequestBody DeliveryPkgRequest request) {
        List<DeliveryPkgPieceVO> items = appDeliveryPkgService.listPendingPackagingPieces(request);
        DeliveryPkgPiecesResponse response = new DeliveryPkgPiecesResponse(
                items,
                appDeliveryPkgService.buildMaterialList(items),
                appDeliveryPkgService.buildSizeList(items),
                appDeliveryPkgService.buildProcessList(items)
        );
        return ApiResponse.success(response);
    }


    @PostMapping("/pkgList")
    public PagedApiResponse<DeliveryPkgListItemResponse> listDeliveryPkgs(@Valid @RequestBody DeliveryPkgListRequest request) {
        DeliveryPkgStatus status = parseStatus(request.getStatus());

        List<DeliveryPkg> items = deliveryPkgService.queryByConditions(
                status,
                request.getManufacturerMetaId(),
                request.getOrderId(),
                request.getRecipientName(),
                request.getRecipientPhone(),
                request.getCreateTimeStart(),
                request.getCreateTimeEnd(),
                request.getCurrent(),
                request.getSize()
        );
        items.forEach(item -> item.setRouteDesc(buildRouteDesc(item)));
        List<DeliveryPkgListItemResponse> responseItems = items.stream()
                .map(this::buildDeliveryPkgListItemResponse)
                .collect(Collectors.toList());
        long total = deliveryPkgService.countByConditions(
                status,
                request.getManufacturerMetaId(),
                request.getOrderId(),
                request.getRecipientName(),
                request.getRecipientPhone(),
                request.getCreateTimeStart(),
                request.getCreateTimeEnd()
        );
        return PagedApiResponse.success(responseItems, request.getCurrent(), request.getSize(), total);
    }

    @PostMapping("/pkgDetail")
    public ApiResponse<DeliveryPkgListItemResponse> queryDeliveryPkgByPkgId(@RequestBody DeliveryPkgActionRequest request) {
        if (request == null || StringUtils.isBlank(request.getDeliveryPkgId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "pkgId参数不能为空");
        }
        DeliveryPkg deliveryPkg = appDeliveryPkgService.findByDeliveryPkgId(request.getDeliveryPkgId().trim());
        deliveryPkg.setRouteDesc(buildRouteDesc(deliveryPkg));
        return ApiResponse.success(buildDeliveryPkgListItemResponse(deliveryPkg));
    }


    private DeliveryPkgListItemResponse buildDeliveryPkgListItemResponse(DeliveryPkg deliveryPkg) {
        DeliveryPkgListItemResponse response = new DeliveryPkgListItemResponse();
        BeanUtils.copyProperties(deliveryPkg, response);

        List<DeliveryPkgListItemResponse.DeliveryPkgItemDetail> details = new ArrayList<>();
        if (deliveryPkg.getDeliveryPkgItems() != null) {
            deliveryPkg.getDeliveryPkgItems().forEach(item -> {
                DeliveryPkgListItemResponse.DeliveryPkgItemDetail detail = new DeliveryPkgListItemResponse.DeliveryPkgItemDetail();
                detail.setOrderItemId(item.getOrderItemId());
                detail.setProductionPieceId(item.getProductionPieceId());
                detail.setQuantity(item.getQuantity());
                detail.setPreviewUrl(item.getPreviewUrl());

                String pieceId = item.getProductionPieceId().get(0);

                if (StringUtils.isNotBlank(pieceId)) {
                    ProductionPiece productionPiece = productionPieceService.findByProductionPieceId(pieceId);
                    if (productionPiece == null) {
                        productionPiece = productionPieceService.findById(pieceId);
                    }
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
                    OrderItem orderItem = orderItemService.findByOrderItemId(orderItemId);
                    if (orderItem == null) {
                        orderItem = orderItemService.findById(orderItemId);
                    }
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
    public ApiResponse<DeliveryPkgAddResultVO> addPkg(@RequestBody DeliveryPkgAddRequest request) {
        DeliveryPkg deliveryPkg = appDeliveryPkgService.addPkg(request);
        return ApiResponse.success(buildAddResult(deliveryPkg));
    }

    @PostMapping("/reprint")
    public ApiResponse<DeliveryPkgAddResultVO> reprint(@RequestBody DeliveryPkgActionRequest request) {
        DeliveryPkg deliveryPkg = appDeliveryPkgService.findByDeliveryPkgId(request.getDeliveryPkgId());
        return ApiResponse.success(buildAddResult(deliveryPkg));
    }

    @PostMapping("/release")
    public ApiResponse<Boolean> release(@RequestBody DeliveryPkgActionRequest request) {
        appDeliveryPkgService.releasePkg(request.getDeliveryPkgId());
        return ApiResponse.success(Boolean.TRUE);
    }

    private DeliveryPkgAddResultVO buildAddResult(DeliveryPkg deliveryPkg) {
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
        result.setRemark("这是一个备注");

        return result;
    }


    private String buildPkgDetailUrl(String pkgId) {
        return UriComponentsBuilder
                .fromUriString("http://121.40.134.45:8083")
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
            return "未自定义路线";
        }
        DeliveryRoute deliveryRoute = deliveryRouteService.findByRouteId(deliveryPkg.getRouteId());
        DeliveryRouteNode routeNode = deliveryRouteNodeRepository.findByRouteNodeId(deliveryPkg.getRouteNodeId());
        if (deliveryRoute == null || routeNode == null) {
            return "未自定义路线";
        }
        return deliveryRoute.getRouteName() + ":" + routeNode.getDistrictName() + "-" + routeNode.getDestDistrictName();
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
                    appDeliveryPkgService.buildMaterialList(pieceVOS),
                    appDeliveryPkgService.buildSizeList(pieceVOS),
                    appDeliveryPkgService.buildProcessList(pieceVOS)
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
