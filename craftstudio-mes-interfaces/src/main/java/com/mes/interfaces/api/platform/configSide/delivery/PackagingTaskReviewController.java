package com.mes.interfaces.api.platform.configSide.delivery;

import com.mes.application.command.delivery.AppDeliveryPkgService;
import com.mes.domain.base.repository.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.springframework.web.bind.annotation.*;

/** Operator-only reconciliation route under the existing config-side authentication interceptor. */
@RestController
@RequestMapping("/api/configSide/delivery-packaging-tasks")
public class PackagingTaskReviewController {
    private final AppDeliveryPkgService service;
    public PackagingTaskReviewController(AppDeliveryPkgService service) { this.service = service; }

    @PostMapping("/{taskId}/reconcile")
    public ApiResponse<Object> reconcile(@PathVariable String taskId, @RequestParam String manufacturerMetaId,
                                        @RequestParam String deliveryPkgId, @RequestParam boolean confirmedUpstreamAndPackage) {
        if (!confirmedUpstreamAndPackage) throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "请先核对上游结果和包裹数据");
        return ApiResponse.success(service.reconcilePackagingTask(taskId, manufacturerMetaId, deliveryPkgId));
    }

    /** Only after an operator has reconciled/corrected side effects; never cancels a live worker or reprints. */
    @PostMapping("/{taskId}/close-reviewed")
    public ApiResponse<Object> closeReviewed(@PathVariable String taskId, @RequestParam String manufacturerMetaId,
                                            @RequestParam String note, @RequestParam boolean confirmedReconciled) {
        if (!confirmedReconciled) throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "请先核对并处理任务的已有副作用");
        return ApiResponse.success(service.closeReviewedPackagingTask(taskId, manufacturerMetaId, note));
    }
}
