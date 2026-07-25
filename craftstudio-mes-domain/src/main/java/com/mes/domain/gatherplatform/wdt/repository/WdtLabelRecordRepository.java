package com.mes.domain.gatherplatform.wdt.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.gatherplatform.wdt.entity.WdtLabelRecord;

/**
 * 旺店通快递面单记录仓储。
 */
public interface WdtLabelRecordRepository extends BaseRepository<WdtLabelRecord> {
    WdtLabelRecord findForOrder(String orderId, String channelOrderId, String logisticsOrderId);
}
