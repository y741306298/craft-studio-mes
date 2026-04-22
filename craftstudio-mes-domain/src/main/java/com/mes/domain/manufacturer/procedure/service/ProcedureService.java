package com.mes.domain.manufacturer.procedure.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.procedure.entity.Procedure;
import com.mes.domain.manufacturer.procedure.repository.ProcedureRepository;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.mes.domain.shared.utils.IdGenerator;
import com.piliofpala.craftstudio.shared.domain.file.vo.File;
import com.piliofpala.craftstudio.shared.domain.file.vo.FilePreview;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProcedureService {

    @Autowired
    private ProcedureRepository procedureRepository;

    @Value("${ali-cloud.oss.endpoint:oss-cn-hangzhou.aliyuncs.com}")
    private String ossEndpoint;

    @Value("${ali-cloud.oss.raw-bucket:craftstudio-mes-test}")
    private String ossBucket;

    /**
     * 根据工序名称查询工序（支持分页）
     * @param procedureName 工序名称
     * @param current 当前页码
     * @param size 每页大小
     * @return 工序列表
     */
    public List<Procedure> findProceduresByName(String procedureName, int current, int size) {
        // 参数验证

        if (size <= 0 || size > 100) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "每页大小必须在1-100之间");
        }
        if (StringUtils.isBlank(procedureName)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序名称不能为空");
        }

        // 根据procedureName进行模糊查询
        Map<String, String> searchFilters = new HashMap<>();
        searchFilters.put("procedureName", procedureName);
        return procedureRepository.fuzzySearch(searchFilters, current, size);
    }

    /**
     * 获取工序总数
     * @param procedureName 工序名称，可为空
     * @return 总数
     */
    public long getTotalCount(String procedureName) {
        if (StringUtils.isNotBlank(procedureName)) {
            Map<String, String> searchFilters = new HashMap<>();
            searchFilters.put("procedureName", procedureName);
            return procedureRepository.totalByFuzzySearch(searchFilters);
        } else {
            return procedureRepository.total();
        }
    }

    /**
     * 添加工序
     * @param procedure 工序实体
     * @return 添加后的实体
     */
    public Procedure addProcedure(Procedure procedure) {
        // 业务验证
        if (procedure == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序不能为空");
        }
        if (StringUtils.isBlank(procedure.getProcedureName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序名称不能为空");
        }

        // 生成唯一的 procedureId
        String procedureId = IdGenerator.generateId("PROC");
        procedure.setProcedureId(procedureId);

        return procedureRepository.add(procedure);
    }

    /**
     * 更新工序
     * @param procedure 工序实体
     */
    public void updateProcedure(Procedure procedure) {
        // 业务验证
        if (procedure == null) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序不能为空");
        }
        if (StringUtils.isBlank(procedure.getId())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序ID不能为空");
        }
        if (StringUtils.isBlank(procedure.getProcedureName())) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "工序名称不能为空");
        }

        procedureRepository.update(procedure);
    }

    /**
     * 删除工序
     * @param id 工序ID
     */
    public void deleteProcedure(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }

        Procedure procedure = procedureRepository.findById(id);
        if (procedure != null) {
            procedureRepository.delete(procedure);
        }
    }

    /**
     * 根据ID获取工序
     * @param id 工序ID
     * @return 工序实体
     */
    public Procedure findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "ID不能为空");
        }
        return procedureRepository.findById(id);
    }

    /**
     * 原有的 add 方法保持兼容性
     */
    public Procedure add(Procedure procedure) {
        return addProcedure(procedure);
    }

    /**
     * 处理裁切工艺：生成切割坐标
     * @param processingNodes 工艺流程节点列表
     * @return 切割坐标列表 [x1, y1, x2, y2, ...]
     */
    public List<Double> generateCuttingCoordinates(List<ProcedureFlowNode> processingNodes) {
        // TODO: 根据实际的裁切工艺逻辑生成切割坐标
        // 这里暂时返回示例坐标
        return Arrays.asList(
                0.0, 0.0,      // 起点 (x1, y1)
                1000.0, 1000.0 // 终点 (x2, y2)
        );
    }


    /**
     * 创建生产工件实体
     * @param orderItem 订单项
     * @param pieceType 工件类型
     * @param imageUrl 图片 URL
     * @return 生产工件实体
     */
    public ProductionPiece createProductionPiece(OrderItem orderItem, String pieceType,  String imageUrl,ProcedureFlow procedureFlow,String maskUrl) {
        if (orderItem == null) {
            throw new IllegalArgumentException("订单项不能为空");
        }

        ProductionPiece piece = new ProductionPiece();
        piece.setOrderItemId(orderItem.getOrderItemId());

        if (orderItem.getProcedureFlow() != null) {
            piece.setProcedureFlow(orderItem.getProcedureFlow());
            piece.setProcedureFlowId(orderItem.getProcedureFlow().getProcedureFlowId());
        }

        piece.setProductionPieceType(pieceType);
        piece.setStatus(ProductionPieceStatus.PROCESSING.getCode());
        piece.setQuantity(orderItem.getQuantity());
        piece.setTemplateCode(imageUrl);
        piece.setCarrierId(orderItem.getLogisticsCarrierInfo().getCarrierId());
        piece.setMaterialConfig(orderItem.getMaterial());
        piece.setProcessingFlow(orderItem.getProcessingFlow());
        piece.setManufacturerId(orderItem.getManufacturerId());
        piece.setProcedureFlow(procedureFlow);
        
        String completeImageUrl = buildCompleteOssUrl(imageUrl);
        ImageFile imageFile = new ImageFile();
        imageFile.setRawFile(completeImageUrl);
        FilePreview filePreview = new FilePreview();
        filePreview.setPreview(completeImageUrl);
        filePreview.setRaw(completeImageUrl);
        filePreview.setThumbnail(completeImageUrl);
        imageFile.setFilePreview(filePreview);
        imageFile.setRawFile(completeImageUrl);
        piece.setProductImageFile(imageFile);
        
        if (StringUtils.isNotBlank(maskUrl)){
            String completeMaskUrl = buildCompleteOssUrl(maskUrl);
            ImageFile maskFile = new ImageFile();
            maskFile.setRawFile(completeMaskUrl);
            FilePreview maskPreview = new FilePreview();
            maskPreview.setPreview(completeMaskUrl);
            maskPreview.setRaw(completeMaskUrl);
            maskPreview.setThumbnail(completeMaskUrl);
            maskFile.setFilePreview(maskPreview);
            maskFile.setRawFile(completeMaskUrl);
            piece.setMaskImageFile(maskFile);
        }

        piece.getProcedureFlow().getNodes().forEach(node -> node.setPieceQuantity(0));
        piece.getProcedureFlow().getNodes().get(0).setPieceQuantity(orderItem.getQuantity());
        return piece;
    }

    /**
     * 构建完整的 OSS URL
     * 如果 URL 已经是完整路径（包含 endpoint），则直接返回
     * 如果只有相对路径，则补充完整的 OSS 地址
     *
     * @param url 原始 URL 或路径
     * @return 完整的 OSS URL
     */
    private String buildCompleteOssUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        
        url = url.trim();
        
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        
        if (url.startsWith("/")) {
            url = url.substring(1);
        }
        
        return "https://" + ossBucket + "." + ossEndpoint + "/" + url;
    }
}
