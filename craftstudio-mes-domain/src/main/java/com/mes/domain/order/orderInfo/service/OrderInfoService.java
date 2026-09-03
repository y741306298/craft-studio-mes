package com.mes.domain.order.orderInfo.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecordStatus;
import com.mes.domain.delivery.deliveryRoute.vo.AddressRecognitionConsignee;
import com.mes.domain.delivery.deliveryRoute.repository.AddressRecognitionRecordRepository;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.order.enums.OrderChannelType;
import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.mes.domain.order.orderInfo.repository.OrderInfoRepository;
import com.mes.domain.order.orderInfo.repository.OrderItemRepository;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.file.vo.File;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.entity.MTOProductSpec;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.ProcessParamConfig;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.params.FileAssetParam;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrderInfoService {

    @Autowired
    private OrderInfoRepository orderInfoRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderPreprocessingService orderPreprocessingService;

    @Autowired
    private AddressRecognitionRecordRepository addressRecognitionRecordRepository;

    /**
     * 根据 ID 获取订单信息
     * @param id 订单 ID
     * @return 订单信息实体
     */
    public OrderInfo findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单 ID 不能为空");
        }
        return orderInfoRepository.findById(id);
    }

    /**
     * 根据订单号查询订单信息
     * @param orderId 订单号
     * @return 订单信息实体
     */
    public OrderInfo findByOrderId(String orderId) {
        return findByOrderIdAndPlatformCode(orderId, null);
    }

    public List<OrderInfo> findByOrderIds(Collection<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return orderInfoRepository.findByOrderIds(orderIds);
    }

    /**
     * 根据订单号和平台号查询订单信息。
     * @param orderId 订单号
     * @param platformCode 平台号（可为空）
     * @return 订单信息实体
     */
    public OrderInfo findByOrderIdAndPlatformCode(String orderId, String platformCode) {
        if (StringUtils.isBlank(orderId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单号不能为空");
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("orderId", orderId);
        if (StringUtils.isNotBlank(platformCode)) {
            filters.put("platformCode", platformCode);
        }
        List<OrderInfo> results = orderInfoRepository.filterList(1, 1, filters);

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 查询订单列表（支持分页）
     * @param current 当前页码
     * @param size 每页大小
     * @return 订单列表
     */
    public List<OrderInfo> listOrders(int current, int size) {
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        return orderInfoRepository.list(current, size);
    }

    /**
     * 根据状态查询订单列表（支持分页）
     * @param status 订单状态
     * @param current 当前页码
     * @param size 每页大小
     * @return 订单列表
     */
    public List<OrderInfo> findOrdersByStatus(String status, int current, int size) {
        if (StringUtils.isBlank(status)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单状态不能为空");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("status", status);
        return orderInfoRepository.filterList(current, size, filters);
    }

    /**
     * 模糊搜索订单（支持分页）
     * @param orderId 订单号（支持模糊匹配）
     * @param current 当前页码
     * @param size 每页大小
     * @return 订单列表
     */
    public List<OrderInfo> searchOrders(String orderId, int current, int size) {
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(orderId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单号不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("orderId", orderId);
        return orderInfoRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 添加订单
     * @param orderInfo 订单信息实体
     * @return 添加后的实体
     */
    public OrderInfo addOrder(OrderInfo orderInfo) {
        if (orderInfo == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单信息不能为空");
        }
        if (StringUtils.isBlank(orderInfo.getOrderId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单号不能为空");
        }

        return orderInfoRepository.add(orderInfo);
    }

    /**
     * 更新订单
     * @param orderInfo 订单信息实体
     */
    public void updateOrder(OrderInfo orderInfo) {
        if (orderInfo == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单信息不能为空");
        }
        if (StringUtils.isBlank(orderInfo.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单 ID 不能为空");
        }

        orderInfoRepository.update(orderInfo);
    }

    public boolean tryAcquireTransferLock(String orderInfoId, String lockToken, Date expiredBefore) {
        return orderInfoRepository.tryAcquireTransferLock(orderInfoId, lockToken, expiredBefore);
    }

    public void releaseTransferLock(String orderInfoId, String lockToken) {
        orderInfoRepository.releaseTransferLock(orderInfoId, lockToken);
    }

    /**
     * 删除订单
     * @param id 订单 ID
     */
    public void deleteOrder(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单 ID 不能为空");
        }

        OrderInfo orderInfo = findById(id);
        if (orderInfo != null) {
            orderInfoRepository.delete(orderInfo);
        }
    }

    /**
     * 批量添加订单
     * @param orders 订单列表
     * @return 添加后的订单列表
     */
    public List<OrderInfo> batchAddOrders(List<OrderInfo> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单列表不能为空");
        }
        return (List<OrderInfo>) orderInfoRepository.batchAdd(orders);
    }

    /**
     * 批量更新订单
     * @param orders 订单列表
     */
    public void batchUpdateOrders(List<OrderInfo> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单列表不能为空");
        }
        orderInfoRepository.batchUpdate(orders);
    }

    /**
     * 获取订单总数
     * @return 订单总数
     */
    public long getTotalCount() {
        return orderInfoRepository.total();
    }

    /**
     * 根据状态统计订单数量
     * @param status 订单状态
     * @return 订单数量
     */
    public long countByStatus(String status) {
        if (StringUtils.isBlank(status)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单状态不能为空");
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("status", status);
        return orderInfoRepository.filterTotal(filters);
    }

    /**
     * 检查订单是否存在
     * @param id 订单 ID
     * @return 是否存在
     */
    public boolean existById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "订单 ID 不能为空");
        }
        return orderInfoRepository.existById(id);
    }

    /**
     * 根据订单号检查订单是否存在
     * @param orderId 订单号
     * @return 是否存在
     */
    public boolean existByOrderId(String orderId) {
        OrderInfo order = findByOrderId(orderId);
        return order != null;
    }

    /**
     * 根据多条件分页查询订单列表
     * @param orderId 订单号（可为 null）
     * @param status 订单状态（可为 null）
     * @param startTime 开始时间（可为 null）
     * @param endTime 结束时间（可为 null）
     * @param current 当前页码
     * @param size 每页大小
     * @return 订单列表
     */
    public List<OrderInfo> findOrdersByConditions(String orderId, String status, Date startTime, Date endTime, String routeId, int current, int size) {
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }

        Map<String, Object> filters = new HashMap<>();
        if (StringUtils.isNotBlank(orderId)) {
            filters.put("orderId", orderId);
        }
        if (StringUtils.isNotBlank(status)) {
            filters.put("status", status);
        }
        if (startTime != null) {
            filters.put("createTime_gte", startTime);
        }
        if (endTime != null) {
            filters.put("createTime_lte", endTime);
        }
        if (StringUtils.isNotBlank(routeId)) {
            filters.put("routeId", routeId.trim());
        }

        if (filters.isEmpty()) {
            return orderInfoRepository.list(current, size);
        }

        return orderInfoRepository.filterList(current, size, filters);
    }

    /**
     * 根据工厂和订单创建时间查询订单，用于订单金额统计。
     */
    public List<OrderInfo> findOrdersByManufacturerAndCreateTime(String manufacturerId, Date startTime,
                                                                  Date endTime, int current, int size) {
        if (StringUtils.isBlank(manufacturerId)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工厂 ID 不能为空");
        }
        if (startTime == null || endTime == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "开始时间和结束时间不能为空");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("manufacturerId", manufacturerId.trim());
        filters.put("createTime_gte", startTime);
        filters.put("createTime_lte", endTime);
        return orderInfoRepository.filterList(current, size, filters);
    }

    /**
     * 根据客户信息查询订单号列表。
     *
     * @param customerName 客户姓名，支持模糊匹配
     * @param customerPhone 客户手机号，支持模糊匹配
     * @return 匹配的订单号列表
     */
    public List<String> findOrderIdsByCustomerConditions(String customerName, String customerPhone) {
        Map<String, Object> filters = new HashMap<>();
        if (StringUtils.isNotBlank(customerName)) {
            filters.put("customer.customerName_like", customerName);
        }
        if (StringUtils.isNotBlank(customerPhone)) {
            filters.put("customer.customerPhone_like", customerPhone);
        }
        if (filters.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> orderIds = new ArrayList<>();
        int current = 1;
        int size = 100;
        while (true) {
            List<OrderInfo> orders = orderInfoRepository.filterList(current, size, filters);
            if (orders == null || orders.isEmpty()) {
                break;
            }
            orders.stream()
                    .map(OrderInfo::getOrderId)
                    .filter(StringUtils::isNotBlank)
                    .forEach(orderIds::add);
            if (orders.size() < size) {
                break;
            }
            current++;
        }
        return orderIds;
    }


    /**
     * 根据下单企业名称查询订单号列表。
     *
     * @param orgName 下单企业名称，支持模糊匹配
     * @return 匹配的订单号列表
     */
    public List<String> findOrderIdsByOrgName(String orgName) {
        if (StringUtils.isBlank(orgName)) {
            return Collections.emptyList();
        }
        Map<String, Object> filters = new HashMap<>();
        filters.put("orgInfo.name_like", orgName.trim());

        List<String> orderIds = new ArrayList<>();
        int current = 1;
        int size = 100;
        while (true) {
            List<OrderInfo> orders = orderInfoRepository.filterList(current, size, filters);
            if (orders == null || orders.isEmpty()) {
                break;
            }
            orders.stream()
                    .map(OrderInfo::getOrderId)
                    .filter(StringUtils::isNotBlank)
                    .forEach(orderIds::add);
            if (orders.size() < size) {
                break;
            }
            current++;
        }
        return orderIds;
    }

    /**
     * 根据多条件查询订单（包含客户手机号）
     * @param orderId 订单号
     * @param status 订单状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param customerPhone 客户手机号
     * @param current 当前页码
     * @param size 每页大小
     * @return 订单列表
     */
    public List<OrderInfo> findOrdersByConditionsWithCustomerPhone(String orderId, String status, Date startTime, Date endTime, String customerPhone, int current, int size) {
        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }

        Map<String, Object> filters = new HashMap<>();
        if (StringUtils.isNotBlank(orderId)) {
            filters.put("orderId", orderId);
        }
        if (StringUtils.isNotBlank(status)) {
            filters.put("status", status);
        }
        if (startTime != null) {
            filters.put("createTime_gte", startTime);
        }
        if (endTime != null) {
            filters.put("createTime_lte", endTime);
        }
        if (StringUtils.isNotBlank(customerPhone)) {
            filters.put("customer.customerPhone", customerPhone);
        }

        if (filters.isEmpty()) {
            return orderInfoRepository.list(current, size);
        }

        return orderInfoRepository.filterList(current, size, filters);
    }

    /**
     * 根据多条件统计订单数量
     * @param orderId 订单号（可为 null）
     * @param status 订单状态（可为 null）
     * @param startTime 开始时间（可为 null）
     * @param endTime 结束时间（可为 null）
     * @return 订单数量
     */
    public long countByConditions(String orderId, String status, Date startTime, Date endTime) {
        Map<String, Object> filters = new HashMap<>();
        if (StringUtils.isNotBlank(orderId)) {
            filters.put("orderId", orderId);
        }
        if (StringUtils.isNotBlank(status)) {
            filters.put("status", status);
        }
        if (startTime != null) {
            filters.put("createTime_gte", startTime);
        }
        if (endTime != null) {
            filters.put("createTime_lte", endTime);
        }
        if (filters.isEmpty()) {
            return orderInfoRepository.total();
        }

        return orderInfoRepository.filterTotal(filters);
    }


    private String buildProcessingFlow(List<ProcedureFlowNode> nodes, String materialName) {
        if (nodes == null || nodes.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        ProcedureFlowNode firstNode = nodes.get(0);
        if (firstNode != null && StringUtils.isNotBlank(firstNode.getNodeName()) && StringUtils.isNotBlank(materialName)) {
            names.add(firstNode.getNodeName() + "（" + materialName + "）");
        }
        for (ProcedureFlowNode node : nodes) {
            if (node == null || StringUtils.isBlank(node.getNodeName())) {
                continue;
            }
            names.add(resolveNodeDisplayName(node));
        }
        return String.join("-", names);
    }

    private String resolveNodeDisplayName(ProcedureFlowNode node) {
        String nodeName = node.getNodeName();
        if (node.getParamConfigs() == null) {
            return nodeName;
        }
        List<String> accessoryNames = new ArrayList<>();
        for (MTOProductSpecDTO.ProcessParamConfigDTO config : node.getParamConfigs()) {
            if (config == null) {
                continue;
            }
            if (!"ACC".equals(resolveConfigType(config))) {
                continue;
            }
            Object nameValue = extractParamDisplayName(config);
            String name = nameValue == null ? null : String.valueOf(nameValue);
            if (StringUtils.isNotBlank(name)) {
                accessoryNames.add(name);
            }
        }
        if (!accessoryNames.isEmpty()) {
            return nodeName + "（" + String.join("、", accessoryNames) + "）";
        }
        return nodeName;
    }

    private String resolveConfigType(Object config) {
        Object directType = extractFieldValue(config, "type");
        if (directType != null && StringUtils.isNotBlank(String.valueOf(directType))) {
            return String.valueOf(directType);
        }
        Object param = extractFieldValue(config, "param");
        Object paramType = extractFieldValue(param, "type");
        return paramType == null ? null : String.valueOf(paramType);
    }

    private Object extractParamDisplayName(Object config) {
        Object param = extractFieldValue(config, "param");
        Object accessorySnapshot = extractFieldValue(param, "accessorySnapshot");
        Object accessoryName = extractFieldValue(accessorySnapshot, "name");
        if (accessoryName != null && StringUtils.isNotBlank(String.valueOf(accessoryName))) {
            return accessoryName;
        }
        Object metaSnapshot = extractFieldValue(param, "processParamMetaSnapshot");
        return extractFieldValue(metaSnapshot, "name");
    }

    private Object extractFieldValue(Object target, String fieldName) {
        if (target == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        if (target instanceof Map<?, ?> map) {
            return map.get(fieldName);
        }
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            return target.getClass().getMethod(getterName).invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 统计符合条件的订单数量（包含客户手机号）
     * @param orderId 订单号
     * @param status 订单状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param customerPhone 客户手机号
     * @return 订单总数
     */
    public long countByConditionsWithCustomerPhone(String orderId, String status, Date startTime, Date endTime, String customerPhone) {
        Map<String, Object> filters = new HashMap<>();
        if (StringUtils.isNotBlank(orderId)) {
            filters.put("orderId", orderId);
        }
        if (StringUtils.isNotBlank(status)) {
            filters.put("status", status);
        }
        if (startTime != null) {
            filters.put("createTime_gte", startTime);
        }
        if (endTime != null) {
            filters.put("createTime_lte", endTime);
        }
        if (StringUtils.isNotBlank(customerPhone)) {
            filters.put("customer.customerPhone", customerPhone);
        }

        if (filters.isEmpty()) {
            return orderInfoRepository.total();
        }

        return orderInfoRepository.filterTotal(filters);
    }

    /**
     * 添加订单及订单项
     * @param orderInfo 订单信息
     * @param orderItems 订单项列表
     * @return 添加后的订单信息
     */
    public List<OrderItem> addOrderWithItems(OrderInfo orderInfo, List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return new ArrayList<>();
        }

        matchOrCreateAddressRecognitionRecord(orderInfo, orderItems);

        // 为每个订单项生成唯一的 orderItemId 并完善 procedureFlow 数据
        List<OrderItem> itemsToAdd = new ArrayList<>();
        Set<String> newItemKeys = new HashSet<>();
        for (OrderItem item : orderItems) {
            if (item == null) {
                continue;
            }
            if (StringUtils.isBlank(item.getOrderItemId())) {
                String orderItemId = IdGenerator.generateOrderItemId();
                item.setOrderItemId(orderItemId);
            }
            item.setOrderId(orderInfo.getOrderId());
            item.setChannel(orderInfo.getChannel());
            item.setOrgInfo(orderInfo.getOrgInfo());
            if (isDuplicateOrderItem(orderInfo, item, newItemKeys)) {
                continue;
            }

            // 从 mtoProduct 中获取 processFlow 并转换为 procedureFlow
            String processFlow = "";
            if (item.getMtoProduct() != null) {
                MTOProductSpecDTO mtoProductDto = item.getMtoProduct();
                MTOProductSpec mtoProductSpec = mtoProductDto.toDO();
                // 获取首个可用的 ASSET 参数图片作为生产图（避免直接强转）
                ImageFile productionImgFile = getFirstAssetImageFile(mtoProductSpec);
                if (productionImgFile != null) {
                    item.setProductionImgFile(productionImgFile);
                }
                //再判断是否存在异形切割图片
                com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.Process processWithContourSliceImg = mtoProductSpec.findProcessWithContourSliceImg();
                if(processWithContourSliceImg != null){
                    FileAssetParam maskParam = (FileAssetParam) processWithContourSliceImg.getParamConfigs().get(0).getParam();
                    File file1 = maskParam.getFile();
                    ImageFile maskfile;
                    if (file1 instanceof ImageFile) {
                        maskfile = (ImageFile) file1;
                    } else {
                        maskfile = ImageFile.cloneFromFile(file1);
                    }
                    item.setMaskImgFile(maskfile);
                }
                ProcedureFlow procedureFlow = orderPreprocessingService.convertProcessFlowToProcedureFlow(mtoProductDto);
                MaterialConfig materialConfigFromMTOProduct = this.orderPreprocessingService.getMaterialConfigFromMTOProduct(item.getMtoProduct());
                if (procedureFlow != null) {
                    List<ProcedureFlowNode> nodes = procedureFlow.getNodes();
                    String materialName = materialConfigFromMTOProduct != null && materialConfigFromMTOProduct.getMaterialSnapshot() != null
                            ? materialConfigFromMTOProduct.getMaterialSnapshot().getName()
                            : null;
                    processFlow = buildProcessingFlow(nodes, materialName);
                    item.setProcedureFlow(procedureFlow);
                }
                item.setLogisticsCarrierInfo(item.getLogisticsCarrierInfo());
                item.setMaterial(materialConfigFromMTOProduct);
            }
            item.setProcessingFlow(processFlow);
            itemsToAdd.add(item);
        }

        if (itemsToAdd.isEmpty()) {
            return new ArrayList<>();
        }

        // 先添加订单主表；如果订单已存在但缺少新增的渠道信息，则补齐渠道信息。
        saveOrPatchOrderInfo(orderInfo);

        // 批量添加订单项，并获取带有ID的结果
        Collection<OrderItem> savedOrderItemsCollection = orderItemRepository.batchAdd(itemsToAdd);
        List<OrderItem> orderItemsResult = new ArrayList<>(savedOrderItemsCollection);

        return orderItemsResult;
    }

    private boolean isDuplicateOrderItem(OrderInfo orderInfo, OrderItem item, Set<String> newItemKeys) {
        String orderId = item.getOrderId();
        String orderItemId = item.getOrderItemId();
        String manufacturerMetaId = resolveOrderItemManufacturerMetaId(orderInfo, item);
        if (StringUtils.isBlank(orderId) || StringUtils.isBlank(orderItemId) || StringUtils.isBlank(manufacturerMetaId)) {
            return false;
        }

        String itemKey = orderId + "|" + orderItemId + "|" + manufacturerMetaId;
        if (!newItemKeys.add(itemKey)) {
            return true;
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("orderId", orderId);
        filters.put("orderItemId", orderItemId);
        filters.put("manufacturerId", manufacturerMetaId);
        return orderItemRepository.filterTotal(filters) > 0;
    }

    private String resolveOrderItemManufacturerMetaId(OrderInfo orderInfo, OrderItem item) {
        if (item != null && StringUtils.isNotBlank(item.getManufacturerId())) {
            return item.getManufacturerId();
        }
        return orderInfo != null ? orderInfo.getManufacturerId() : null;
    }

    private void saveOrPatchOrderInfo(OrderInfo orderInfo) {
        OrderInfo existingOrder = findExistingOrder(orderInfo);
        if (existingOrder == null) {
            orderInfoRepository.add(orderInfo);
            return;
        }
        if (existingOrder.getChannel() == null && orderInfo.getChannel() != null) {
            existingOrder.setChannel(orderInfo.getChannel());
            orderInfoRepository.update(existingOrder);
        }
    }

    private OrderInfo findExistingOrder(OrderInfo orderInfo) {
        if (orderInfo == null || StringUtils.isBlank(orderInfo.getOrderId())) {
            return null;
        }
        Map<String, Object> filters = new HashMap<>();
        filters.put("orderId", orderInfo.getOrderId());
        if (StringUtils.isNotBlank(orderInfo.getManufacturerId())) {
            filters.put("manufacturerId", orderInfo.getManufacturerId());
        }
        List<OrderInfo> results = orderInfoRepository.filterList(1, 1, filters);
        return results == null || results.isEmpty() ? null : results.get(0);
    }


    private void fillAddressRecognitionOrderInfo(AddressRecognitionRecord record, OrderInfo orderInfo) {
        record.setOrderId(orderInfo.getOrderId());
        record.setOrgInfo(orderInfo.getOrgInfo());
        if (orderInfo.getCustomer() != null) {
            AddressRecognitionConsignee consignee = new AddressRecognitionConsignee();
            consignee.setName(orderInfo.getCustomer().getCustomerName());
            consignee.setPhone(orderInfo.getCustomer().getCustomerPhone());
            consignee.setAddress(orderInfo.getCustomer().getAddress());
            record.setConsignee(consignee);
        }
    }

    private String resolveManufacturerMetaId(List<OrderItem> orderItems) {
        if (orderItems == null) {
            return null;
        }
        for (OrderItem item : orderItems) {
            if (item != null && StringUtils.isNotBlank(item.getManufacturerId())) {
                return item.getManufacturerId();
            }
        }
        return null;
    }

    private void matchOrCreateAddressRecognitionRecord(OrderInfo orderInfo, List<OrderItem> orderItems) {
        if (isWdtOrder(orderInfo)) {
            return;
        }
        if (orderInfo == null || orderInfo.getCustomer() == null || orderInfo.getCustomer().getAddress() == null) {
            return;
        }
        Address address = orderInfo.getCustomer().getAddress();
        if (StringUtils.isBlank(address.getTerminalRegionCode()) || StringUtils.isBlank(address.getDetailAddress())) {
            return;
        }

        String manufacturerMetaId = resolveManufacturerMetaId(orderItems);
        if (StringUtils.isBlank(manufacturerMetaId)) {
            return;
        }

        AddressRecognitionRecord record = addressRecognitionRecordRepository.findByAddress(
                manufacturerMetaId,
                address.getTerminalRegionCode(),
                address.getDetailAddress()
        );
        if (record == null) {
            record = new AddressRecognitionRecord();
            record.setManufacturerMetaId(manufacturerMetaId);
            record.setAddress(address);
            fillAddressRecognitionOrderInfo(record, orderInfo);
            record.setStatus(AddressRecognitionRecordStatus.UNASSIGNED);
            addressRecognitionRecordRepository.add(record);
            return;
        }

        if (AddressRecognitionRecordStatus.UNASSIGNED.equals(record.getStatus()) && StringUtils.isBlank(record.getOrderId())) {
            fillAddressRecognitionOrderInfo(record, orderInfo);
            record.setManufacturerMetaId(manufacturerMetaId);
            addressRecognitionRecordRepository.update(record);
            return;
        }

        if (record.getOrgInfo() == null || record.getConsignee() == null) {
            fillAddressRecognitionOrderInfo(record, orderInfo);
            record.setManufacturerMetaId(manufacturerMetaId);
            addressRecognitionRecordRepository.update(record);
        }

        if (AddressRecognitionRecordStatus.ASSIGNED.equals(record.getStatus())
                && StringUtils.isNotBlank(record.getRouteId())
                && StringUtils.isNotBlank(record.getNodeId())) {
            orderInfo.setRouteId(record.getRouteId());
            orderInfo.setRouteNodeId(record.getNodeId());
            if (orderItems != null) {
                for (OrderItem item : orderItems) {
                    if (item != null) {
                        item.setRouteId(record.getRouteId());
                        item.setRouteNodeId(record.getNodeId());
                    }
                }
            }
        }
    }

    private boolean isWdtOrder(OrderInfo orderInfo) {
        return orderInfo != null
                && orderInfo.getChannel() != null
                && OrderChannelType.GATHER_PLATFORM.equals(orderInfo.getChannel().getType());
    }

    private ImageFile getFirstAssetImageFile(MTOProductSpec mtoProductSpec) {
        if (mtoProductSpec == null || mtoProductSpec.getFirstProcessParamConfigs() == null) {
            return null;
        }

        List<ProcessParamConfig> firstProcessParamConfigs = mtoProductSpec.getFirstProcessParamConfigs();
        for (ProcessParamConfig processParamConfig : firstProcessParamConfigs) {
            if (processParamConfig == null || processParamConfig.getParam() == null) {
                continue;
            }
            if (!(processParamConfig.getParam() instanceof FileAssetParam)) {
                continue;
            }
            File file = ((FileAssetParam) processParamConfig.getParam()).getFile();
            if (file == null) {
                continue;
            }
            if (file instanceof ImageFile) {
                return (ImageFile) file;
            }
            return ImageFile.cloneFromFile(file);
        }
        return null;
    }
}
