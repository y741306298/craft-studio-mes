package com.mes.interfaces.api.platform.manufacturerSide.typesetting;

import com.alibaba.fastjson.JSON;
import com.mes.application.command.typesetting.AppTypesettingService;
import com.mes.application.command.typesetting.enums.TypesettingSourceType;
import com.mes.application.command.typesetting.vo.*;
import com.mes.application.dto.TypesettingQuery;
import com.mes.application.dto.req.typesetting.ConfirmPrintRequest;
import com.mes.application.dto.req.typesetting.BatchConfirmLayoutRequest;
import com.mes.application.dto.req.typesetting.BatchConfirmPrintRequest;
import com.mes.application.dto.req.typesetting.GenerateQrCodeRequest;
import com.mes.application.dto.req.typesetting.GenerateTempCodeRequest;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.application.dto.req.typesetting.ReleaseLayoutRequest;
import com.mes.application.command.api.resp.NestingResponse;
import com.mes.application.command.api.resp.FormeGenerationResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/manufacturerSide/typesetting")
@RequiredArgsConstructor
public class TypesettingController {

    @Autowired
    private AppTypesettingService appTypesettingService;

    Logger logger = Logger.getLogger(TypesettingController.class.getName());

    private static final java.util.Set<String> PREPROCESS_NODE_NAMES = new java.util.HashSet<>(Arrays.asList("预处理", "待排版", "排版中", "待打包", "已打包"));

    /**
     * 统一查询排版和生产工件列表
     *
     * @param request 查询请求参数
     * @return 分页查询结果
     */
    @PostMapping("/list")
        public ApiResponse<TypesettingAndProductionPiecesResponse> listTypesettingAndProductionPieces(@RequestBody TypesettingQuery request) {
        
        PagedResult<TypesettingProductionPieceVO> result = 
                appTypesettingService.findTypesettingAndProductionPieces(request);
        
        List<TypesettingProductionPieceVO> items = (List<TypesettingProductionPieceVO>) result.items();
        sanitizeProcedureFlow(items);
        return ApiResponse.success(buildTypesettingAndProductionPiecesResponse(items));
    }

