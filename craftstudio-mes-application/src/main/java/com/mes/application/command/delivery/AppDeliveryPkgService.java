package com.mes.application.command.delivery;

import com.alibaba.fastjson.JSON;
import com.mes.application.command.delivery.req.AuthOrderRequest;
import com.mes.application.command.delivery.req.Kuaidi100OrderParam;
import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;
import com.mes.application.dto.req.delivery.DeliveryPkgActionRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgAddRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgRequest;
import com.mes.application.dto.req.delivery.DeliveryPkgScopedRequest;
import com.mes.application.dto.resp.delivery.DeliveryPkgPiecesResponse;
import com.mes.application.shared.utils.MD5Util;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryMan;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryPkg;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryRecord;
import com.mes.domain.delivery.deliveryPkg.entity.DeliverySiid;
import com.mes.domain.delivery.deliveryPkg.entity.DeliveryToken;
import com.mes.domain.delivery.deliveryPkg.enums.PreOrderLabelConsumeStatus;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryManRepository;
import com.mes.domain.delivery.deliveryPkg.service.DeliveryPkgService;
import com.mes.domain.delivery.deliveryPkg.repository.DeliverySiidRepository;
import com.mes.domain.delivery.deliveryPkg.repository.DeliveryTokenRepository;
import com.mes.domain.delivery.deliveryPkg.vo.AuthOrderResponse;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.gatherplatform.wdt.entity.WdtLabelRecord;
import com.mes.domain.gatherplatform.wdt.repository.WdtLabelRecordRepository;
import com.mes.domain.manufacturer.procedureFlow.vo.ProcessingFlowCondition;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.productionPiece.entity.DeliveryPkgInfo;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.service.OrderInfoService;
import com.mes.domain.order.orderInfo.service.OrderItemService;
import com.mes.domain.order.enums.OrderStatus;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Service
public class AppDeliveryPkgService {

    private static final int SCOPED_FULL_LIST_SIZE = 999;

    private static final String NODE_ID_PENDING_PACKING = "NODE_PENDING_PACKING";
    private static final String NODE_ID_PACKED = "NODE_PACKAGED";
    private static final String NODE_NAME_PENDING_PACKING = "待打包";
    private static final String NODE_NAME_PACKED = "已打包";
    private static final String PRE_ORDER_SIID = "KX100L3AD65411C274";

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

    @Autowired
    private WdtLabelRecordRepository wdtLabelRecordRepository;


    public List<DeliveryPkgPieceVO> listPendingPackagingPieces(DeliveryPkgRequest request) {
        String manufacturerMetaId = request.getManufacturerMetaId();
        if (StringUtils.isBlank(manufacturerMetaId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "manufacturerMetaId 不能为空");
        }

        List<ProductionPiece> productionPieces = productionPieceService.listPendingPackagingPiecesByConditions(
                manufacturerMetaId,
                request.getMaterialName(),
                resolveProcessConditions(request.getProcessNames(), request.getProcessName()),
                request.getWidth(),
                request.getRouteId());

        List<DeliveryPkgPieceVO> items = new ArrayList<>();
        if (productionPieces == null) {
            productionPieces = new ArrayList<>();
        }
        for (ProductionPiece productionPiece : productionPieces) {
            DeliveryPkgPieceVO vo = buildPendingPackagingPieceVO(productionPiece);
            if (vo != null) {
                items.add(vo);
            }
        }

        return items.stream().filter(item -> {
            boolean matchOrderId = StringUtils.isBlank(request.getOrderId())
                    || (StringUtils.isNotBlank(item.getOrderId()) && item.getOrderId().contains(request.getOrderId()));
            boolean matchOrderItemId = StringUtils.isBlank(request.getOrderItemId())
                    || (StringUtils.isNotBlank(item.getOrderItemId()) && item.getOrderItemId().contains(request.getOrderItemId()));
            boolean matchCustomerName = StringUtils.isBlank(request.getCustomerName())
                    || (item.getOrderCustomer() != null && StringUtils.isNotBlank(item.getOrderCustomer().getCustomerName())
                    && item.getOrderCustomer().getCustomerName().contains(request.getCustomerName()));
            boolean matchCustomerPhone = StringUtils.isBlank(request.getCustomerPhone())
                    || (item.getOrderCustomer() != null && StringUtils.isNotBlank(item.getOrderCustomer().getCustomerPhone())
                    && item.getOrderCustomer().getCustomerPhone().contains(request.getCustomerPhone()));
            boolean matchCarrierName = StringUtils.isBlank(request.getCarrierName())
                    || (item.getLogisticsCarrierInfo() != null && StringUtils.isNotBlank(item.getLogisticsCarrierInfo().getCarrierName())
                    && item.getLogisticsCarrierInfo().getCarrierName().contains(request.getCarrierName()));
            boolean matchOrgName = StringUtils.isBlank(request.getOrgName())
                    || (item.getOrgInfo() != null && StringUtils.isNotBlank(item.getOrgInfo().getName())
                    && item.getOrgInfo().getName().contains(request.getOrgName()));
            boolean matchStart = request.getStartTime() == null || (item.getCreateTime() != null && !item.getCreateTime().before(request.getStartTime()));
            boolean matchEnd = request.getEndTime() == null || (item.getCreateTime() != null && !item.getCreateTime().after(request.getEndTime()));
            return matchOrderId && matchOrderItemId && matchCustomerName && matchCustomerPhone && matchCarrierName && matchOrgName && matchStart && matchEnd;
        }).sorted(Comparator
                .comparing((DeliveryPkgPieceVO item) -> Boolean.TRUE.equals(item.getIsUrgent()))
                .reversed()
                .thenComparing(DeliveryPkgPieceVO::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }


    public List<DeliveryPkgPieceVO> listPendingPackagingPiecesById(DeliveryPkgScopedRequest request) {
        if (request == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "查询参数不能为空");
        }
        if (StringUtils.isBlank(request.getManufacturerMetaId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "manufacturerMetaId 不能为空");
        }
        validateSingleDeliveryScopedId(request);

        List<ProductionPiece> productionPieces = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getOrderId())) {
            int current = 1;
            while (true) {
                List<OrderItem> orderItems = orderItemService.findByOrderId(request.getOrderId().trim(), request.getManufacturerMetaId(), current, 100);
                if (orderItems == null || orderItems.isEmpty()) {
                    break;
                }
                orderItems.stream()
                        .map(OrderItem::getOrderItemId)
                        .filter(StringUtils::isNotBlank)
                        .map(orderItemId -> listPendingPackagingPiecesByOrderItemId(request, orderItemId))
                        .forEach(productionPieces::addAll);
                if (orderItems.size() < 100) {
                    break;
                }
                current++;
            }
        } else {
            productionPieces.addAll(listPendingPackagingPiecesByOrderItemId(request, request.getOrderItemId()));
        }

        List<DeliveryPkgPieceVO> items = new ArrayList<>();
        if (productionPieces == null) {
            productionPieces = new ArrayList<>();
        }
        for (ProductionPiece productionPiece : productionPieces) {
            DeliveryPkgPieceVO vo = buildPendingPackagingPieceVO(productionPiece);
            if (vo != null) {
                items.add(vo);
            }
        }

