package com.mes.application.command.delivery;

import com.alibaba.fastjson.JSON;
import com.mes.application.command.delivery.req.AuthOrderRequest;
import com.mes.application.command.delivery.req.Kuaidi100OrderParam;
import com.mes.application.dto.req.delivery.DeliveryPkgRequest;
import com.mes.application.shared.utils.MD5Util;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryRecord;
import com.mes.domain.delivery.deliveryPkg.entity.DeliverySiid;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryManRepository;
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
import com.mes.domain.order.orderInfo.vo.OrderCustomer;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class AppDeliveryPkgService {

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
    private com.mes.domain.manufacturer.productionPiece.service.ProductionPieceService productionPieceService;

    @Autowired
    private com.mes.domain.delivery.deliveryPkg.repository.DeliveryRecordRepository deliveryRecordRepository;

    public void toPkg(DeliveryPkgRequest request) {
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
