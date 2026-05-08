package com.mes.interfaces.api.platform.manufacturerSide.print;

import com.mes.application.command.print.AppPrintService;
import com.mes.application.command.print.vo.PendingPrintTypesettingVO;
import com.mes.application.command.print.vo.PrintReportResult;
import com.mes.application.dto.req.typesetting.ReleaseLayoutRequest;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/manufacturerSide/print")
@RequiredArgsConstructor
public class PrintController {

    private final AppPrintService appPrintService;

    /**
     * 查询状态为"待打印"的排版信息（分页）
     */
    @GetMapping("/pending/list")
    public ApiResponse<PagedResult<PendingPrintTypesettingVO>> listPendingPrintTypesetting(
            @RequestParam String manufacturerMetaId,
            @RequestParam(required = false) String id,
            @RequestParam(required = false, defaultValue = "1") int current,
            @RequestParam(required = false, defaultValue = "20") int size) {
        log.info("listPendingPrintTypesetting: manufacturerMetaId={}, id={}, current={}, size={}", manufacturerMetaId, id, current, size);
        return ApiResponse.success(appPrintService.findPendingPrintTypesetting(manufacturerMetaId, id, current, size));
    }

    /**
     * 根据ID获取打印任务数据
     */
    @GetMapping("/task/getById")
    public ApiResponse<TypesettingPrintTask> getPrintTaskById(@RequestParam String id) {
        return ApiResponse.success(appPrintService.findPrintTaskById(id));
    }

    /**
     * 打印报备。
     * 1. remark 有值时更新排版备注；
     * 2. 使用入参 quantity 扣减印版 leaveQuantity，并判断是否可报备完成；
     * 3. 若可完成，将该印版关联生产工件从"打印中"节点数量划转到"待打包"节点。
     */
    @PostMapping("/report")
    public ApiResponse<PrintReportResult> report(@RequestBody TypesettingInfo request) {
        return ApiResponse.success(appPrintService.reportPrinting(request));
    }

    /**
     * 释放排版：删除排版信息，并将“待打印”节点数量回退到“待排版”节点。
     */
    @PostMapping("/releaseLayout")
    public ApiResponse<Boolean> releaseLayout(@RequestBody ReleaseLayoutRequest request) {
        appPrintService.releaseLayout(request.getIdList());
        return ApiResponse.success(true);
    }

    /**
     * 重做：直接增加排版 leaveQuantity。
     */
    @PostMapping("/redo")
    public ApiResponse<Boolean> redo(@RequestBody TypesettingInfo request) {
        appPrintService.redo(request);
        return ApiResponse.success(true);
    }
}
