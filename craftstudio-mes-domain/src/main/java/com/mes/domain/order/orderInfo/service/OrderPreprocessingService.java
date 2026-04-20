package com.mes.domain.order.orderInfo.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.FlowStatus;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.params.ProcessParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderPreprocessingService {

    /**
     * 将 MTOProductSpecDTO 中的 ProcessFlowDTO 转换为 ProcedureFlow
     * @param mtoProduct MTO 产品规格 DTO
     * @return 转换后的 ProcedureFlow，如果 mtoProduct 为 null 或没有 processFlow 则返回 null
     */
    public ProcedureFlow convertProcessFlowToProcedureFlow(MTOProductSpecDTO mtoProduct) {
        if (mtoProduct == null) {
            return null;
        }

        // 从 MTOProductSpecDTO 中获取 processFlow
        MTOProductSpecDTO.ProcessFlowDTO processFlowDTO = getProcessFlowFromMTOProduct(mtoProduct);
        if (processFlowDTO == null) {
            return null;
        }

        // 创建 ProcedureFlow
        ProcedureFlow procedureFlow = new ProcedureFlow();

        // 设置基本信息
        procedureFlow.setProcedureFlowId(getProcessFlowId(processFlowDTO));
        procedureFlow.setProcedureFlowName(getProcessFlowName(processFlowDTO));
        procedureFlow.setFlowDescription(getProcessFlowDescription(processFlowDTO));
        procedureFlow.setFlowStatus(FlowStatus.NOT_STARTED);

        // 转换节点列表
        List<MTOProductSpecDTO.ProcessDTO> nodes = getProcessFlowNodes(processFlowDTO);
        if (nodes != null && !nodes.isEmpty()) {
            List<ProcedureFlowNode> procedureFlowNodes = nodes.stream()
                    .map(this::convertProcessFlowNodeToProcedureFlowNode)
                    .collect(Collectors.toList());

            procedureFlow.setNodes(procedureFlowNodes);
            procedureFlow.setTotalNodes(procedureFlowNodes.size());
        } else {
            procedureFlow.setNodes(new ArrayList<>());
            procedureFlow.setTotalNodes(0);
        }

        return procedureFlow;
    }

    /**
     * 从 MTOProductSpecDTO 中获取 processFlow
     * 注意：此处需要使用反射或其他方式访问外部 DTO 的字段
     */
    private MTOProductSpecDTO.ProcessFlowDTO getProcessFlowFromMTOProduct(MTOProductSpecDTO mtoProduct) {
        try {
            java.lang.reflect.Method getProcessFlowMethod = mtoProduct.getClass().getDeclaredMethod("getProcessFlow");
            return (MTOProductSpecDTO.ProcessFlowDTO) getProcessFlowMethod.invoke(mtoProduct);
        } catch (Exception e) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "无法获取 processFlow 信息：" + e.getMessage());
        }
    }

    /**
     * 获取 processFlow 的 ID
     */
    private String getProcessFlowId(Object processFlowDTO) {
        try {
            java.lang.reflect.Method getIdMethod = processFlowDTO.getClass().getDeclaredMethod("getId");
            return (String) getIdMethod.invoke(processFlowDTO);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 processFlow 的名称
     */
    private String getProcessFlowName(Object processFlowDTO) {
        try {
            java.lang.reflect.Method getNameMethod = processFlowDTO.getClass().getDeclaredMethod("getName");
            return (String) getNameMethod.invoke(processFlowDTO);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 processFlow 的描述
     */
    private String getProcessFlowDescription(Object processFlowDTO) {
        try {
            java.lang.reflect.Method getDescriptionMethod = processFlowDTO.getClass().getDeclaredMethod("getDescription");
            return (String) getDescriptionMethod.invoke(processFlowDTO);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 processFlow 的节点列表
     */
    private List<MTOProductSpecDTO.ProcessDTO> getProcessFlowNodes(MTOProductSpecDTO.ProcessFlowDTO processFlowDTO) {
        if (processFlowDTO == null) {
            return new ArrayList<>();
        }
        
        try {
            // 获取根节点
            java.lang.reflect.Method getRootProcessMethod = processFlowDTO.getClass().getDeclaredMethod("getRootProcess");
            MTOProductSpecDTO.ProcessDTO rootProcess = (MTOProductSpecDTO.ProcessDTO) getRootProcessMethod.invoke(processFlowDTO);
            
            if (rootProcess == null) {
                return new ArrayList<>();
            }
            
            // 从根节点开始递归收集所有节点
            List<MTOProductSpecDTO.ProcessDTO> allNodes = new ArrayList<>();
            collectProcesses(rootProcess, allNodes);
            return allNodes;
        } catch (Exception e) {
            System.err.println("获取 processFlow 节点失败：" + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 递归收集工艺流程中的所有节点
     */
    private void collectProcesses(MTOProductSpecDTO.ProcessDTO process, List<MTOProductSpecDTO.ProcessDTO> allNodes) throws Exception {
        if (process == null) {
            return;
        }
        
        // 添加当前节点
        allNodes.add(process);
        
        // 获取下一个节点（next）
        try {
            java.lang.reflect.Method getNextMethod = process.getClass().getDeclaredMethod("getNext");
            MTOProductSpecDTO.ProcessDTO nextProcess = (MTOProductSpecDTO.ProcessDTO) getNextMethod.invoke(process);
            if (nextProcess != null) {
                collectProcesses(nextProcess, allNodes);
            }
        } catch (NoSuchMethodException e) {
            // 没有 next 方法则停止
        }
    }


    /**
     * 将 ProcessDTO 转换为 ProcedureFlowNode
     */
    private ProcedureFlowNode convertProcessFlowNodeToProcedureFlowNode(MTOProductSpecDTO.ProcessDTO processDTO) {
        if (processDTO == null) {
            return null;
        }
        
        ProcedureFlowNode node = new ProcedureFlowNode();

        try {
            // 设置节点基本信息
            node.setNodeId(processDTO.getProcessMetaId());
            
            // 获取工序名称
            if (processDTO.getProcessMetaSnapshot() != null) {
                node.setNodeName(processDTO.getProcessMetaSnapshot().getName());
            }
            
            // 设置节点顺序（需要在外部统一设置）
            node.setNodeOrder(0);
            
            // 设置节点类型
            node.setNodeType("PROCESS");
            
            // 设置描述
            if (processDTO.getProcessMetaSnapshot() != null) {
                node.setDescription(processDTO.getProcessMetaSnapshot().getName());
            }

            // 获取并设置 paramConfigs（如果存在）
            if (processDTO.getParamConfigs() != null && !processDTO.getParamConfigs().isEmpty()) {
                List< MTOProductSpecDTO.ProcessParamConfigDTO> paramConfigDTOs = 
                    processDTO.getParamConfigs().stream()
                        .map(this::safeConvertParamConfig)
                        .toList();
                
                // 由于 paramConfigs 是外部类的字段，需要使用反射设置
                try {
                    java.lang.reflect.Method setParamConfigsMethod = node.getClass()
                        .getDeclaredMethod("setParamConfigs", List.class);
                    setParamConfigsMethod.setAccessible(true);
                    setParamConfigsMethod.invoke(node, paramConfigDTOs);
                } catch (NoSuchMethodException e) {
                    // 如果 node 没有 paramConfigs 字段，忽略
                }
            }

            // 设置节点状态，默认为 PENDING
            node.setNodeStatus(NodeStatus.PENDING);
            node.setProcessMetaSnapshot(processDTO.getProcessMetaSnapshot());
        } catch (Exception e) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "转换节点失败：" + e.getMessage());
        }

        return node;
    }

    /**
     * 将本地的 ProcessParam 转换为外部的 ProcessParamDTO
     */
    @SuppressWarnings("unchecked")
    private MTOProductSpecDTO.ProcessParamDTO convertLocalParamToExternalParam(
            ProcessParam param) {
        if (param == null) {
            return null;
        }
        
        try {
            // 调用外部的 fromDO 静态方法
            Class<?> externalParamDTOClass = MTOProductSpecDTO.ProcessParamDTO.class;
            java.lang.reflect.Method fromDOMethod = externalParamDTOClass.getDeclaredMethod("fromDO", 
                ProcessParam.class);
            
            return (MTOProductSpecDTO.ProcessParamDTO) 
                fromDOMethod.invoke(null, param);
        } catch (Exception e) {
            System.err.println("转换工艺参数失败：" + e.getMessage());
            return null;
        }
    }

    /**
     * 安全地转换 ProcessParamConfig，允许任何字段为 null
     */
    private MTOProductSpecDTO.ProcessParamConfigDTO safeConvertParamConfig(
            com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO.ProcessParamConfigDTO config) {
        if (config == null) {
            return null;
        }
        
        try {
            MTOProductSpecDTO.ProcessParamConfigDTO dto = new MTOProductSpecDTO.ProcessParamConfigDTO();
            dto.setCustomizable(config.isCustomizable());
            
            if (config.getParam() != null) {
                try {
                    dto.setParam(convertLocalParamToExternalParam(config.getParam().toDO()));
                } catch (Exception e) {
                    System.err.println("转换参数配置失败，跳过该参数: " + e.getMessage());
                }
            }
            
            return dto;
        } catch (Exception e) {
            System.err.println("创建参数配置 DTO 失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从 MTOProductSpecDTO 中获取 MaterialConfig
     * @param mtoProduct MTO 产品规格 DTO
     * @return 材质配置信息，如果不存在则返回 null
     */
    public MaterialConfig getMaterialConfigFromMTOProduct(MTOProductSpecDTO mtoProduct) {
        if (mtoProduct == null) {
            return null;
        }

        try {
            // 使用反射调用 getMaterialConfig 方法
            java.lang.reflect.Method method = mtoProduct.getClass().getMethod("getMaterialConfig");
            if (method != null) {
                Object result = method.invoke(mtoProduct);
                if (result instanceof MaterialConfig) {
                    return (MaterialConfig) result;
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            // 方法不存在或调用失败，返回 null
            return null;
        }

        return null;
    }
}
