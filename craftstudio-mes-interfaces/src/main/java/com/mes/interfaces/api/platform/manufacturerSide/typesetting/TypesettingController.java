package com.mes.interfaces.api.platform.manufacturerSide.typesetting;

import com.alibaba.fastjson.JSON;
import com.mes.application.command.typesetting.AppTypesettingService;
import com.mes.application.command.typesetting.vo.*;
import com.mes.application.dto.TypesettingQuery;
import com.mes.application.dto.req.typesetting.ConfirmPrintRequest;
import com.mes.application.dto.req.typesetting.GenerateQrCodeRequest;
import com.mes.application.dto.req.typesetting.GenerateTempCodeRequest;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.application.dto.req.typesetting.ReleaseLayoutRequest;
import com.mes.application.command.api.resp.NestingResponse;
import com.mes.application.command.api.resp.FormeGenerationResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
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

    /**
     * 统一查询排版和生产工件列表
     *
     * @param request 查询请求参数
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public ApiResponse<List<TypesettingProductionPieceVO>> listTypesettingAndProductionPieces(@RequestBody TypesettingQuery request) {
        
        PagedResult<TypesettingProductionPieceVO> result = 
                appTypesettingService.findTypesettingAndProductionPieces(request);
        
        return ApiResponse.success((List<TypesettingProductionPieceVO>) result.items());
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
            @RequestParam(required = false, defaultValue = "1") int current,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        PagedResult<TypesettingInfo> result = 
                appTypesettingService.findConfirmingTypesetting(manufacturerMetaId, current, size);
        
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
            ApiResponse<LayoutConfirmResult> failResponse = new ApiResponse<>();
            failResponse.setMessage(result.getMessage());
            return failResponse;
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
        if (request.getDeviceInfoId() == null || request.getDeviceInfoId().isBlank()) {
            ApiResponse<ConfirmPrintResult> failResponse = new ApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage("设备编号不能为空");
            return failResponse;
        }
        ConfirmPrintResult result = appTypesettingService.confirmPrint(request);
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
        
        if (request.getTypesettingIds() == null || request.getTypesettingIds().isEmpty()) {
            ApiResponse<ReleaseLayoutResult> failResponse = new ApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage("排版 ID 列表不能为空");
            return failResponse;
        }
        
        ReleaseLayoutResult result = appTypesettingService.releaseLayout(request.getTypesettingIds());
        
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
     * 获取全部排版方式配置
     */
    @GetMapping("/layoutModes")
    public ApiResponse<List<TypesettingLayoutModeVO>> listLayoutModes() {
        List<TypesettingLayoutModeVO> modes = Arrays.stream(TypesettingLayoutMode.values())
                .map(TypesettingLayoutModeVO::from)
                .collect(Collectors.toList());
        return ApiResponse.success(modes);
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
        return ApiResponse.success("回调处理待续");
    }
}
