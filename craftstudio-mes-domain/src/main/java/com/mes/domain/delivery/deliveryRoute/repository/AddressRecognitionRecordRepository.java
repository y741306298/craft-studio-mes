package com.mes.domain.delivery.deliveryRoute.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;

public interface AddressRecognitionRecordRepository extends BaseRepository<AddressRecognitionRecord> {

    AddressRecognitionRecord findByAddress(String terminalRegionCode, String detailAddress);
}