        return items.stream()
                .filter(item -> matchesDeliveryScopedRequest(item, request))
                .sorted(Comparator
                        .comparing((DeliveryPkgPieceVO item) -> Boolean.TRUE.equals(item.getIsUrgent()))
                        .reversed()
                        .thenComparing(DeliveryPkgPieceVO::getCreateTime,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private void validateSingleDeliveryScopedId(DeliveryPkgScopedRequest request) {
        int idCount = 0;
        if (StringUtils.isNotBlank(request.getOrderId())) {
            idCount++;
        }
        if (StringUtils.isNotBlank(request.getOrderItemId())) {
            idCount++;
        }
        if (idCount != 1) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "orderId、orderItemId 必须且只能传一个");
        }
    }

    private List<ProductionPiece> listPendingPackagingPiecesByOrderItemId(DeliveryPkgScopedRequest request, String orderItemId) {
        return productionPieceService.findProductionPiecesByConditions(
                request.getManufacturerMetaId(),
                null,
                request.getMaterialName(),
                null,
                orderItemId,
                null,
                null,
                1,
                SCOPED_FULL_LIST_SIZE
        );
    }

    private boolean matchesDeliveryScopedRequest(DeliveryPkgPieceVO item, DeliveryPkgScopedRequest request) {
        boolean matchOrderId = StringUtils.isBlank(request.getOrderId())
                || (StringUtils.isNotBlank(item.getOrderId()) && item.getOrderId().contains(request.getOrderId()));
        boolean matchCustomerName = StringUtils.isBlank(request.getCustomerName())
                || (item.getOrderCustomer() != null && StringUtils.isNotBlank(item.getOrderCustomer().getCustomerName())
                && item.getOrderCustomer().getCustomerName().contains(request.getCustomerName()));
        boolean matchCustomerPhone = StringUtils.isBlank(request.getCustomerPhone())
                || (item.getOrderCustomer() != null && StringUtils.isNotBlank(item.getOrderCustomer().getCustomerPhone())
                && item.getOrderCustomer().getCustomerPhone().contains(request.getCustomerPhone()));
        boolean matchCarrierName = StringUtils.isBlank(request.getCarrierName())
                || (item.getLogisticsCarrierInfo() != null && StringUtils.isNotBlank(item.getLogisticsCarrierInfo().getCarrierName())
                && item.getLogisticsCarrierInfo().getCarrierName().contains(request.getCarrierName()));
        boolean matchStart = request.getStartTime() == null || (item.getCreateTime() != null && !item.getCreateTime().before(request.getStartTime()));
        boolean matchEnd = request.getEndTime() == null || (item.getCreateTime() != null && !item.getCreateTime().after(request.getEndTime()));
        boolean matchWidth = request.getWidth() == null || Objects.equals(item.getWidth(), request.getWidth());
        boolean matchProcesses = matchesAllProcessNames(item, resolveLegacyProcessNames(request.getProcessNames(), request.getProcessName()));
        return matchOrderId && matchCustomerName && matchCustomerPhone && matchCarrierName && matchStart && matchEnd && matchWidth && matchProcesses;
    }

