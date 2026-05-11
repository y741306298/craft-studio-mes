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
     */
    private MTOProductSpecDTO.ProcessFlowDTO getProcessFlowFromMTOProduct(MTOProductSpecDTO mtoProduct) {
        return mtoProduct.getProcessFlow();
    }

    /**
     * 获取 processFlow 的 ID
     */
    private String getProcessFlowId(MTOProductSpecDTO.ProcessFlowDTO processFlowDTO) {
        return processFlowDTO.getId();
    }

    /**
     * 获取 processFlow 的名称
     */
    private String getProcessFlowName(MTOProductSpecDTO.ProcessFlowDTO processFlowDTO) {
        return processFlowDTO.getName();
    }

    /**
     * 获取 processFlow 的描述
     */
    private String getProcessFlowDescription(MTOProductSpecDTO.ProcessFlowDTO processFlowDTO) {
        return processFlowDTO.getDescription();
    }

    /**
     * 获取 processFlow 的节点列表
     */
    private List<MTOProductSpecDTO.ProcessDTO> getProcessFlowNodes(MTOProductSpecDTO.ProcessFlowDTO processFlowDTO) {
        if (processFlowDTO == null) {
            return new ArrayList<>();
        }
        
        MTOProductSpecDTO.ProcessDTO rootProcess = processFlowDTO.getRootProcess();
        if (rootProcess == null) {
            return new ArrayList<>();
        }
        List<MTOProductSpecDTO.ProcessDTO> allNodes = new ArrayList<>();
        collectProcesses(rootProcess, allNodes);
        return allNodes;
    }

    /**
     * 递归收集工艺流程中的所有节点
     */
    private void collectProcesses(MTOProductSpecDTO.ProcessDTO process, List<MTOProductSpecDTO.ProcessDTO> allNodes) {
        if (process == null) {
            return;
        }
        
        // 添加当前节点
        allNodes.add(process);
        
        MTOProductSpecDTO.ProcessDTO nextProcess = process.getNext();
        if (nextProcess != null) {
            collectProcesses(nextProcess, allNodes);
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

            // 将 paramConfigs 转成结构化对象，避免持久化时变成纯字符串
            if (processDTO.getParamConfigs() != null && !processDTO.getParamConfigs().isEmpty()) {
                List<MTOProductSpecDTO.ProcessParamConfigDTO> paramConfigDTOs = processDTO.getParamConfigs().stream()
                        .map(this::convertParamConfigToTypedObject)
                        .collect(Collectors.toList());
                node.setParamConfigs(paramConfigDTOs);
            }

            // 设置节点状态，默认为 PENDING
            node.setNodeStatus(NodeStatus.PENDING);
            node.setProcessMetaSnapshot(processDTO.getProcessMetaSnapshot());
        } catch (Exception e) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "转换节点失败：" + e.getMessage());
        }

        return node;
    }

    private MTOProductSpecDTO.ProcessParamConfigDTO convertParamConfigToTypedObject(
            MTOProductSpecDTO.ProcessParamConfigDTO source) {
        if (source == null) {
            return null;
        }

        MTOProductSpecDTO.ProcessParamConfigDTO target = new MTOProductSpecDTO.ProcessParamConfigDTO();
        target.setCustomizable(source.isCustomizable());

        if (source.getParam() == null) {
            return target;
        }

        try {
            ProcessParam processParam = source.getParam().toDO();
            MTOProductSpecDTO.ProcessParamDTO typedParam = MTOProductSpecDTO.ProcessParamDTO.fromDO(processParam);
            target.setParam(typedParam);
            return target;
        } catch (Exception ex) {
            try {
                target.setParam(source.getParam());
                return target;
            } catch (Exception fallbackEx) {
                throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams,
                        "工艺参数对象转换失败: " + fallbackEx.getMessage());
            }
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
