package com.mes.domain.manufacturer.typesetting.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingStatus;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TypesettingService {

    @Autowired
    private TypesettingRepository typesettingRepository;

    /**
     * 根据多条件查询排版信息（支持分页）
     * @param status 状态
     * @param material 材质
     * @param nodeName 工序节点名称
     * @param current 当前页码
     * @param size 每页大小
     * @return 排版信息列表
     */
    public List<TypesettingInfo> findTypesettingByConditions(
            String status,
            String material, 
            String nodeName,
            int current,
            int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }

        Map<String, Object> filters = new HashMap<>();
        
        if (status != null) {
            filters.put("status", status);
        }
        if (StringUtils.isNotBlank(material)) {
            filters.put("material", material);
        }
        
        List<TypesettingInfo> allItems = typesettingRepository.filterList(current, size, filters);
        
        if (StringUtils.isNotBlank(nodeName)) {
            return allItems.stream()
                .filter(item -> {
                    if (item.getProcedureFlow() != null && item.getProcedureFlow().getNodes() != null) {
                        return item.getProcedureFlow().getNodes().stream()
                            .anyMatch(node -> nodeName.equals(node.getNodeName()));
                    }
                    return false;
                })
                .collect(Collectors.toList());
        }
        
        return allItems;
    }

    /**
     * 根据多条件查询排版信息总数
     * @param status 状态
     * @param material 材质
     * @param nodeName 工序节点名称
     * @return 总数
     */
    public long countTypesettingByConditions(
            String status,
            String material, 
            String nodeName) {
        Map<String, Object> filters = new HashMap<>();
        
        if (status != null) {
            filters.put("status", status);
        }
        if (StringUtils.isNotBlank(material)) {
            filters.put("material", material);
        }
        
        long total = typesettingRepository.filterTotal(filters);
        
        if (StringUtils.isNotBlank(nodeName)) {
            List<TypesettingInfo> allItems = typesettingRepository.filterList(1, Integer.MAX_VALUE, filters);
            return allItems.stream()
                .filter(item -> {
                    if (item.getProcedureFlow() != null && item.getProcedureFlow().getNodes() != null) {
                        return item.getProcedureFlow().getNodes().stream()
                            .anyMatch(node -> nodeName.equals(node.getNodeName()));
                    }
                    return false;
                })
                .count();
        }
        
        return total;
    }

    /**
     * 根据排版文件 ID 查询排版信息
     * @param typesettingId 排版文件 ID
     * @return 排版信息列表
     */
    public TypesettingInfo findTypesettingByTypesettingId(String typesettingId) {

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("typesettingId", typesettingId);
        List<TypesettingInfo> typesettingInfos = typesettingRepository.fuzzySearch(searchFilters, 1, 100);
        if (typesettingInfos.size() > 0) {
            return typesettingInfos.get(0);
        }
        return null;
    }

    /**
     * 根据排版文件 ID 查询排版信息列表
     * @param typesettingId 排版文件 ID
     * @return 排版信息列表
     */
    public List<TypesettingInfo> findTypesettingListByTypesettingId(String typesettingId) {
        if (StringUtils.isBlank(typesettingId)) {
            return Collections.emptyList();
        }
        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("typesettingId", typesettingId);
        return typesettingRepository.fuzzySearch(searchFilters, 1, 1000);
    }

    /**
     * 根据材质查询排版信息（支持分页）
     * @param material 材质
     * @param current 当前页码
     * @param size 每页大小
     * @return 排版信息列表
     */
    public List<TypesettingInfo> findTypesettingByMaterial(String material, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (StringUtils.isBlank(material)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "材质不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("material", material);
        return typesettingRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 根据状态查询排版信息（支持分页）
     * @param status 排版状态
     * @param current 当前页码
     * @param size 每页大小
     * @return 排版信息列表
     */
    public List<TypesettingInfo> findTypesettingByStatus(TypesettingStatus status, int current, int size) {

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在 1-100 之间");
        }
        if (status == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版状态不能为空");
        }

        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("status", status.getCode());
        return typesettingRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 获取排版信息总数
     * @param typesettingId 排版文件 ID，可为空
     * @return 总数
     */
    public long getTotalCount(String typesettingId) {
        if (StringUtils.isNotBlank(typesettingId)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("typesettingId", typesettingId);
            return typesettingRepository.totalByFuzzySearch(searchFilters);
        } else {
            return typesettingRepository.total();
        }
    }

    /**
     * 获取指定状态的排版信息总数
     * @param status 排版状态
     * @return 总数
     */
    public long getCountByStatus(TypesettingStatus status) {
        if (status == null) {
            return typesettingRepository.total();
        }
        
        Map<String, Object> filters = new HashMap<>();
        filters.put("status", status.getCode());
        return typesettingRepository.filterTotal(filters);
    }

    /**
     * 添加排版信息
     * @param typesettingInfo 排版信息实体
     * @return 添加后的实体
     */
    public TypesettingInfo addTypesetting(TypesettingInfo typesettingInfo) {
        if (typesettingInfo == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版信息不能为空");
        }
        if (StringUtils.isBlank(typesettingInfo.getTypesettingId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版文件 ID 不能为空");
        }
        
        if (typesettingInfo.getStatus() == null) {
            typesettingInfo.setStatus(TypesettingStatus.PENDING.getCode());
        }
        
        if (typesettingInfo.getQuantity() != null && typesettingInfo.getQuantity() < 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "数量不能为负数");
        }
        
        if (typesettingInfo.getCompletedQuantity() != null && typesettingInfo.getCompletedQuantity() < 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "已完成数量不能为负数");
        }
        typesettingInfo.applyLayoutModeConfig();
        return typesettingRepository.add(typesettingInfo);
    }

    /**
     * 更新排版信息
     * @param typesettingInfo 排版信息实体
     */
    public void updateTypesetting(TypesettingInfo typesettingInfo) {
        if (typesettingInfo == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版信息不能为空");
        }
        if (StringUtils.isBlank(typesettingInfo.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版信息 ID 不能为空");
        }
        
        if (typesettingInfo.getQuantity() != null && typesettingInfo.getQuantity() < 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "数量不能为负数");
        }
        
        if (typesettingInfo.getCompletedQuantity() != null && typesettingInfo.getCompletedQuantity() < 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "已完成数量不能为负数");
        }
        
        if (typesettingInfo.getCompletedQuantity() != null && typesettingInfo.getQuantity() != null 
            && typesettingInfo.getCompletedQuantity() > typesettingInfo.getQuantity()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "已完成数量不能大于总数量");
        }

        typesettingInfo.applyLayoutModeConfig();
        typesettingRepository.update(typesettingInfo);
    }

    /**
     * 更新排版状态
     * @param id 排版信息 ID
     * @param status 新排版状态
     */
    public void updateTypesettingStatus(String id, TypesettingStatus status) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版信息 ID 不能为空");
        }
        if (status == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版状态不能为空");
        }
        
        TypesettingInfo typesettingInfo = findById(id);
        if (typesettingInfo != null) {
            typesettingInfo.setStatus(status.getCode());
            updateTypesetting(typesettingInfo);
        }
    }

    /**
     * 更新完成数量
     * @param id 排版信息 ID
     * @param completedQuantity 完成数量
     */
    public void updateCompletedQuantity(String id, Integer completedQuantity) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "排版信息 ID 不能为空");
        }
        if (completedQuantity == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "完成数量不能为空");
        }
        if (completedQuantity < 0) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "完成数量不能为负数");
        }
        
        TypesettingInfo typesettingInfo = findById(id);
        if (typesettingInfo != null) {
            if (typesettingInfo.getQuantity() != null && completedQuantity > typesettingInfo.getQuantity()) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "完成数量不能大于总数量");
            }
            typesettingInfo.setCompletedQuantity(completedQuantity);
            updateTypesetting(typesettingInfo);
        }
    }

    /**
     * 将排版状态更新为待排版
     * @param id 排版信息 ID
     */
    public void markAsPending(String id) {
        updateTypesettingStatus(id, TypesettingStatus.PENDING);
    }

    /**
     * 将排版状态更新为排版中
     * @param id 排版信息 ID
     */
    public void markAsInProgress(String id) {
        updateTypesettingStatus(id, TypesettingStatus.IN_PROGRESS);
    }

    /**
     * 将排版状态更新为确认中
     * @param id 排版信息 ID
     */
    public void markAsConfirming(String id) {
        updateTypesettingStatus(id, TypesettingStatus.CONFIRMING);
    }

    /**
     * 将排版状态更新为已下达
     * @param id 排版信息 ID
     */
    public void markAsCompleted(String id) {
        updateTypesettingStatus(id, TypesettingStatus.COMPLETED);
    }

    /**
     * 删除排版信息
     * @param id 排版信息 ID
     */
    public void deleteTypesetting(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }
        
        TypesettingInfo typesettingInfo = typesettingRepository.findById(id);
        if (typesettingInfo != null) {
            typesettingRepository.delete(typesettingInfo);
        }
    }

    /**
     * 根据 ID 获取排版信息
     * @param id 排版信息 ID
     * @return 排版信息实体
     */
    public TypesettingInfo findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID 不能为空");
        }
        return typesettingRepository.findById(id);
    }
}
