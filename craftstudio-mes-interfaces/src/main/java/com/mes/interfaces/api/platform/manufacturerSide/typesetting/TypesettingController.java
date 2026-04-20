package com.mes.interfaces.api.platform.manufacturerSide.typesetting;

import com.mes.application.command.typesetting.AppTypesettingService;
import com.mes.application.command.typesetting.vo.*;
import com.mes.application.dto.TypesettingQuery;
import com.mes.application.dto.req.typesetting.ConfirmPrintRequest;
import com.mes.application.dto.req.typesetting.LayoutConfirmRequest;
import com.mes.application.dto.req.typesetting.ReleaseLayoutRequest;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.application.dto.resp.PagedApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manufacturerSide/typesetting")
@RequiredArgsConstructor
public class TypesettingController {

    @Autowired
    private AppTypesettingService appTypesettingService;

    /**
     * 统一查询排版和生产工件列表
     * @param request 查询请求参数
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<TypesettingProductionPieceVO> listTypesettingAndProductionPieces(
            @Valid @RequestBody TypesettingQuery request) {
        
        PagedResult<TypesettingProductionPieceVO> result = 
                appTypesettingService.findTypesettingAndProductionPieces(request);
        
        return PagedApiResponse.success((List<TypesettingProductionPieceVO>) result.items(), request.getPagedQuery().getCurrent(), request.getPagedQuery().getSize(), result.total());
    }

    /**
     * 开始排版：校验材料和工艺，并更新生产工件状态
     *
     * @param request 确认排版请求，包含生产工件 ID 列表和排版 API 地址
     * @return 排版结果
     */
    @PostMapping("/confirmLayout")
    public ApiResponse<LayoutConfirmResult> confirmLayout(@Valid @RequestBody LayoutConfirmRequest request) {
        LayoutConfirmResult result = appTypesettingService.confirmLayout(request);
        if (!result.isSuccess()) {
            ApiResponse<LayoutConfirmResult> failResponse = new ApiResponse<>();
            failResponse.setMessage(result.getMessage());
            return failResponse;
        }
        
        return ApiResponse.success(result);
    }

    /**
     * 确认打印：将排版数据根据状态机改为待打印状态
     *
     * @param request 确认打印请求，包含生产工件 ID 列表
     * @return 操作结果
     */
    @PostMapping("/confirmPrint")
    public ApiResponse<ConfirmPrintResult> confirmPrint(@Valid @RequestBody ConfirmPrintRequest request) {
        
        if (request.getProductionPieceIds() == null || request.getProductionPieceIds().isEmpty()) {
            ApiResponse<ConfirmPrintResult> failResponse = new ApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage("生产工件 ID 列表不能为空");
            return failResponse;
        }
        
        ConfirmPrintResult result = appTypesettingService.confirmPrint(request.getProductionPieceIds());
        
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
     * @param request 释放排版请求，包含生产工件 ID 列表
     * @return 操作结果
     */
    @PostMapping("/releaseLayout")
    public ApiResponse<ReleaseLayoutResult> releaseLayout(@Valid @RequestBody ReleaseLayoutRequest request) {
        
        if (request.getProductionPieceIds() == null || request.getProductionPieceIds().isEmpty()) {
            ApiResponse<ReleaseLayoutResult> failResponse = new ApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage("生产工件 ID 列表不能为空");
            return failResponse;
        }
        
        ReleaseLayoutResult result = appTypesettingService.releaseLayout(request.getProductionPieceIds());
        
        return ApiResponse.success(result);
    }
}
