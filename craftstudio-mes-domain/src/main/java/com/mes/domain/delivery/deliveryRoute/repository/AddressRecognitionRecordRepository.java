package com.mes.domain.delivery.deliveryRoute.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;

import java.util.List;

public interface AddressRecognitionRecordRepository extends BaseRepository<AddressRecognitionRecord> {

    AddressRecognitionRecord findByAddress(String manufacturerMetaId, String terminalRegionCode, String detailAddress);

    List<AddressRecognitionRecord> listByStatus(String status, String manufacturerMetaId, String detailAddress, long current, int size);

    long totalByStatus(String status, String manufacturerMetaId, String detailAddress);

    List<AddressRecognitionRecord> listAssignedByRoute(String routeId, long current, int size);

    List<AddressRecognitionRecord> listAssignedByRouteNode(String manufacturerMetaId, String routeId, String nodeId, String detailAddress, long current, int size);

    long totalAssignedByRouteNode(String manufacturerMetaId, String routeId, String nodeId, String detailAddress);

    Integer findMaxOrderByRouteNode(String routeId, String nodeId);
}
