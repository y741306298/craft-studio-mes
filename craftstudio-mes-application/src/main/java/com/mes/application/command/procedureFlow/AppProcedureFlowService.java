package com.mes.application.command.procedureFlow;

import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.repository.ProcedureFlowRepository;
import com.mes.domain.manufacturer.procedureFlow.service.ProcedureFlowService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppProcedureFlowService {

    @Autowired
    private ProcedureFlowService domainProcedureFlowService;

    @Autowired
    private ProcedureFlowRepository procedureFlowRepository;

    public PagedResult<ProcedureFlow> findProcedureFlows(String procedureFlowName, PagedQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        List<ProcedureFlow> items;
        long total;

        if (StringUtils.isBlank(procedureFlowName)) {
            items = procedureFlowRepository.list(query.getCurrent(), query.getSize());
            total = procedureFlowRepository.total();
        } else {
            items = domainProcedureFlowService.findProcedureFlowsByName(procedureFlowName, (int) query.getCurrent(), query.getSize());
            total = domainProcedureFlowService.getTotalCount(procedureFlowName);
        }

        return new PagedResult<ProcedureFlow>(items, total, query.getSize(), query.getCurrent());
    }

    public void addProcedureFlow(ProcedureFlow command) {
        if (command == null) {
            throw new IllegalArgumentException("工序流程不能为空");
        }
        domainProcedureFlowService.addProcedureFlow(command);
    }

    public void updateProcedureFlow(ProcedureFlow command) {
        if (command == null) {
            throw new IllegalArgumentException("工序流程不能为空");
        }
        if (StringUtils.isBlank(command.getId())) {
            throw new IllegalArgumentException("工序流程 ID 不能为空");
        }
        domainProcedureFlowService.updateProcedureFlow(command);
    }

    public void deleteProcedureFlow(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        domainProcedureFlowService.deleteProcedureFlow(id);
    }

    public ProcedureFlow findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        return domainProcedureFlowService.findById(id);
    }
}
