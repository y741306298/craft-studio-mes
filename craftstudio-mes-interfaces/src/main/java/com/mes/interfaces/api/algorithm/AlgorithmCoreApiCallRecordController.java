package com.mes.interfaces.api.algorithm;

import com.mes.application.command.api.AlgorithmCoreApiCallRecordQueryService;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.shared.algorithm.entity.AlgorithmCoreApiCallRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/algorithmCoreApiCallRecord")
public class AlgorithmCoreApiCallRecordController {
    private final AlgorithmCoreApiCallRecordQueryService queryService;

    public AlgorithmCoreApiCallRecordController(AlgorithmCoreApiCallRecordQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * 按调用类型和业务来源 ID 查询最近一条算法核心接口调用记录。
     */
    @GetMapping
    public ApiResponse<AlgorithmCoreApiCallRecord> findLatest(
            @RequestParam String type,
            @RequestParam String sourceId) {
        return ApiResponse.success(queryService.findLatest(type, sourceId));
    }
}
