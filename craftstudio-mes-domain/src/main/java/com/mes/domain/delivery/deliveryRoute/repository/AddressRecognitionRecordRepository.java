package com.mes.domain.delivery.deliveryRoute.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;

import java.util.List;

public interface AddressRecognitionRecordRepository extends BaseRepository<AddressRecognitionRecord> {

    AddressRecognitionRecord findByAddress(String terminalRegionCode, String detailAddress);

    List<AddressRecognitionRecord> listByStatus(String status, long current, int size);

    long totalByStatus(String status);
}
