package com.mes.application.command.delivery;

import com.alibaba.fastjson.JSON;
import com.mes.application.command.delivery.req.AuthOrderRequest;
import com.mes.application.command.delivery.req.Kuaidi100OrderParam;
import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgRequest;
import com.mes.application.shared.utils.MD5Util;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryRecord;
import com.mes.domain.delivery.deliveryPkg.entity.DeliverySiid;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryManRepository;
import com.mes.domain.delivery.deliveryPkg.service.DeliveryPkgService;
import com.mes.domain.delivery.deliveryPkg.repository.DeliverySiidRepository;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryTokenRepository;
import com.mes.domain.delivery.deliveryPkg.vo.AuthOrderResponse;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.productionPiece.entity.DeliveryPkgInfo;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.geo.world.repository.WorldRepository;
import com.piliofpala.craftstudio.shared.domain.geo.world.vo.World;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class AppDeliveryPkgService {

    @Autowired
    private WorldRepository worldRepository;

    private final static String DELIVERYKEY = "lCnjtXBY2496";
    private final static String DELIVERYCUSTOMER = "DAAB0437EF6D9C03B8B4FC96C165FFB1";
    private final static String DELIVERYSECRET = "8868a7ce6733416b844d964b56bb716f";
    private final static String MAPTRACKURL = "https://poll.kuaidi100.com/poll/maptrack.do";

    @Autowired
    private DeliveryManRepository deliveryManRepository;

    @Autowired
    private DeliveryTokenRepository deliveryTokenRepository;

    @Autowired
    private DeliverySiidRepository deliverySiidRepository;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService productionPieceService;

    @Autowired
    private com.mes.domain.delivery.deliveryPkg.repository.DeliveryRecordRepository deliveryRecordRepository;

    @Autowired
    private DeliveryPkgService deliveryPkgService;


    public List<DeliveryPkgPieceVO> listPendingPackagingPieces(String manufacturerMetaId) {
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "manufacturerMetaId 不能为空");
        }

        List<ProductionPiece> productionPieces = productionPieceService.findProductionPiecesByConditions(
                manufacturerMetaId,
                null,
                null,
                null,
                null,
                null,
                1,
                Integer.MAX_VALUE
        );

        List<DeliveryPkgPieceVO> items = new ArrayList<>();
        for (ProductionPiece productionPiece : productionPieces) {
            int pendingQty = getNodeQuantity(productionPiece, "待打包");
            if (pendingQty <= 0) {
                continue;
            }
            int packedQty = getNodeQuantity(productionPiece, "已打包");

            DeliveryPkgPieceVO vo = DeliveryPkgPieceVO.fromProductionPiece(productionPiece);
            vo.setPendingPkgQuantity(pendingQty);
            vo.setPackedQuantity(packedQty);
            vo.setStatus(resolvePackagingStatus(pendingQty, packedQty));

            OrderItem orderItem = orderItemService.findByOrderItemId(productionPiece.getOrderItemId());
            if (orderItem != null) {
                vo.setLogisticsCarrierInfo(orderItem.getLogisticsCarrierInfo());
                if (orderItem.getMaterial() != null) {
                    vo.setMaterialConfig(orderItem.getMaterial());
                }
                if (StringUtils.isBlank(vo.getOrderId())) {
                    vo.setOrderId(orderItem.getOrderId());
                }
            }

            if (StringUtils.isNotBlank(vo.getOrderId())) {
                OrderInfo orderInfo = orderInfoService.findByOrderId(vo.getOrderId());
                if (orderInfo != null) {
                    vo.setOrderCustomer(orderInfo.getCustomer());
                    World world = worldRepository.loadWorld();
                    Address address = new Address(orderInfo.getCustomer().getAddress().getTerminalRegionCode(), orderInfo.getCustomer().getAddress().getDetailAddress());
                    String fullAddress = address.buildFullAddressString(world);
                    vo.setAddress(fullAddress);
                }
            }
            items.add(vo);
        }

        return items;
    }

    private int getNodeQuantity(ProductionPiece piece, String nodeName) {
        if (piece == null || piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
            return 0;
        }
        return piece.getProcedureFlow().getNodes().stream()
                .filter(Objects::nonNull)
                .filter(node -> nodeName.equals(node.getNodeName()))
                .map(ProcedureFlowNode::getPieceQuantity)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(0);
    }

    private String resolvePackagingStatus(int pendingQty, int packedQty) {
        if (packedQty == 0) {
            return "待打包";
        }
        if (pendingQty > 0) {
            return "部分打包";
        }
        return "已完成";
    }

    public String toPkg(DeliveryPkgRequest request) {
        String url = "https://api.kuaidi100.com/label/order";
        String deliveryManId = request.getDeliveryManId();
        String orderId = request.getOrderId();
        String carrierId = request.getCarrierId();
        String deliverySiidId = request.getDeliverySiidId();
        String manufacturerMetaId = request.getManufacturerMetaId();
        List<ProductionPiece> productionPieces = request.getProductionPieces();
        //查询寄件人信息
        DeliveryMan deliveryMan = deliveryManRepository.findByDeliveryManIdAndManufacturerMetaId(deliveryManId, manufacturerMetaId);
        //查询快递方式基本配置
        DeliveryToken deliveryToken = deliveryTokenRepository.findByCarrierIdAndManufacturerMetaId(carrierId, manufacturerMetaId);
        //查询云打印设备
        DeliverySiid deliverySiid = deliverySiidRepository.findByDeliverySiidIdAndManufacturerMetaId(deliverySiidId, manufacturerMetaId);
        //查询订单信息
        OrderInfo orderInfo = orderInfoService.findByOrderId(orderId);
        OrderCustomer customer = orderInfo.getCustomer();
        //组装请求参数
        Kuaidi100OrderParam kuaidi100OrderParam = Kuaidi100OrderParam.createKuaidi100OrderParam(request, deliveryToken, deliveryMan, customer);
        String paramStr = JSON.toJSONString(kuaidi100OrderParam);
        // 5. 调用快递100 API
        String result = callPost(url, paramStr, "label.order");
        AuthOrderResponse response = JSON.parseObject(result, AuthOrderResponse.class);
        //添加打印记录
        DeliveryRecord deliveryRecord = this.createDeliveryRecord(request);
        if (response.getSuccess()){
            String kuaidinum = response.getData().getKuaidinum();
            deliveryRecord.setKuaidiNum(kuaidinum);
            deliveryRecord.setDeliveryTime(new Date());
            deliveryRecord.setIsSuccess(true);
            deliveryRecordRepository.add(deliveryRecord);

            // 6. 获取快递单号并更新零件数量
            if (StringUtils.isNotBlank(kuaidinum) && productionPieces != null) {
                for (ProductionPiece productionPiece : productionPieces) {
                    List<ProcedureFlowNode> nodes = productionPiece.getProcedureFlow().getNodes();

                    ProcedureFlowNode pendingPackingNode = null;
                    ProcedureFlowNode packedNode = null;
                    int pendingPackingIndex = -1;

                    for (int i = 0; i < nodes.size(); i++) {
                        ProcedureFlowNode node = nodes.get(i);
                        if ("待打包".equals(node.getNodeName())) {
                            pendingPackingNode = node;
                            pendingPackingIndex = i;
                        } else if ("已打包".equals(node.getNodeName())) {
                            packedNode = node;
                        }
                    }

                    if (pendingPackingNode != null && packedNode != null) {
                        Integer quantity = productionPiece.getQuantity();
                        if (quantity != null && quantity > 0) {
                            Integer pendingQuantity = pendingPackingNode.getPieceQuantity() != null ? pendingPackingNode.getPieceQuantity() : 0;
                            pendingPackingNode.setPieceQuantity(pendingQuantity - quantity);

                            if (pendingPackingNode.getPieceQuantity() <= 0) {
                                pendingPackingNode.setNodeStatus(NodeStatus.COMPLETED);
                            }

                            Integer packedQuantity = packedNode.getPieceQuantity() != null ? packedNode.getPieceQuantity() : 0;
                            packedNode.setPieceQuantity(packedQuantity + quantity);
                            packedNode.setNodeStatus(NodeStatus.ACTIVE);
                            DeliveryPkgInfo deliveryPkgInfo = new DeliveryPkgInfo();
                            deliveryPkgInfo.setCarrierId(carrierId);
                            deliveryPkgInfo.setKuaidiNum(kuaidinum);
                            deliveryPkgInfo.setQuantity(quantity);
                            productionPiece.setDeliveryPkgInfos(productionPiece.getDeliveryPkgInfos());
                            productionPieceService.updateProductionPiece(productionPiece);
                        }
                    }
                }
            }
        }else{
            deliveryRecord.setDeliveryTime(new Date());
            deliveryRecord.setIsSuccess(false);
            deliveryRecord.setErrorMsg(response.getMessage());
            deliveryRecordRepository.add(deliveryRecord);
            //返回错误原因
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.serviceError,response.getMessage());
        }

        return response.getData() == null ? null : response.getData().getTaskId();
    }

    public DeliveryPkg addPkg(DeliveryPkgAddRequest request) {
        if (request == null || request.getPieces() == null || request.getPieces().isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "打包零件不能为空");
        }
        if (StringUtils.isBlank(request.getDeliveryManId()) || StringUtils.isBlank(request.getDeliverySiidId())
                || StringUtils.isBlank(request.getManufacturerMetaId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "发货人、打印机、商家信息不能为空");
        }

        String orderId = null;
        String carrierId = null;
        String carrierName = null;
        List<ProductionPiece> selectedPieces = new ArrayList<>();
        for (DeliveryPkgAddRequest.DeliveryPkgPieceItem item : request.getPieces()) {
            if (item == null || item.getPiece() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "零件与打包数量必须填写且数量大于0");
            }
            DeliveryPkgPieceVO pieceVO = item.getPiece();
            if (StringUtils.isBlank(pieceVO.getProductionPieceId()) || pieceVO.getLogisticsCarrierInfo() == null) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "零件信息不完整");
            }

            if (StringUtils.isBlank(orderId)) {
                orderId = pieceVO.getOrderId();
                carrierId = pieceVO.getLogisticsCarrierInfo().getCarrierId();
                carrierName = pieceVO.getLogisticsCarrierInfo().getCarrierName();
            } else if (!Objects.equals(orderId, pieceVO.getOrderId())
                    || !Objects.equals(carrierId, pieceVO.getLogisticsCarrierInfo().getCarrierId())) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "仅支持同一订单且同一物流方式一起打包");
            }

            ProductionPiece sourcePiece = productionPieceService.findByProductionPieceId(pieceVO.getProductionPieceId());
            if (sourcePiece == null) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "存在无效的生产零件");
            }
            int pendingQty = getNodeQuantity(sourcePiece, "待打包");
            if (item.getQuantity() > pendingQty) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams,
                        "零件[" + pieceVO.getProductionPieceId() + "]打包数量超过待打包数量");
            }
            sourcePiece.setQuantity(item.getQuantity());
            selectedPieces.add(sourcePiece);
        }

        DeliveryPkg deliveryPkg = createAndSaveDeliveryPkg(request, orderId, carrierId, carrierName);

        boolean isMyselfDelivery = "自主配送".equals(carrierName) || "送货上门".equals(carrierName);
        if (isMyselfDelivery) {
            if (StringUtils.isBlank(request.getRouteId()) || StringUtils.isBlank(request.getRouteNodeId())) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "自主配送必须指定路线和段落");
            }
            for (ProductionPiece productionPiece : selectedPieces) {
                List<ProcedureFlowNode> nodes = productionPiece.getProcedureFlow().getNodes();
                ProcedureFlowNode pendingPackingNode = null;
                ProcedureFlowNode packedNode = null;
                for (ProcedureFlowNode node : nodes) {
                    if ("待打包".equals(node.getNodeName())) {
                        pendingPackingNode = node;
                    } else if ("已打包".equals(node.getNodeName())) {
                        packedNode = node;
                    }
                }

                if (pendingPackingNode != null && packedNode != null) {
                    Integer quantity = productionPiece.getQuantity();
                    Integer pendingQuantity = pendingPackingNode.getPieceQuantity() != null ? pendingPackingNode.getPieceQuantity() : 0;
                    pendingPackingNode.setPieceQuantity(pendingQuantity - quantity);
                    if (pendingPackingNode.getPieceQuantity() <= 0) {
                        pendingPackingNode.setNodeStatus(NodeStatus.COMPLETED);
                    }
                    Integer packedQuantity = packedNode.getPieceQuantity() != null ? packedNode.getPieceQuantity() : 0;
                    packedNode.setPieceQuantity(packedQuantity + quantity);
                    packedNode.setNodeStatus(NodeStatus.ACTIVE);

                    List<DeliveryPkgInfo> pkgInfos = productionPiece.getDeliveryPkgInfos();
                    if (pkgInfos == null) {
                        pkgInfos = new ArrayList<>();
                    }
                    DeliveryPkgInfo deliveryPkgInfo = new DeliveryPkgInfo();
                    deliveryPkgInfo.setCarrierId(carrierId);
                    deliveryPkgInfo.setCarrierName(carrierName + "(" + request.getRouteId() + "/" + request.getRouteNodeId() + ")");
                    deliveryPkgInfo.setQuantity(quantity);
                    pkgInfos.add(deliveryPkgInfo);
                    productionPiece.setDeliveryPkgInfos(pkgInfos);
                    productionPieceService.updateProductionPiece(productionPiece);
                }
            }
            return deliveryPkg;
        }

        DeliveryPkgRequest toPkgRequest = new DeliveryPkgRequest();
        toPkgRequest.setProductionPieces(selectedPieces);
        toPkgRequest.setOrderId(orderId);
        toPkgRequest.setCarrierId(request.getCarrierId());
        toPkgRequest.setDeliveryManId(request.getDeliveryManId());
        toPkgRequest.setDeliverySiidId(request.getDeliverySiidId());
        toPkgRequest.setManufacturerMetaId(request.getManufacturerMetaId());
        String taskId = this.toPkg(toPkgRequest);
        if (StringUtils.isNotBlank(taskId)) {
            deliveryPkg.setDeliveryPkgCode(taskId);
            deliveryPkgService.updateDeliveryPkg(deliveryPkg);
        }
        return deliveryPkg;
    }


    private DeliveryPkg createAndSaveDeliveryPkg(DeliveryPkgAddRequest request, String orderId, String carrierId, String carrierName) {
        DeliveryPkg deliveryPkg = new DeliveryPkg();
        String deliveryPkgId = IdGenerator.generateId("DP");
        deliveryPkg.setDeliveryPkgId(deliveryPkgId);
        deliveryPkg.setDeliveryPkgCode(deliveryPkgId);
        deliveryPkg.setOrderId(orderId);
        deliveryPkg.setCarrierId(carrierId);
        deliveryPkg.setCarrierName(carrierName);
        deliveryPkg.setDeliveryManId(request.getDeliveryManId());
        deliveryPkg.setDeliverySiidId(request.getDeliverySiidId());
        deliveryPkg.setManufacturerMetaId(request.getManufacturerMetaId());
        deliveryPkg.setRouteId(request.getRouteId());
        deliveryPkg.setRouteNodeId(request.getRouteNodeId());

        List<com.mes.domain.delivery.deliveryPkg.vo.DeliveryPkgItem> pkgItems = new ArrayList<>();
        for (DeliveryPkgAddRequest.DeliveryPkgPieceItem item : request.getPieces()) {
            com.mes.domain.delivery.deliveryPkg.vo.DeliveryPkgItem pkgItem = new com.mes.domain.delivery.deliveryPkg.vo.DeliveryPkgItem();
            pkgItem.setOrderItemId(item.getPiece().getOrderItemId());
            pkgItem.setProductionPieceId(Collections.singletonList(item.getPiece().getProductionPieceId()));
            pkgItem.setQuantity(item.getQuantity());
            pkgItems.add(pkgItem);
        }
        deliveryPkg.setDeliveryPkgItems(pkgItems);

        DeliveryPkgPieceVO firstPiece = request.getPieces().get(0).getPiece();
        if (firstPiece.getOrderCustomer() != null) {
            deliveryPkg.setRecipientName(firstPiece.getOrderCustomer().getCustomerName());
            deliveryPkg.setRecipientPhone(firstPiece.getOrderCustomer().getCustomerPhone());
            if (firstPiece.getOrderCustomer().getAddress() != null) {
                Address address = firstPiece.getOrderCustomer().getAddress();
                String s = address.buildFullAddressString(worldRepository.loadWorld());
                deliveryPkg.setRecipientAddress(s);
            }
        }

        return deliveryPkgService.createDeliveryPkg(deliveryPkg);
    }

    private String callPost(String url,String paramStr,String method){
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type","application/x-www-form-urlencoded");
        MultiValueMap param = new LinkedMultiValueMap();
        long t = new Date().getTime();
        String sign = this.getSign(paramStr, t);
        param.add("method",method);
        param.add("key",DELIVERYKEY);
        param.add("sign",sign);
        param.add("t",t);
        param.add("param",paramStr);
        HttpEntity httpEntity = new HttpEntity<>(param,headers);
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.postForObject(url,httpEntity, String.class);
        return result;
    }

    private String getSign(String paramStr,long t){
        String timeStr = String.valueOf(t);
        String signStr = paramStr+timeStr+DELIVERYKEY+DELIVERYSECRET;
        String sign = MD5Util.stringToMD5(signStr);
        return sign;
    }

    private String getMapSign(String paramStr){
        String key = DELIVERYKEY;
        String customer = DELIVERYCUSTOMER;
        String signStr = paramStr+key+customer;
        String sign = MD5Util.stringToMD5(signStr);
        return sign;
    }

    public static DeliveryRecord createDeliveryRecord(DeliveryPkgRequest request) {
        DeliveryRecord record = new DeliveryRecord();
        record.setOrderId(request.getOrderId());
        record.setCarrierId(request.getCarrierId());
        record.setDeliveryManId(request.getDeliveryManId());
        record.setDeliverySiidId(request.getDeliverySiidId());
        record.setUserId(request.getUserId());
        record.setManufacturerMetaId(request.getManufacturerMetaId());
        record.setRemark(request.getRemark());
        List<ProductionPiece> productionPieces = request.getProductionPieces();
        ArrayList<DeliveryRecord.ProductionPieceDTO> productionPieceDTOs = new ArrayList<DeliveryRecord.ProductionPieceDTO>();
        for (ProductionPiece productionPiece : productionPieces) {
            DeliveryRecord.ProductionPieceDTO productionPieceDTO = new DeliveryRecord.ProductionPieceDTO();
            productionPieceDTO.setProductionPieceId(productionPiece.getId());
            productionPieceDTO.setQuantity(productionPiece.getQuantity());
            productionPieceDTOs.add(productionPieceDTO);
        }
        record.setPieces(productionPieceDTOs);
        return record;
    }

}
