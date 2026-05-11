package com.mes.interfaces.api.platform.manufacturerSide.delivery;

import com.mes.application.command.delivery.AppDeliveryPkgService;
import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.command.delivery.vo.DeliveryPkgAddResultVO;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgActionRequest;
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
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.infra.oss.ImageToImageSearchServiceImp;
import io.micrometer.common.util.StringUtils;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
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
    @Autowired
    private ImageToImageSearchServiceImp imageSearch;

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
                request.getOrderId(),
                request.getRecipientName(),
                request.getRecipientPhone(),
                request.getCreateTimeStart(),
                request.getCreateTimeEnd(),
                request.getCurrent(),
                request.getSize()
        );
        items.forEach(item -> item.setRouteDesc(buildRouteDesc(item)));
        long total = deliveryPkgService.countByConditions(
                status,
                request.getManufacturerMetaId(),
                request.getOrderId(),
                request.getRecipientName(),
                request.getRecipientPhone(),
                request.getCreateTimeStart(),
                request.getCreateTimeEnd()
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
        qrCode.setContent("https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/basetag/qr.jpeg");
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

    private String buildRouteDesc(DeliveryPkg deliveryPkg) {
        if (StringUtils.isBlank(deliveryPkg.getRouteId()) || StringUtils.isBlank(deliveryPkg.getRouteNodeId())) {
            return "";
        }
        DeliveryRoute deliveryRoute = deliveryRouteService.findByRouteId(deliveryPkg.getRouteId());
        DeliveryRouteNode routeNode = deliveryRouteNodeRepository.findByRouteNodeId(deliveryPkg.getRouteNodeId());
        if (deliveryRoute == null || routeNode == null) {
            return "";
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

    @GetMapping("/EndToEndImageSearch")
    public ApiResponse<List<DeliveryPkgPieceVO>> EndToEndImageSearch(
            @RequestParam String queryImageUrl,
            @RequestParam(defaultValue = "20") Integer topK) {
        try {
            System.out.println("Step 1: Generating embedding for query image: " + queryImageUrl);
            float[] queryVector = imageSearch.generateImageEmbedding(queryImageUrl);
            System.out.println("Query vector generated, dimension: " + queryVector.length);

            System.out.println("Step 2: Searching for similar images in DashVector...");
            List<ImageToImageSearchServiceImp.ImageSearchResult> results =
                imageSearch.searchSimilarImages(queryVector, topK);

            List<DeliveryPkgPieceVO> pieceVOS = results.stream()
                    .map(ImageToImageSearchServiceImp.ImageSearchResult::getProductionPieceId)
                    .filter(StringUtils::isNotBlank)
                    .map(productionPieceService::findByProductionPieceId)
                    .filter(Objects::nonNull)
                    .filter(this::hasPendingPackagingQuantity)
                    .map(DeliveryPkgPieceVO::fromProductionPiece)
                    .collect(Collectors.toList());

            System.out.println("Search completed, found " + pieceVOS.size() + " packaging-ready pieces");
            return ApiResponse.success(pieceVOS);

        } catch (Exception e) {
            System.err.println("End-to-end test failed: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.fail(ApiResponse.RepStatusCode.serviceError, "Failed: " + e.getMessage());
        }
    }

    private boolean hasPendingPackagingQuantity(ProductionPiece piece) {
        if (piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
            return false;
        }
        for (ProcedureFlowNode node : piece.getProcedureFlow().getNodes()) {
            if ("待打包".equals(node.getNodeName())) {
                return node.getPieceQuantity() != null && node.getPieceQuantity() > 0;
            }
        }
        return false;
    }

    @GetMapping("/testUpsertImageVector")
    public void testUpsertImageVector() {
        try {
            String imageUrl = "https://craftstudio-mes-test.oss-cn-hangzhou.aliyuncs.com/pieceImg/69fb388ed7913b07a47afef1/dd82cd9dc39bf2d304f7802fd4c1ad8d_part001_thumbnail.png";
            String docId = "test-image-001";

            System.out.println("Step 1: Generating embedding for image...");
            float[] vector = imageSearch.generateImageEmbedding(imageUrl);
            System.out.println("Vector generated, dimension: " + vector.length);

            System.out.println("Step 2: Upserting vector to DashVector...");
            boolean success = imageSearch.upsertImageVector(docId, imageUrl, vector);
            System.out.println("Upsert result: " + (success ? "SUCCESS" : "FAILED"));

        } catch (Exception e) {
            System.err.println("Upsert test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
