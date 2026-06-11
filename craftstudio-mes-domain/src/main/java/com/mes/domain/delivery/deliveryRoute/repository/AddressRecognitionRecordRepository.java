package com.mes.domain.delivery.deliveryRoute.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;

import java.util.List;

public interface AddressRecognitionRecordRepository extends BaseRepository<AddressRecognitionRecord> {

    AddressRecognitionRecord findByAddress(String terminalRegionCode, String detailAddress);

    List<AddressRecognitionRecord> listByStatus(String status, String detailAddress, long current, int size);

    long totalByStatus(String status, String detailAddress);

    List<AddressRecognitionRecord> listAssignedByRouteNode(String routeId, String nodeId, String detailAddress, long current, int size);

    long totalAssignedByRouteNode(String routeId, String nodeId, String detailAddress);
}
