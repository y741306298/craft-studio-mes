package com.mes.infra.dal.gatherplatform.wdt;

import com.mes.domain.gatherplatform.wdt.entity.WdtLabelRecord;
import com.mes.domain.gatherplatform.wdt.repository.WdtLabelRecordRepository;
import com.mes.infra.base.BaseRepositoryImp;
import com.mes.infra.dal.gatherplatform.wdt.po.WdtLabelRecordPO;
import org.springframework.stereotype.Repository;

/**
 * 基于 MongoDB 的旺店通快递面单记录仓储实现。
 */
@Repository
public class WdtLabelRecordRepositoryImp extends BaseRepositoryImp<WdtLabelRecord, WdtLabelRecordPO> implements WdtLabelRecordRepository {
    /**
     * 返回当前仓储对应的 MongoDB 持久化类型。
     */
    @Override
    public Class<WdtLabelRecordPO> poClass() {
        return WdtLabelRecordPO.class;
    }
}
