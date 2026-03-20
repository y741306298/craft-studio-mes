package com.mes.application.command.procedure;

import com.mes.domain.manufacturer.procedure.entity.Procedure;
import com.mes.domain.manufacturer.procedure.repository.ProcedureRepository;
import com.mes.domain.manufacturer.procedure.service.ProcedureService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppProcedureService {

    @Autowired
    private ProcedureService domainProcedureService;

    @Autowired
    private ProcedureRepository procedureRepository;

    public PagedResult<Procedure> findProcedures(String procedureName, PagedQuery query) {
        // 参数验证
        if (query == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (query.getSize() <= 0 || query.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在 1-100 之间");
        }

        List<Procedure> items;
        long total;

        if (StringUtils.isBlank(procedureName)) {
            // 无条件分页查询
            items = procedureRepository.list(query.getCurrent(), query.getSize());
            total = procedureRepository.total();
        } else {
            // 根据名称模糊查询
            items = domainProcedureService.findProceduresByName(procedureName, (int) query.getCurrent(), query.getSize());
            total = domainProcedureService.getTotalCount(procedureName);
        }

        // 构建分页结果
        return new PagedResult<Procedure>(items, total, query.getSize(), query.getCurrent());
    }

    public void addProcedure(Procedure command) {
        if (command == null) {
            throw new IllegalArgumentException("工序不能为空");
        }
        domainProcedureService.addProcedure(command);
    }

    public void updateProcedure(Procedure command) {
        if (command == null) {
            throw new IllegalArgumentException("工序不能为空");
        }
        if (StringUtils.isBlank(command.getId())) {
            throw new IllegalArgumentException("工序ID不能为空");
        }
        domainProcedureService.updateProcedure(command);
    }

    public void deleteProcedure(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID不能为空");
        }
        domainProcedureService.deleteProcedure(id);
    }

    public Procedure findById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID不能为空");
        }
        return domainProcedureService.findById(id);
    }
}