    @GetMapping("/list/condition")
    public ApiResponse<TypesettingAndProductionPiecesResponse> listByCondition(
            @RequestParam String manufacturerMetaId,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String processingName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false, defaultValue = "1") Integer current,
            @RequestParam(required = false, defaultValue = "50") Integer size) {
        TypesettingQuery query = new TypesettingQuery();
        query.setManufacturerMetaId(manufacturerMetaId);
        query.setMaterialName(materialName);
        query.setProcessingName(processingName);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setSourceType(sourceType);
        query.setCurrent(current);
        query.setSize(size);
        PagedResult<TypesettingProductionPieceVO> result = appTypesettingService.findTypesettingAndProductionPieces(query);
        List<TypesettingProductionPieceVO> items = new ArrayList<>((List<TypesettingProductionPieceVO>) result.items());
        sanitizeProcedureFlow(items);
        return ApiResponse.success(buildTypesettingAndProductionPiecesResponse(items));
    }

    private TypesettingAndProductionPiecesResponse buildTypesettingAndProductionPiecesResponse(List<TypesettingProductionPieceVO> items) {
        List<String> processingFlowList = buildProcessingFlowList(items);
        List<String> materialList = buildMaterialList(items);
        List<TypesettingAndProductionPiecesResponse.SourceTypeOption> sourceType = buildSourceTypeList();
        return new TypesettingAndProductionPiecesResponse(items, processingFlowList, materialList, sourceType);
    }

    private List<String> buildProcessingFlowList(List<TypesettingProductionPieceVO> items) {
        return items.stream()
                .filter(Objects::nonNull)
                .map(TypesettingProductionPieceVO::getProcedureFlow)
                .filter(Objects::nonNull)
                .map(ProcedureFlow::getNodes)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(ProcedureFlowNode::getNodeName)
                .filter(Objects::nonNull)
                .filter(nodeName -> !nodeName.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
    }

    private List<String> buildMaterialList(List<TypesettingProductionPieceVO> items) {
        return items.stream()
                .filter(Objects::nonNull)
                .map(TypesettingProductionPieceVO::getMaterialConfig)
                .filter(Objects::nonNull)
                .map(materialConfig -> materialConfig.getMaterialSnapshot())
                .filter(Objects::nonNull)
                .map(materialSnapshot -> materialSnapshot.getName())
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
    }

    private List<TypesettingAndProductionPiecesResponse.SourceTypeOption> buildSourceTypeList() {
        return Arrays.stream(TypesettingSourceType.values())
                .map(type -> new TypesettingAndProductionPiecesResponse.SourceTypeOption(type.getCode(), type.getDescription()))
                .collect(Collectors.toList());
    }



    private void sanitizeProcedureFlow(List<TypesettingProductionPieceVO> items) {
        if (items == null) {
            return;
        }
        for (TypesettingProductionPieceVO item : items) {
            ProcedureFlow flow = item.getProcedureFlow();
            if (flow == null || flow.getNodes() == null) {
                continue;
            }
            List<ProcedureFlowNode> filteredNodes = flow.getNodes().stream()
                    .filter(Objects::nonNull)
                    .filter(node -> !PREPROCESS_NODE_NAMES.contains(node.getNodeName()))
                    .collect(Collectors.toList());
            flow.setNodes(filteredNodes);
            flow.setTotalNodes(filteredNodes.size());
            item.setProcedureFlow(flow);
        }
    }

    /**
     * 查询状态为待确认（confirming）的排版信息列表（分页）
     *
     * @param manufacturerMetaId 厂商元数据ID
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认20，最大100）
     * @return 分页查询结果
     */
    @GetMapping("/confirming/list")
    public ApiResponse<PagedResult<TypesettingInfo>> listConfirmingTypesetting(
            @RequestParam String manufacturerMetaId,
            @RequestParam(required = false) String typesettingId,
            @RequestParam(required = false, defaultValue = "1") int current,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        PagedResult<TypesettingInfo> result = 
                appTypesettingService.findConfirmingTypesetting(manufacturerMetaId, typesettingId, current, size);
        
        return ApiResponse.success(result);
    }

    /**
     * 开始排版：校验材料和工艺，并更新生产工件状态
     *
     * @param request 确认排版请求，包含生产工件 ID 列表和排版 API 地址
     * @return 排版结果
     */
    @PostMapping("/toLayout")
    public ApiResponse<LayoutConfirmResult> toLayout(@Valid @RequestBody LayoutConfirmRequest request) {
        LayoutConfirmResult result = appTypesettingService.toLayout(request);
        if (!result.isSuccess()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, result.getMessage());
        }

        return ApiResponse.success(result);
    }

    /**
     * 确认排版。
     *
     * <p>入参直接使用 TypesettingInfo（至少包含 id，layoutMode 可选覆盖）。
     * 服务层会按 id 读取最新排版记录并构建 Forme 生成请求。
     */
    @PostMapping("/confirmLayout")
        public ApiResponse<LayoutConfirmResult> confirmLayout(@RequestBody TypesettingInfo request) {
        LayoutConfirmResult result = appTypesettingService.confirmLayout(request);
        if (!result.isSuccess()) {
            ApiResponse<LayoutConfirmResult> failResponse = new ApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage(result.getMessage());
            return failResponse;
        }
        return ApiResponse.success(result);
    }


    @PostMapping("/confirmLayout/batch")
    public ApiResponse<LayoutConfirmResult> batchConfirmLayout(@RequestBody BatchConfirmLayoutRequest request) {
        LayoutConfirmResult result = appTypesettingService.batchConfirmLayout(request);
        if (!result.isSuccess()) {
            ApiResponse<LayoutConfirmResult> failResponse = new ApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage(result.getMessage());
            return failResponse;
        }
        return ApiResponse.success(result);
    }

    /**
     * 确认打印：将排版数据根据状态机改为待打印状态
     *
     * @param request 确认打印请求，包含排版ID、设备编号
     * @return 操作结果
     */
    @PostMapping("/confirmPrint")
    public ApiResponse<ConfirmPrintResult> confirmPrint(@Valid @RequestBody ConfirmPrintRequest request) {
        if (request.getId() == null || request.getId().isBlank()) {
            ApiResponse<ConfirmPrintResult> failResponse = new ApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage("排版ID不能为空");
            return failResponse;
        }
        if (request.getDeviceCode() == null || request.getDeviceCode().isBlank()) {
            ApiResponse<ConfirmPrintResult> failResponse = new ApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage("设备编号不能为空");
            return failResponse;
        }
        ConfirmPrintResult result = appTypesettingService.confirmPrint(request);
        return ApiResponse.success(result);
    }


    @PostMapping("/confirmPrint/batch")
    public ApiResponse<ConfirmPrintResult> batchConfirmPrint(@Valid @RequestBody BatchConfirmPrintRequest request) {
        ConfirmPrintResult result = appTypesettingService.batchConfirmPrint(request);
        return ApiResponse.success(result);
    }

    /**
     * 开始打印：将排版数据根据状态机改为打印中状态
     *
     * @param request 开始打印请求，包含生产工件 ID 列表
     * @return 操作结果
     */
    @PostMapping("/startPrint")
    public ApiResponse<ConfirmPrintResult> startPrint(@Valid @RequestBody ConfirmPrintRequest request) {
        
        if (request.getProductionPieceIds() == null || request.getProductionPieceIds().isEmpty()) {
            ApiResponse<ConfirmPrintResult> failResponse = new ApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage("生产工件 ID 列表不能为空");
            return failResponse;
        }
        
        ConfirmPrintResult result = appTypesettingService.startPrint(request.getProductionPieceIds());
        
        return ApiResponse.success(result);
    }

    /**
     * 释放排版：删除排版文件，将参与的零件状态改回待排版状态
     *
     * @param request 释放排版请求，包含排版 ID 列表
     * @return 操作结果
     */
    @PostMapping("/releaseLayout")
    public ApiResponse<ReleaseLayoutResult> releaseLayout(@Valid @RequestBody ReleaseLayoutRequest request) {
        
        if (request.getIdList() == null || request.getIdList().isEmpty()) {
            ApiResponse<ReleaseLayoutResult> failResponse = new ApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage("排版 ID 列表不能为空");
            return failResponse;
        }
        
        ReleaseLayoutResult result = appTypesettingService.releaseLayout(request.getIdList());
        
        return ApiResponse.success(result);
    }

    /**
     * 生成二维码（Base64）
     */
    @PostMapping("/generateQrCode")
    public ApiResponse<GenerateQrCodeResult> generateQrCode(@RequestBody GenerateQrCodeRequest request) {
        return ApiResponse.success(appTypesettingService.generateQrCode(request));
    }

    /**
     * 生成临时码（循环数字）
     */
    @PostMapping("/generateTempCode")
    public ApiResponse<GenerateTempCodeResult> generateTempCode(@RequestBody GenerateTempCodeRequest request) {
        return ApiResponse.success(appTypesettingService.generateTempCode(request));
    }

    /**
     * 查询所有排版方式枚举
     */
    @GetMapping("/layoutModes")
    public ApiResponse<List<TypesettingLayoutModeVO>> listLayoutModes() {
        return ApiResponse.success(appTypesettingService.listLayoutModes());
    }

    /**
     * 查询默认排版规格
     */
    @GetMapping("/layoutSpecs")
    public ApiResponse<List<TypesettingLayoutSpecVO>> listLayoutSpecs() {
        return ApiResponse.success(appTypesettingService.listDefaultLayoutSpecs());
    }

    /**
     * 异形排版算法异步回调
     */
    @PostMapping("/callback/generate_nested_files")
    public ApiResponse<String> handleGenerateNestedFilesCallback(@RequestBody NestingResponse response) {
        logger.info("========== handleGenerateNestedFilesCallback 入参开始 ==========");
        logger.info("response: " + JSON.toJSONString(response));
        logger.info("========== handleGenerateNestedFilesCallback 入参结束 ==========");
        appTypesettingService.handleNestingCallback(response);
        return ApiResponse.success("回调处理成功");
    }

    /**
     * 网格排版算法异步回调
     */
    @PostMapping("/callback/generate_grid_nested_files")
    public ApiResponse<String> handleGenerateGridNestedFilesCallback(@RequestBody NestingResponse response) {
        logger.info("========== handleGenerateNestedFilesCallback 入参开始 ==========");
        logger.info("response: " + JSON.toJSONString(response));
        logger.info("========== handleGenerateNestedFilesCallback 入参结束 ==========");
        appTypesettingService.handleNestingCallback(response);
        return ApiResponse.success("回调处理成功");
    }

    /**
     * 版式生成算法异步回调
     */
    @PostMapping("/callback/generate_forme")
    public ApiResponse<String> handleGenerateFormeCallback(@RequestBody FormeGenerationResponse response) {
        logger.info("========== handleGenerateFormeCallback 入参开始 ==========");
        logger.info("response: " + JSON.toJSONString(response));
        logger.info("========== handleGenerateFormeCallback 入参结束 ==========");
        appTypesettingService.handleGenerateFormeCallback(response);
        return ApiResponse.success("回调处理成功");
    }


}