    private List<ProcessingFlowCondition> resolveProcessConditions(List<ProcessingFlowCondition> processNames, String processName) {
        List<ProcessingFlowCondition> resolved = processNames == null ? new ArrayList<>() : processNames.stream()
                .filter(Objects::nonNull)
                .filter(condition -> StringUtils.isNotBlank(condition.getProcessName()))
                .map(condition -> {
                    ProcessingFlowCondition normalized = new ProcessingFlowCondition();
                    normalized.setProcessName(condition.getProcessName().trim());
                    normalized.setAccessoryName(StringUtils.isBlank(condition.getAccessoryName()) ? null : condition.getAccessoryName().trim());
                    return normalized;
                })
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                condition -> condition.getProcessName() + "\u0000" + (condition.getAccessoryName() == null ? "" : condition.getAccessoryName()),
                                condition -> condition,
                                (left, right) -> left,
                                java.util.LinkedHashMap::new
                        ),
                        map -> new ArrayList<>(map.values())
                ));
        if (resolved.isEmpty() && StringUtils.isNotBlank(processName)) {
            ProcessingFlowCondition condition = new ProcessingFlowCondition();
            condition.setProcessName(processName.trim());
            resolved.add(condition);
        }
        return resolved;
    }


    private List<String> resolveLegacyProcessNames(List<String> processNames, String processName) {
        List<String> resolved = processNames == null ? new ArrayList<>() : processNames.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (resolved.isEmpty() && StringUtils.isNotBlank(processName)) {
            resolved.add(processName.trim());
        }
        return resolved;
    }

    private boolean matchesAllProcessNames(DeliveryPkgPieceVO item, List<String> processNames) {
        if (processNames == null || processNames.isEmpty()) {
            return true;
        }
        if (item == null || item.getProcedureFlow() == null || item.getProcedureFlow().getNodes() == null) {
            return false;
        }
        java.util.Set<String> itemProcessNames = item.getProcedureFlow().getNodes().stream()
                .filter(Objects::nonNull)
                .map(ProcedureFlowNode::getNodeName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        return itemProcessNames.containsAll(processNames);
    }



    public DeliveryPkgPieceVO findPendingPackagingPieceById(String productionPieceId) {
        if (StringUtils.isBlank(productionPieceId)) {
            return null;
        }
        ProductionPiece productionPiece = productionPieceService.findByProductionPieceId(productionPieceId);
        return buildPendingPackagingPieceVO(productionPiece);
    }

    private DeliveryPkgPieceVO buildPendingPackagingPieceVO(ProductionPiece productionPiece) {
        if (productionPiece == null) {
            return null;
        }
        int pendingQty = getNodeQuantity(productionPiece, NODE_ID_PENDING_PACKING, NODE_NAME_PENDING_PACKING);
        if (pendingQty <= 0) {
            return null;
        }
        int packedQty = getNodeQuantity(productionPiece, NODE_ID_PACKED, NODE_NAME_PACKED);

        DeliveryPkgPieceVO vo = DeliveryPkgPieceVO.fromProductionPiece(productionPiece);
        vo.setPendingPkgQuantity(pendingQty);
        vo.setPackedQuantity(packedQty);
        vo.setStatus(resolvePackagingStatus(pendingQty, packedQty));

        OrderItem orderItem = orderItemService.findByOrderItemId(productionPiece.getOrderItemId());
        if (orderItem != null) {
            vo.setLogisticsCarrierInfo(orderItem.getLogisticsCarrierInfo());
            if (vo.getOrgInfo() == null) {
                vo.setOrgInfo(orderItem.getOrgInfo());
            }
            if (orderItem.getMaterial() != null) {
                vo.setMaterialConfig(orderItem.getMaterial());
            }
            if (StringUtils.isBlank(vo.getOrderId())) {
                vo.setOrderId(orderItem.getOrderId());
            }
            if (StringUtils.isBlank(vo.getRouteId())) {
                vo.setRouteId(orderItem.getRouteId());
            }
            if (StringUtils.isBlank(vo.getRouteNodeId())) {
                vo.setRouteNodeId(orderItem.getRouteNodeId());
            }
        }

        if (StringUtils.isNotBlank(vo.getOrderId())) {
            OrderInfo orderInfo = orderInfoService.findByOrderId(vo.getOrderId());
            if (orderInfo != null) {
                vo.setOrderCustomer(orderInfo.getCustomer());
                if (vo.getOrgInfo() == null) {
                    vo.setOrgInfo(orderInfo.getOrgInfo());
                }
                World world = worldRepository.loadWorld();
                Address address = new Address(orderInfo.getCustomer().getAddress().getTerminalRegionCode(), orderInfo.getCustomer().getAddress().getDetailAddress());
                String fullAddress = address.buildFullAddressString(world);
                vo.setAddress(fullAddress);
            }
        }
        return vo;
    }

    public List<String> buildMaterialList(List<DeliveryPkgPieceVO> items) {
        return items.stream().filter(Objects::nonNull)
                .map(DeliveryPkgPieceVO::getMaterialConfig)
                .filter(Objects::nonNull)
                .map(MaterialConfig::getMaterialSnapshot)
                .filter(Objects::nonNull)
                .map(snapshot -> snapshot.getName())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new)).stream().collect(Collectors.toList());
    }

    public List<DeliveryPkgPiecesResponse.ProcessingFlowOption> buildProcessList(List<DeliveryPkgPieceVO> items) {
        return items.stream().filter(Objects::nonNull)
                .map(DeliveryPkgPieceVO::getProcedureFlow)
                .filter(Objects::nonNull)
                .map(flow -> flow.getNodes())
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(node -> new DeliveryPkgPiecesResponse.ProcessingFlowOption(
                        node.getNodeName(),
                        extractAccessoryName(node)
                ))
                .filter(option -> StringUtils.isNotBlank(option.getProcessName()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                option -> option.getProcessName() + "\u0000" + (option.getAccessoryName() == null ? "" : option.getAccessoryName()),
                                option -> option,
                                (left, right) -> left,
                                java.util.LinkedHashMap::new
                        ),
                        map -> new ArrayList<>(map.values())
                ));
    }

    private String extractAccessoryName(ProcedureFlowNode node) {
        if (node == null || node.getParamConfigs() == null) {
            return null;
        }
        for (Object config : node.getParamConfigs()) {
            Object param = extractFieldValue(config, "param");
            Object accessorySnapshot = extractFieldValue(param, "accessorySnapshot");
            Object name = extractFieldValue(accessorySnapshot, "name");
            if (name != null && StringUtils.isNotBlank(String.valueOf(name))) {
                return String.valueOf(name);
            }
        }
        return null;
    }

    private Object extractFieldValue(Object target, String fieldName) {
        if (target == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        if (target instanceof Map<?, ?> map) {
            return map.get(fieldName);
        }
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
    }

    public List<Double> buildSizeList(List<DeliveryPkgPieceVO> items) {
        return items.stream().map(DeliveryPkgPieceVO::getWidth).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)).stream().collect(Collectors.toList());
    }
    private int getNodeQuantity(ProductionPiece piece, String nodeId, String nodeName) {
        if (piece == null || piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
            return 0;
        }
        Integer quantity = piece.getProcedureFlow().getNodes().stream()
                .filter(Objects::nonNull)
                .filter(node -> nodeId.equals(node.getNodeId()) || nodeName.equals(node.getNodeName()))
                .map(ProcedureFlowNode::getPieceQuantity)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        return Math.max(quantity, 0);
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
        DeliveryPkgPrintResult result = executePkg(request);
        return result == null ? null : result.getTaskId();
    }

    private DeliveryPkgPrintResult executePkg(DeliveryPkgRequest request) {
        String url = "https://api.kuaidi100.com/label/order";
        String deliveryManId = request.getDeliveryManId();
        String orderId = request.getOrderId();
        String carrierId = request.getCarrierId();
        String deliverySiidId = request.getDeliverySiidId();
        String manufacturerMetaId = request.getManufacturerMetaId();
        List<ProductionPiece> productionPieces = request.getProductionPieces();
        //查询快递方式基本配置，未配置电子面单时按送货上门打包处理
        DeliveryToken deliveryToken = deliveryTokenRepository.findByCarrierIdAndManufacturerMetaId(carrierId, manufacturerMetaId);
        if (deliveryToken == null) {
            transferPiecesToPacked(productionPieces, null, carrierId, request.getCarrierName(), null, null, null);
            return null;
        }
        //查询寄件人信息
        DeliveryMan deliveryMan = deliveryManRepository.findByDeliveryManIdAndManufacturerMetaId(deliveryManId, manufacturerMetaId);
        //查询订单信息
        OrderInfo orderInfo = orderInfoService.findByOrderId(orderId);
        OrderCustomer customer = orderInfo.getCustomer();
        //组装请求参数
        Kuaidi100OrderParam kuaidi100OrderParam = Kuaidi100OrderParam.createKuaidi100OrderParam(request, deliveryToken, deliveryMan, customer);
        kuaidi100OrderParam.setSiid(resolveKuaidi100SiidForPkg(deliverySiidId, manufacturerMetaId, deliveryToken));
        String paramStr = JSON.toJSONString(kuaidi100OrderParam);
        // 5. 调用快递100 API
        String result = callPost(url, paramStr, "label.order");
        AuthOrderResponse response = JSON.parseObject(result, AuthOrderResponse.class);
        boolean isResponseSuccess = response != null
                && response.getCode() == ApiResponse.RepStatusCode.success
                && Boolean.TRUE.equals(response.getSuccess())
                && response.getData() != null;
        if (!isResponseSuccess) {
            String errorMsg = response == null || StringUtils.isBlank(response.getMessage())
                    ? "快递100电子面单下单失败"
                    : response.getMessage();
            // 快递100失败时立即抛出，失败分支不新增发货记录、不创建包裹、不更新生产零件。
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.serviceError, errorMsg);
        }

        //添加打印记录
        DeliveryRecord deliveryRecord = this.createDeliveryRecord(request);
        deliveryRecord.setSiid(resolveRequestSiid(request));
        String kuaidinum = response.getData().getKuaidinum();
        deliveryRecord.setKuaidiNum(kuaidinum);
        deliveryRecord.setTaskId(response.getData().getTaskId());
        deliveryRecord.setReprintCount(0);
        if (PRE_ORDER_SIID.equals(request.getDeliverySiidId())) {
            deliveryRecord.setConsumeStatus(PreOrderLabelConsumeStatus.PRE_ORDERED);
        }
        deliveryRecord.setDeliveryTime(new Date());
        deliveryRecord.setIsSuccess(true);
        deliveryRecordRepository.add(deliveryRecord);

        // 6. 获取快递单号并更新零件数量
        if (StringUtils.isNotBlank(kuaidinum) && productionPieces != null) {
            for (ProductionPiece requestPiece : productionPieces) {
                Integer packageQuantity = requestPiece == null ? null : requestPiece.getQuantity();
                if (requestPiece == null || StringUtils.isBlank(requestPiece.getId())
                        || packageQuantity == null || packageQuantity <= 0) {
                    continue;
                }

                // addPkg 传入的 productionPiece 只携带打包数量等临时字段，不能直接 save，
                // 否则会用不完整对象覆盖 MongoDB 原记录。这里重新读取完整生产件后再更新。
                ProductionPiece productionPiece = productionPieceService.findById(requestPiece.getId());
                if (productionPiece == null || productionPiece.getProcedureFlow() == null
                        || productionPiece.getProcedureFlow().getNodes() == null) {
                    continue;
                }

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
                    Integer pendingQuantity = pendingPackingNode.getPieceQuantity() != null ? pendingPackingNode.getPieceQuantity() : 0;
                    pendingPackingNode.setPieceQuantity(pendingQuantity - packageQuantity);

                    if (pendingPackingNode.getPieceQuantity() <= 0) {
                        pendingPackingNode.setNodeStatus(NodeStatus.COMPLETED);
                    }

                    Integer packedQuantity = packedNode.getPieceQuantity() != null ? packedNode.getPieceQuantity() : 0;
                    packedNode.setPieceQuantity(packedQuantity + packageQuantity);
                    packedNode.setNodeStatus(NodeStatus.ACTIVE);
                    DeliveryPkgInfo deliveryPkgInfo = new DeliveryPkgInfo();
                    deliveryPkgInfo.setCarrierId(carrierId);
                    deliveryPkgInfo.setKuaidiNum(kuaidinum);
                    deliveryPkgInfo.setQuantity(packageQuantity);
                    List<DeliveryPkgInfo> pkgInfos = productionPiece.getDeliveryPkgInfos();
                    if (pkgInfos == null) {
                        pkgInfos = new ArrayList<>();
                    }
                    pkgInfos.add(deliveryPkgInfo);
                    productionPiece.setDeliveryPkgInfos(pkgInfos);
                    productionPieceService.updateProductionPiece(productionPiece);
                }
            }
        }

        return new DeliveryPkgPrintResult(response.getData().getTaskId(), response.getData().getKuaidinum());
    }

    public DeliveryPkg addPkg(DeliveryPkgAddRequest request) {
        if (request == null || request.getPieces() == null || request.getPieces().isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "打包零件不能为空");
        }
        String orderId = null;
        String carrierId = null;
        String carrierName = null;
        String presetType = null;
        List<ProductionPiece> selectedPieces = new ArrayList<>();
        Map<String, Integer> packageQuantityMap = new HashMap<>();
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
                presetType = pieceVO.getLogisticsCarrierInfo().getPresetType();
            } else if (!Objects.equals(orderId, pieceVO.getOrderId())
                    || !Objects.equals(carrierId, pieceVO.getLogisticsCarrierInfo().getCarrierId())
                    || !Objects.equals(presetType, pieceVO.getLogisticsCarrierInfo().getPresetType())) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "仅支持同一订单且同一物流方式一起打包");
            }

            ProductionPiece sourcePiece = productionPieceService.findByProductionPieceId(pieceVO.getProductionPieceId());
            if (sourcePiece == null) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "存在无效的生产零件");
            }
            int pendingQty = getNodeQuantity(sourcePiece, NODE_ID_PENDING_PACKING, NODE_NAME_PENDING_PACKING);
            if (item.getQuantity() > pendingQty) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams,
                        "零件[" + pieceVO.getProductionPieceId() + "]打包数量超过待打包数量");
            }
            selectedPieces.add(sourcePiece);
            packageQuantityMap.put(sourcePiece.getId(), item.getQuantity());
        }

        // Channel is the first routing decision. Gather-platform orders must never fall
        // through to the Kuaidi100 path based on printer/token heuristics.
        OrderInfo orderInfo = orderInfoService.findByOrderId(orderId);
        if (orderInfo != null && orderInfo.getChannel() != null
                && orderInfo.getChannel().getType() == com.mes.domain.order.enums.OrderChannelType.GATHER_PLATFORM) {
            return packGatherPlatformOrder(request, orderInfo, selectedPieces, packageQuantityMap,
                    carrierId, carrierName, presetType);
        }

        boolean useCustomPackagingFlow = "CUSTOM".equalsIgnoreCase(presetType)
                || StringUtils.isBlank(request.getDeliveryManId())
                || StringUtils.isBlank(request.getDeliverySiidId());

        DeliveryToken deliveryToken = null;
        if (!useCustomPackagingFlow) {
            deliveryToken = deliveryTokenRepository.findByCarrierIdAndManufacturerMetaId(carrierId, request.getManufacturerMetaId());
            useCustomPackagingFlow = deliveryToken == null;
        }

        String actualPresetType = useCustomPackagingFlow ? "CUSTOM" : presetType;

        if (!useCustomPackagingFlow) {
            if (orderInfo != null && StringUtils.isNotBlank(orderInfo.getKuaidiNum())) {
                DeliveryRecord preOrderRecord = deliveryRecordRepository.findByKuaidiNum(orderInfo.getKuaidiNum());
                if (preOrderRecord != null && StringUtils.isNotBlank(preOrderRecord.getTaskId())) {
                    String reprintSiid = resolveKuaidi100SiidForAddPkg(request, deliveryToken);
                    if (StringUtils.isBlank(reprintSiid)) {
                        throw new BusinessNotAllowException(ApiResponse.RepStatusCode.serviceError, "快递100云打印设备不能为空");
                    }
                    if (preOrderRecord.getConsumeStatus() != PreOrderLabelConsumeStatus.CONSUMED) {
                        DeliveryRecord claimed = deliveryRecordRepository.claimForPrinting(preOrderRecord.getId(),
                                preOrderRecord.getConsumeStatus(), reprintSiid);
                        if (claimed == null) {
                            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.serviceError, "面单正在打印，请勿重复提交");
                        }
                        AuthOrderResponse reprintResponse;
                        try {
                            reprintResponse = reprintKuaidi100Label(claimed.getTaskId(), reprintSiid);
                        } catch (RuntimeException ex) {
                            claimed.setConsumeStatus(PreOrderLabelConsumeStatus.PRINT_FAILED);
                            deliveryRecordRepository.update(claimed);
                            throw ex;
                        }
                        if (reprintResponse == null || !Boolean.TRUE.equals(reprintResponse.getSuccess())) {
                            claimed.setConsumeStatus(PreOrderLabelConsumeStatus.PRINT_FAILED);
                            claimed.setErrorMsg(reprintResponse == null ? "快递100预下单面单复打失败" : reprintResponse.getMessage());
                            deliveryRecordRepository.update(claimed);
                            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.serviceError, claimed.getErrorMsg());
                        }
                        claimed.setConsumeStatus(PreOrderLabelConsumeStatus.CONSUMED);
                        claimed.setPackedAt(new Date());
                        claimed.setReprintCount(claimed.getReprintCount() == null ? 1 : claimed.getReprintCount() + 1);
                        if (StringUtils.isBlank(claimed.getDeliveryPkgId())) claimed.setDeliveryPkgId("DP" + claimed.getId());
                        deliveryRecordRepository.update(claimed); // persist external success before local recovery work
                        preOrderRecord = claimed;
                    }
                    DeliveryPkg deliveryPkg = findOrCreateConsumedPkg(preOrderRecord, request, orderId, carrierId,
                            carrierName, actualPresetType, orderInfo.getKuaidiNum());
                    transferPiecesToPacked(selectedPieces, packageQuantityMap, carrierId, carrierName,
                            request.getRouteId(), request.getRouteNodeId(), orderInfo.getKuaidiNum());
                    orderInfo.setKuaidiNum(null);
                    orderInfoService.updateOrder(orderInfo);
                    return deliveryPkg;
                }
            }
        }

        if (useCustomPackagingFlow) {
            DeliveryPkg deliveryPkg = createAndSaveDeliveryPkg(request, orderId, carrierId, carrierName, actualPresetType, null);
            transferPiecesToPacked(selectedPieces, packageQuantityMap, carrierId, carrierName, request.getRouteId(), request.getRouteNodeId(), null);
            return deliveryPkg;
        }

        DeliveryPkgRequest toPkgRequest = new DeliveryPkgRequest();
        List<ProductionPiece> pkgRequestPieces = new ArrayList<>();
        for (ProductionPiece productionPiece : selectedPieces) {
            ProductionPiece pkgPiece = new ProductionPiece();
            pkgPiece.setId(productionPiece.getId());
            pkgPiece.setProcedureFlow(productionPiece.getProcedureFlow());
            pkgPiece.setDeliveryPkgInfos(productionPiece.getDeliveryPkgInfos());
            pkgPiece.setQuantity(packageQuantityMap.getOrDefault(productionPiece.getId(), 0));
            pkgRequestPieces.add(pkgPiece);
        }
        toPkgRequest.setProductionPieces(pkgRequestPieces);
        toPkgRequest.setOrderId(orderId);
        toPkgRequest.setCarrierId(carrierId);
        toPkgRequest.setCarrierName(carrierName);
        toPkgRequest.setDeliveryManId(request.getDeliveryManId());
        toPkgRequest.setDeliverySiidId(request.getDeliverySiidId());
        toPkgRequest.setManufacturerMetaId(request.getManufacturerMetaId());
        toPkgRequest.setRemark(buildDeliveryPkgRemarks(orderId, actualPresetType, null, request.getPieces(), null));
        // 调用配送系统打包，打印面单；失败时 toPkg 会先保存失败记录再抛异常，且不会创建包裹或更新零件
        DeliveryPkgPrintResult printResult = this.executePkg(toPkgRequest);
        String taskId = printResult == null ? null : printResult.getTaskId();
        String kuaidiNum = printResult == null ? null : printResult.getKuaidiNum();
        DeliveryPkg deliveryPkg = createAndSaveDeliveryPkg(request, orderId, carrierId, carrierName, actualPresetType, kuaidiNum);
        if (StringUtils.isNotBlank(taskId)) {
            deliveryPkg.setDeliveryPkgCode(taskId);
            deliveryPkgService.updateDeliveryPkg(deliveryPkg);
        }
        java.util.Set<String> touchedOrderItemIds = selectedPieces.stream()
                .map(ProductionPiece::getOrderItemId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        refreshPackagingCompletionStatus(touchedOrderItemIds);
        return deliveryPkg;
    }

    private DeliveryPkg packGatherPlatformOrder(DeliveryPkgAddRequest request, OrderInfo orderInfo,
                                                        List<ProductionPiece> pieces, Map<String, Integer> quantities,
                                                        String carrierId, String carrierName, String presetType) {
        String channelOrderId = orderInfo.getChannel().getOrderId();
        WdtLabelRecord record = wdtLabelRecordRepository.findForOrder(orderInfo.getOrderId(), channelOrderId,
                orderInfo.getKuaidiNum());
        if (record == null) {
            // A gather-platform order without a pre-ordered WDT label is still packable,
            // but must return the same locally printable label as the custom flow.
            DeliveryPkg customPkg = createAndSaveDeliveryPkg(request, orderInfo.getOrderId(), carrierId,
                    carrierName, "CUSTOM", null);
            transferPiecesToPacked(pieces, quantities, carrierId, carrierName, request.getRouteId(),
                    request.getRouteNodeId(), null);
            return customPkg;
        }

        String stablePkgId = StringUtils.isBlank(record.getDeliveryPkgId()) ? "DPWDT" + record.getId()
                : record.getDeliveryPkgId();
        List<DeliveryPkg> existing = deliveryPkgService.findByDeliveryPkgId(stablePkgId);
        DeliveryPkg pkg = existing.isEmpty()
                ? createAndSaveDeliveryPkg(request, orderInfo.getOrderId(), carrierId, carrierName, presetType,
                    record.getLogisticsOrderId(), stablePkgId) : existing.get(0);
        transferPiecesToPacked(pieces, quantities, carrierId, carrierName, request.getRouteId(),
                request.getRouteNodeId(), record.getLogisticsOrderId());
        record.setDeliveryPkgId(stablePkgId);
        record.setConsumeStatus(PreOrderLabelConsumeStatus.CONSUMED);
        wdtLabelRecordRepository.update(record);
        pkg.setWdtLabelRecord(record);
        orderInfo.setKuaidiNum(null);
        orderInfoService.updateOrder(orderInfo);
        return pkg;
    }

    private DeliveryPkg findOrCreateConsumedPkg(DeliveryRecord record, DeliveryPkgAddRequest request,
                                                     String orderId, String carrierId, String carrierName,
                                                     String presetType, String kuaidiNum) {
        if (StringUtils.isNotBlank(record.getDeliveryPkgId())) {
            List<DeliveryPkg> existing = deliveryPkgService.findByDeliveryPkgId(record.getDeliveryPkgId());
            if (!existing.isEmpty()) return existing.get(0);
        }
        DeliveryPkg pkg = createAndSaveDeliveryPkg(request, orderId, carrierId, carrierName, presetType,
                kuaidiNum, record.getDeliveryPkgId());
        pkg.setDeliveryPkgCode(record.getTaskId());
        return deliveryPkgService.updateDeliveryPkg(pkg);
    }

    private void transferPiecesToPacked(List<ProductionPiece> productionPieces, Map<String, Integer> packageQuantityMap,
                                        String carrierId, String carrierName, String routeId, String routeNodeId, String kuaidiNum) {
        if (productionPieces == null || productionPieces.isEmpty()) {
            return;
        }
        java.util.Set<String> touchedOrderItemIds = new java.util.HashSet<>();
        if (productionPieces == null) {
            productionPieces = new ArrayList<>();
        }
        for (ProductionPiece productionPiece : productionPieces) {
            if (productionPiece == null || productionPiece.getProcedureFlow() == null
                    || productionPiece.getProcedureFlow().getNodes() == null) {
                continue;
            }
            List<ProcedureFlowNode> nodes = productionPiece.getProcedureFlow().getNodes();
            ProcedureFlowNode pendingPackingNode = null;
            ProcedureFlowNode packedNode = null;
            for (ProcedureFlowNode node : nodes) {
                if (NODE_NAME_PENDING_PACKING.equals(node.getNodeName())) {
                    pendingPackingNode = node;
                } else if (NODE_NAME_PACKED.equals(node.getNodeName())) {
                    packedNode = node;
                }
            }

            if (pendingPackingNode == null || packedNode == null) {
                continue;
            }

            Integer quantity = packageQuantityMap == null
                    ? productionPiece.getQuantity()
                    : packageQuantityMap.getOrDefault(productionPiece.getId(), 0);
            if (quantity == null || quantity <= 0) {
                continue;
            }

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
            DeliveryPkgInfo deliveryPkgInfo = pkgInfos.stream()
                    .filter(Objects::nonNull)
                    .filter(info -> Objects.equals(carrierId, info.getCarrierId())
                            && Objects.equals(kuaidiNum, info.getKuaidiNum()))
                    .findFirst().orElse(null);
            if (deliveryPkgInfo == null) {
                deliveryPkgInfo = new DeliveryPkgInfo();
                deliveryPkgInfo.setCarrierId(carrierId);
                deliveryPkgInfo.setKuaidiNum(kuaidiNum);
                pkgInfos.add(deliveryPkgInfo);
            }
            if (StringUtils.isNotBlank(carrierName)) {
                String routeCarrierSuffix = (StringUtils.isBlank(routeId) || StringUtils.isBlank(routeNodeId))
                        ? "(未自定义路线)"
                        : "(" + routeId + "/" + routeNodeId + ")";
                deliveryPkgInfo.setCarrierName(carrierName + routeCarrierSuffix);
            }
            int alreadyPacked = deliveryPkgInfo.getQuantity() == null ? 0 : deliveryPkgInfo.getQuantity();
            deliveryPkgInfo.setQuantity(alreadyPacked + quantity);
            productionPiece.setDeliveryPkgInfos(pkgInfos);
            updatePiecePackagingStateAfterTransfer(productionPiece, touchedOrderItemIds);
        }
        refreshPackagingCompletionStatus(touchedOrderItemIds);
    }

    private void updatePiecePackagingStateAfterTransfer(ProductionPiece piece, java.util.Set<String> touchedOrderItemIds) {
        if (piece == null) {
            return;
        }
        if (touchedOrderItemIds != null && StringUtils.isNotBlank(piece.getOrderItemId())) {
            touchedOrderItemIds.add(piece.getOrderItemId());
        }
        boolean pieceFullyPacked = isPieceFullyPacked(piece);
        boolean flag = !TypesettingStatus.COMPLETED.getCode().equals(piece.getStatus());
        if (pieceFullyPacked && flag) {
            piece.setStatus(TypesettingStatus.COMPLETED.getCode());
        }
        productionPieceService.updateProductionPiece(piece);
    }


    private void refreshPackagingCompletionStatus(java.util.Set<String> touchedOrderItemIds) {
        if (touchedOrderItemIds == null || touchedOrderItemIds.isEmpty()) {
            return;
        }
        for (String orderItemId : touchedOrderItemIds) {
            if (StringUtils.isBlank(orderItemId)) {
                continue;
            }
            List<ProductionPiece> orderItemPieces = new ArrayList<>();
            int current = 1;
            while (true) {
                List<ProductionPiece> page = productionPieceService.findProductionPiecesByOrderItemId(orderItemId, current, 100);
                if (page == null || page.isEmpty()) {
                    break;
                }
                orderItemPieces.addAll(page);
                if (page.size() < 100) {
                    break;
                }
                current++;
            }

            OrderItem orderItem = orderItemService.findByOrderItemId(orderItemId);
            Integer requiredPackedQuantity = getOrderItemQuantity(orderItem);
            boolean allPacked = requiredPackedQuantity != null && !orderItemPieces.isEmpty() && orderItemPieces.stream()
                    .allMatch(piece -> isPieceFullyPacked(piece, requiredPackedQuantity));
            if (!allPacked) {
                continue;
            }

            for (ProductionPiece piece : orderItemPieces) {
                boolean changed = false;
                if (!TypesettingStatus.COMPLETED.getCode().equals(piece.getStatus())) {
                    piece.setStatus(TypesettingStatus.COMPLETED.getCode());
                    changed = true;
                }
                if (Boolean.TRUE.equals(piece.getIsUrgent())) {
                    piece.setIsUrgent(false);
                    changed = true;
                }
                if (changed) {
                    productionPieceService.updateProductionPiece(piece);
                }
            }

            boolean allPiecesCompleted = !orderItemPieces.isEmpty() && orderItemPieces.stream()
                    .allMatch(piece -> TypesettingStatus.COMPLETED.getCode().equals(piece.getStatus()));
            if (!allPiecesCompleted) {
                continue;
            }

            if (orderItem != null) {
                boolean changed = false;
                if (orderItem.getStatus() != OrderStatus.PACKAGED) {
                    orderItem.setStatus(OrderStatus.PACKAGED);
                    changed = true;
                }
                if (Boolean.TRUE.equals(orderItem.getIsUrgent())) {
                    orderItem.setIsUrgent(false);
                    changed = true;
                }
                if (changed) {
                    orderItemService.updateOrderItem(orderItem);
                }
            }
        }
    }

    private boolean isPieceFullyPacked(ProductionPiece piece) {
        Integer requiredPackedQuantity = getPieceRequiredPackedQuantity(piece);
        return isPieceFullyPacked(piece, requiredPackedQuantity);
    }

    private boolean isPieceFullyPacked(ProductionPiece piece, Integer requiredPackedQuantity) {
        if (piece == null || requiredPackedQuantity == null || requiredPackedQuantity <= 0) {
            return false;
        }
        int packedQty = getNodeQuantity(piece, NODE_ID_PACKED, NODE_NAME_PACKED);
        return packedQty >= requiredPackedQuantity;
    }

    private Integer getPieceRequiredPackedQuantity(ProductionPiece piece) {
        if (piece == null || StringUtils.isBlank(piece.getOrderItemId())) {
            return piece == null ? null : piece.getQuantity();
        }
        return getOrderItemQuantity(orderItemService.findByOrderItemId(piece.getOrderItemId()), piece.getQuantity());
    }

    private Integer getOrderItemQuantity(OrderItem orderItem) {
        return getOrderItemQuantity(orderItem, null);
    }

    private Integer getOrderItemQuantity(OrderItem orderItem, Integer fallbackQuantity) {
        if (orderItem != null && orderItem.getQuantity() != null && orderItem.getQuantity() > 0) {
            return orderItem.getQuantity();
        }
        return fallbackQuantity;
    }

    private void revertPackagedOrderItems(java.util.Set<String> touchedOrderItemIds) {
        if (touchedOrderItemIds == null || touchedOrderItemIds.isEmpty()) {
            return;
        }
        for (String orderItemId : touchedOrderItemIds) {
            if (StringUtils.isBlank(orderItemId)) {
                continue;
            }
            OrderItem orderItem = orderItemService.findByOrderItemId(orderItemId);
            if (orderItem != null && orderItem.getStatus() == OrderStatus.PACKAGED) {
                orderItem.setStatus(OrderStatus.IN_PRODUCTION);
                orderItemService.updateOrderItem(orderItem);
            }
        }
    }


    private DeliveryPkg createAndSaveDeliveryPkg(DeliveryPkgAddRequest request, String orderId, String carrierId, String carrierName, String presetType, String kuaidiNum) {
        return createAndSaveDeliveryPkg(request, orderId, carrierId, carrierName, presetType, kuaidiNum, null);
    }

    private DeliveryPkg createAndSaveDeliveryPkg(DeliveryPkgAddRequest request, String orderId, String carrierId, String carrierName, String presetType, String kuaidiNum, String stablePkgId) {
        DeliveryPkg deliveryPkg = new DeliveryPkg();
        String deliveryPkgId = StringUtils.isBlank(stablePkgId) ? IdGenerator.generateId("DP") : stablePkgId;
        deliveryPkg.setDeliveryPkgId(deliveryPkgId);
        deliveryPkg.setDeliveryPkgCode(deliveryPkgId);
        deliveryPkg.setOrderId(orderId);
        deliveryPkg.setCarrierId(carrierId);
        deliveryPkg.setCarrierName(carrierName);
        deliveryPkg.setDeliveryWay(presetType);
        deliveryPkg.setPresetType(presetType);
        deliveryPkg.setDeliveryManId(request.getDeliveryManId());
        deliveryPkg.setDeliverySiidId(request.getDeliverySiidId());
        deliveryPkg.setSiid(resolveDeliveryPkgDefaultSiid(request));
        deliveryPkg.setManufacturerMetaId(request.getManufacturerMetaId());
        deliveryPkg.setRouteId(request.getRouteId());
        deliveryPkg.setRouteNodeId(request.getRouteNodeId());
        deliveryPkg.setKuaidiNum(kuaidiNum);
        OrderInfo orderInfo = StringUtils.isBlank(orderId) ? null : orderInfoService.findByOrderId(orderId);
        if (orderInfo != null) {
            deliveryPkg.setOrgInfo(orderInfo.getOrgInfo());
        }
        deliveryPkg.setRemarks(buildDeliveryPkgRemarks(orderId, presetType, kuaidiNum, request.getPieces(), orderInfo));

        List<com.mes.domain.delivery.deliveryPkg.vo.DeliveryPkgItem> pkgItems = new ArrayList<>();
        for (DeliveryPkgAddRequest.DeliveryPkgPieceItem item : request.getPieces()) {
            com.mes.domain.delivery.deliveryPkg.vo.DeliveryPkgItem pkgItem = new com.mes.domain.delivery.deliveryPkg.vo.DeliveryPkgItem();
            pkgItem.setOrderItemId(item.getPiece().getOrderItemId());
            pkgItem.setProductionPieceId(Collections.singletonList(item.getPiece().getProductionPieceId()));
            pkgItem.setQuantity(item.getQuantity());
            if (item.getPiece().getPreviewUrl() != null) {
                pkgItem.setPreviewUrl(item.getPiece().getPreviewUrl());
            }
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


    private String buildDeliveryPkgRemarks(String orderId, String presetType, String kuaidiNum,
                                           List<DeliveryPkgAddRequest.DeliveryPkgPieceItem> pieces, OrderInfo orderInfo) {
        List<String> remarkParts = new ArrayList<>();
        if (StringUtils.isNotBlank(orderId)) {
            remarkParts.add("订单:" + orderId);
        }
        addProductionImageFileNames(remarkParts, pieces);

        if (!"CUSTOM".equalsIgnoreCase(presetType)) {
            if (orderInfo == null && StringUtils.isNotBlank(orderId)) {
                orderInfo = orderInfoService.findByOrderId(orderId);
            }
            if (orderInfo != null && StringUtils.isNotBlank(orderInfo.getRemark())) {
                remarkParts.add(orderInfo.getRemark());
            }
            return String.join("\n", remarkParts);
        }

        if (orderInfo == null && StringUtils.isNotBlank(orderId)) {
            orderInfo = orderInfoService.findByOrderId(orderId);
        }
        if (orderInfo != null && StringUtils.isNotBlank(orderInfo.getRemark())) {
            remarkParts.add(orderInfo.getRemark());
        }
        return String.join("\n", remarkParts);
    }


    private void addProductionImageFileNames(List<String> remarkParts, List<DeliveryPkgAddRequest.DeliveryPkgPieceItem> pieces) {
        if (remarkParts == null || pieces == null || pieces.isEmpty()) {
            return;
        }
        List<String> imageFileNames = pieces.stream()
                .filter(Objects::nonNull)
                .map(DeliveryPkgAddRequest.DeliveryPkgPieceItem::getPiece)
                .filter(Objects::nonNull)
                .map(DeliveryPkgPieceVO::getOrderItemId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .map(orderItemService::findByOrderItemId)
                .filter(Objects::nonNull)
                .map(OrderItem::getProductionImgFile)
                .map(this::getImageFileName)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (!imageFileNames.isEmpty()) {
            remarkParts.add("文件:" + String.join(",", imageFileNames));
        }
    }


    private String getImageFileName(Object imageFile) {
        if (imageFile == null) {
            return null;
        }
        try {
            Object value = imageFile.getClass().getMethod("getName").invoke(imageFile);
            return value == null ? null : String.valueOf(value);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static class DeliveryPkgPrintResult {
        private final String taskId;
        private final String kuaidiNum;

        public DeliveryPkgPrintResult(String taskId, String kuaidiNum) {
            this.taskId = taskId;
            this.kuaidiNum = kuaidiNum;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getKuaidiNum() {
            return kuaidiNum;
        }
    }

    public DeliveryPkg findByDeliveryPkgId(String deliveryPkgId) {
        if (StringUtils.isBlank(deliveryPkgId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "deliveryPkgId不能为空");
        }
        List<DeliveryPkg> deliveryPkgs = deliveryPkgService.findByDeliveryPkgId(deliveryPkgId);
        return deliveryPkgs.stream()
                .filter(pkg -> deliveryPkgId.equals(pkg.getDeliveryPkgId()))
                .findFirst()
                .orElseThrow(() -> new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "包裹不存在"));
    }


    public AuthOrderResponse reprintKuaidi100Label(DeliveryPkgActionRequest request) {
        if (request == null || StringUtils.isBlank(request.getDeliveryPkgId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "deliveryPkgId不能为空");
        }
        DeliveryPkg deliveryPkg = findByDeliveryPkgId(request.getDeliveryPkgId().trim());
        String taskId = deliveryPkg.getDeliveryPkgCode();
        if (StringUtils.isBlank(taskId)) {
            return buildKuaidi100ReprintFailure("请先申请打印再进行复打");
        }

        DeliveryRecord deliveryRecord = deliveryRecordRepository.findByTaskId(taskId);
        if (deliveryRecord == null) {
            return buildKuaidi100ReprintFailure("请先申请打印再进行复打");
        }

        String siid = request.getSiid();
        if (StringUtils.isBlank(siid)) {
            siid = deliveryPkg.getSiid();
        }
        if (StringUtils.isBlank(siid)) {
            siid = resolveKuaidi100Siid(deliveryRecord.getDeliverySiidId(), deliveryPkg.getManufacturerMetaId());
        }
        if (StringUtils.isBlank(siid)) {
            siid = resolveKuaidi100Siid(deliveryPkg.getDeliverySiidId(), deliveryPkg.getManufacturerMetaId());
        }
        if (StringUtils.isBlank(siid)) {
            return buildKuaidi100ReprintFailure("云打印设备不能为空");
        }

        AuthOrderResponse response = reprintKuaidi100Label(taskId, siid);
        if (response != null && Boolean.TRUE.equals(response.getSuccess()) && StringUtils.isNotBlank(deliveryRecord.getId())) {
            deliveryRecord.setReprintCount(deliveryRecord.getReprintCount() == null ? 1 : deliveryRecord.getReprintCount() + 1);
            deliveryRecordRepository.update(deliveryRecord);
        }
        return response;
    }


    private AuthOrderResponse reprintKuaidi100Label(String taskId, String siid) {
        Map<String, String> fdParam = new HashMap<>();
        fdParam.put("taskId", taskId);
        fdParam.put("siid", siid);
        String result = callPost("https://api.kuaidi100.com/label/order", JSON.toJSONString(fdParam), "printOld");
        return JSON.parseObject(result, AuthOrderResponse.class);
    }

    public DeliveryPkgPrintResult preOrderKuaidi100Label(OrderInfo orderInfo) {
        return preOrderKuaidi100Label(orderInfo, null);
    }

    public DeliveryPkgPrintResult preOrderKuaidi100Label(OrderInfo orderInfo, List<OrderItem> orderItems) {
        if (orderInfo == null || orderInfo.getLogisticsCarrierInfo() == null
                || StringUtils.isBlank(orderInfo.getManufacturerId())) {
            return null;
        }
        if ("CUSTOM".equalsIgnoreCase(orderInfo.getLogisticsCarrierInfo().getPresetType())) {
            return null;
        }
        String carrierId = orderInfo.getLogisticsCarrierInfo().getCarrierId();
        if (StringUtils.isBlank(carrierId)) {
            return null;
        }
        DeliveryMan deliveryMan = resolveDefaultDeliveryMan(orderInfo.getManufacturerId());
        DeliveryToken deliveryToken = deliveryTokenRepository.findByCarrierIdAndManufacturerMetaId(carrierId, orderInfo.getManufacturerId());
        if (deliveryMan == null || deliveryToken == null) {
            return null;
        }
        DeliveryPkgRequest request = new DeliveryPkgRequest();
        request.setOrderId(orderInfo.getOrderId());
        request.setCarrierId(carrierId);
        request.setCarrierName(orderInfo.getLogisticsCarrierInfo().getCarrierName());
        request.setDeliveryManId(deliveryMan.getDeliveryManId());
        request.setDeliverySiidId(PRE_ORDER_SIID);
        request.setManufacturerMetaId(orderInfo.getManufacturerId());
        request.setRemark(buildPreOrderKuaidi100Remark(orderInfo, orderItems));
        request.setProductionPieces(new ArrayList<>());
        return executePkg(request);
    }

    private String buildPreOrderKuaidi100Remark(OrderInfo orderInfo, List<OrderItem> orderItems) {
        List<String> remarkParts = new ArrayList<>();
        if (orderInfo != null && StringUtils.isNotBlank(orderInfo.getOrderId())) {
            remarkParts.add("订单:" + orderInfo.getOrderId());
        }
        addOrderItemProductionImageFileNames(remarkParts, orderItems);
        if (orderInfo != null && StringUtils.isNotBlank(orderInfo.getRemark())) {
            remarkParts.add(orderInfo.getRemark());
        }
        return String.join("\n", remarkParts);
    }

    private void addOrderItemProductionImageFileNames(List<String> remarkParts, List<OrderItem> orderItems) {
        if (remarkParts == null || orderItems == null || orderItems.isEmpty()) {
            return;
        }
        List<String> imageFileNames = orderItems.stream()
                .filter(Objects::nonNull)
                .map(OrderItem::getProductionImgFile)
                .map(this::getImageFileName)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (!imageFileNames.isEmpty()) {
            remarkParts.add("文件:" + String.join(",", imageFileNames));
        }
    }

    private DeliveryMan resolveDefaultDeliveryMan(String manufacturerMetaId) {
        List<DeliveryMan> deliveryMen = deliveryManRepository.findByManufacturerMetaId(manufacturerMetaId);
        if (deliveryMen == null || deliveryMen.isEmpty()) {
            return null;
        }
        return deliveryMen.stream().filter(man -> Boolean.TRUE.equals(man.getIsDefault())).findFirst().orElse(deliveryMen.get(0));
    }

    private String resolveRequestSiid(DeliveryPkgRequest request) {
        if (request == null) {
            return null;
        }
        if (PRE_ORDER_SIID.equals(request.getDeliverySiidId())) {
            return PRE_ORDER_SIID;
        }
        return resolveKuaidi100Siid(request.getDeliverySiidId(), request.getManufacturerMetaId());
    }

    private String resolveDeliveryPkgDefaultSiid(DeliveryPkgAddRequest request) {
        if (request == null) {
            return null;
        }
        if (StringUtils.isNotBlank(request.getDeliverySiidId())) {
            return resolveKuaidi100Siid(request.getDeliverySiidId(), request.getManufacturerMetaId());
        }
        if (StringUtils.isNotBlank(request.getSiid())) {
            return request.getSiid().trim();
        }
        return null;
    }

    private String resolveKuaidi100SiidForAddPkg(DeliveryPkgAddRequest request, DeliveryToken deliveryToken) {
        if (request != null && StringUtils.isNotBlank(request.getDeliverySiidId())) {
            return resolveKuaidi100Siid(request.getDeliverySiidId(), request.getManufacturerMetaId());
        }
        return deliveryToken == null ? null : deliveryToken.getSiid();
    }

    private String resolveKuaidi100SiidForPkg(String deliverySiidId, String manufacturerMetaId, DeliveryToken deliveryToken) {
        if (StringUtils.isNotBlank(deliverySiidId)) {
            return resolveKuaidi100Siid(deliverySiidId, manufacturerMetaId);
        }
        return deliveryToken == null ? null : deliveryToken.getSiid();
    }

    private String resolveKuaidi100Siid(String deliverySiidId, String manufacturerMetaId) {
        if (StringUtils.isBlank(deliverySiidId)) {
            return null;
        }
        DeliverySiid deliverySiid = deliverySiidRepository.findByDeliverySiidIdAndManufacturerMetaId(deliverySiidId, manufacturerMetaId);
        if (deliverySiid == null) {
            return deliverySiidId;
        }
        return StringUtils.isBlank(deliverySiid.getSiid()) ? deliverySiidId : deliverySiid.getSiid();
    }

    private AuthOrderResponse buildKuaidi100ReprintFailure(String message) {
        AuthOrderResponse response = new AuthOrderResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setCode(9999);
        return response;
    }

    public void releasePkg(String deliveryPkgId) {
        DeliveryPkg deliveryPkg = findByDeliveryPkgId(deliveryPkgId);
        java.util.Set<String> touchedOrderItemIds = new java.util.HashSet<>();
        if (deliveryPkg.getDeliveryPkgItems() != null && !deliveryPkg.getDeliveryPkgItems().isEmpty()) {
            for (com.mes.domain.delivery.deliveryPkg.vo.DeliveryPkgItem pkgItem : deliveryPkg.getDeliveryPkgItems()) {
                if (pkgItem.getProductionPieceId() == null) {
                    continue;
                }
                for (String pieceId : pkgItem.getProductionPieceId()) {
                    ProductionPiece piece = productionPieceService.findByProductionPieceId(pieceId);
                    if (piece == null || piece.getProcedureFlow() == null || piece.getProcedureFlow().getNodes() == null) {
                        continue;
                    }
                    Optional<ProcedureFlowNode> pendingNodeOpt = piece.getProcedureFlow().getNodes().stream()
                            .filter(n -> NODE_NAME_PENDING_PACKING.equals(n.getNodeName()))
                            .findFirst();
                    Optional<ProcedureFlowNode> packedNodeOpt = piece.getProcedureFlow().getNodes().stream()
                            .filter(n -> NODE_NAME_PACKED.equals(n.getNodeName()))
                            .findFirst();
                    if (pendingNodeOpt.isEmpty() || packedNodeOpt.isEmpty()) {
                        continue;
                    }
                    ProcedureFlowNode pendingNode = pendingNodeOpt.get();
                    ProcedureFlowNode packedNode = packedNodeOpt.get();
                    int releaseQuantity = pkgItem.getQuantity() == null ? 0 : pkgItem.getQuantity();
                    if (releaseQuantity <= 0) {
                        continue;
                    }
                    int packedQty = packedNode.getPieceQuantity() == null ? 0 : packedNode.getPieceQuantity();
                    int pendingQty = pendingNode.getPieceQuantity() == null ? 0 : pendingNode.getPieceQuantity();
                    int actualReleaseQuantity = Math.min(releaseQuantity, packedQty);
                    if (actualReleaseQuantity <= 0) {
                        continue;
                    }
                    packedNode.setPieceQuantity(packedQty - actualReleaseQuantity);
                    pendingNode.setPieceQuantity(pendingQty + actualReleaseQuantity);
                    pendingNode.setNodeStatus(NodeStatus.ACTIVE);
                    if (packedNode.getPieceQuantity() <= 0) {
                        packedNode.setNodeStatus(NodeStatus.PENDING);
                    }
                    if (TypesettingStatus.COMPLETED.getCode().equals(piece.getStatus())) {
                        piece.setStatus(TypesettingStatus.PRINTING.getCode());
                    }
                    if (StringUtils.isNotBlank(piece.getOrderItemId())) {
                        touchedOrderItemIds.add(piece.getOrderItemId());
                    }
                    productionPieceService.updateProductionPiece(piece);
                }
            }
        }

        revertPackagedOrderItems(touchedOrderItemIds);
        deliveryPkgService.deleteDeliveryPkg(deliveryPkg.getId());
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
        record.setCarrierName(request.getCarrierName());
        record.setDeliveryManId(request.getDeliveryManId());
        record.setDeliverySiidId(request.getDeliverySiidId());
        record.setUserId(request.getUserId());
        record.setManufacturerMetaId(request.getManufacturerMetaId());
        record.setRemark(request.getRemark());
        List<ProductionPiece> productionPieces = request.getProductionPieces();
        ArrayList<DeliveryRecord.ProductionPieceDTO> productionPieceDTOs = new ArrayList<DeliveryRecord.ProductionPieceDTO>();
        if (productionPieces == null) {
            productionPieces = new ArrayList<>();
        }
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
