package com.mes.domain.manufacturer.typesetting.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import com.mes.domain.manufacturer.procedureFlow.vo.ProcessingFlowCondition;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface TypesettingRepository extends BaseRepository<TypesettingInfo> {
    void batchUpdateCallbackFailure(Collection<String> ids, String status, String remark);

    boolean compareAndSetPrintReport(String id, Integer expectedLeaveQuantity, int leaveQuantity,
                                     String status, String remark);

    List<TypesettingInfo> findPendingByConditions(String manufacturerMetaId, String materialName,
            List<ProcessingFlowCondition> processingNames, Date startTime, Date endTime, Boolean urgent, long offset, int size);

    long countPendingByConditions(String manufacturerMetaId, String materialName,
            List<ProcessingFlowCondition> processingNames, Date startTime, Date endTime, Boolean urgent);
}
