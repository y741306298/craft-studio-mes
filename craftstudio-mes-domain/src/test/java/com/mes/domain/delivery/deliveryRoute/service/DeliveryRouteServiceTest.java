package com.mes.domain.delivery.deliveryRoute.service;

import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecord;
import com.mes.domain.delivery.deliveryRoute.entity.AddressRecognitionRecordStatus;
import com.mes.domain.delivery.deliveryRoute.entity.DeliveryRoute;
import com.mes.domain.delivery.deliveryRoute.entity.RouteNode;
import com.mes.domain.delivery.deliveryRoute.repository.AddressRecognitionRecordRepository;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteNodeBindingRepository;
import com.mes.domain.delivery.deliveryRoute.repository.DeliveryRouteRepository;
import com.mes.domain.manufacturer.productionPiece.repository.ProductionPieceRepository;
import com.mes.domain.order.orderInfo.repository.OrderInfoRepository;
import com.mes.domain.order.orderInfo.repository.OrderItemRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryRouteServiceTest {

    private DeliveryRouteService service;
    private DeliveryRouteRepository routeRepository;
    private AddressRecognitionRecordRepository addressRecognitionRecordRepository;

    @BeforeEach
    void setUp() {
        service = new DeliveryRouteService();
        routeRepository = mock(DeliveryRouteRepository.class);
        ReflectionTestUtils.setField(service, "deliveryRouteRepository", routeRepository);
        ReflectionTestUtils.setField(service, "orderInfoRepository", mock(OrderInfoRepository.class));
        ReflectionTestUtils.setField(service, "orderItemRepository", mock(OrderItemRepository.class));
        ReflectionTestUtils.setField(service, "productionPieceRepository", mock(ProductionPieceRepository.class));
        addressRecognitionRecordRepository = mock(AddressRecognitionRecordRepository.class);
        ReflectionTestUtils.setField(service, "addressRecognitionRecordRepository", addressRecognitionRecordRepository);
        ReflectionTestUtils.setField(service, "deliveryRouteNodeBindingRepository", mock(DeliveryRouteNodeBindingRepository.class));
    }

    @Test
    void addRoutePersistsNodesSortedByUniqueOrder() {
        DeliveryRoute route = route(null, node(null, "第二站", 2), node(null, "第一站", 1));
        route.setRouteName("测试路线");
        route.setManufacturerMetaId("manufacturer-1");
        when(routeRepository.add(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryRoute saved = service.addDeliveryRoute(route);

        assertThat(saved.getRouteNodes()).extracting(RouteNode::getName).containsExactly("第一站", "第二站");
        assertThat(saved.getRouteNodes()).extracting(RouteNode::getNodeOrder).containsExactly(1, 2);
        assertThat(saved.getRouteNodes()).extracting(RouteNode::getId).containsExactly("2", "1");
    }

    @Test
    void updateChangesExistingNodeAndAllocatesIdForNewNode() {
        DeliveryRoute existing = route("route-1", node("1", "旧名称", 1));
        when(routeRepository.findById("route-1")).thenReturn(existing);
        DeliveryRoute update = route("route-1", node(null, "新节点", 2), node("1", "新名称", 1));
        update.setRouteName("测试路线");

        service.updateDeliveryRoute(update);

        ArgumentCaptor<DeliveryRoute> captor = ArgumentCaptor.forClass(DeliveryRoute.class);
        verify(routeRepository).update(captor.capture());
        assertThat(captor.getValue().getRouteNodes()).extracting(RouteNode::getId).containsExactly("1", "2");
        assertThat(captor.getValue().getRouteNodes()).extracting(RouteNode::getName).containsExactly("新名称", "新节点");
    }

    @Test
    void updateRejectsDuplicateNodeOrder() {
        DeliveryRoute existing = route("route-1", node("1", "第一站", 1));
        when(routeRepository.findById("route-1")).thenReturn(existing);
        DeliveryRoute update = route("route-1", node("1", "第一站", 1), node(null, "第二站", 1));
        update.setRouteName("测试路线");

        assertThatThrownBy(() -> service.updateDeliveryRoute(update))
                .isInstanceOf(BusinessNotAllowException.class)
                .hasMessageContaining("节点顺序不能重复");
    }

    @Test
    void removeNodeMovesSubsequentOrdersUp() {
        DeliveryRoute existing = route("route-1",
                node("1", "第一站", 1), node("2", "第二站", 2), node("3", "第三站", 3));
        when(routeRepository.findById("route-1")).thenReturn(existing);

        service.removeSimpleRouteNode("route-1", "2");

        ArgumentCaptor<DeliveryRoute> captor = ArgumentCaptor.forClass(DeliveryRoute.class);
        verify(routeRepository).update(captor.capture());
        assertThat(captor.getValue().getRouteNodes()).extracting(RouteNode::getId).containsExactly("1", "3");
        assertThat(captor.getValue().getRouteNodes()).extracting(RouteNode::getNodeOrder).containsExactly(1, 2);
    }

    @Test
    void migrationBackfillsOrderFromCurrentArrayPosition() {
        DeliveryRoute first = route("route-1", node("1", "第一站", null), node("2", "第二站", null));
        DeliveryRoute empty = route("route-2");
        when(routeRepository.listAll()).thenReturn(List.of(first, empty));

        int updated = service.migrateSimpleRouteNodeOrders();

        assertThat(updated).isEqualTo(1);
        assertThat(first.getRouteNodes()).extracting(RouteNode::getNodeOrder).containsExactly(1, 2);
        verify(routeRepository).batchUpdate(List.of(first));
    }

    @Test
    void batchBindLoadsAndUpdatesAddressRecognitionRecordsOnce() {
        AddressRecognitionRecord first = addressRecognitionRecord("record-1");
        AddressRecognitionRecord second = addressRecognitionRecord("record-2");
        List<String> recordIds = List.of("record-1", "record-2");
        when(addressRecognitionRecordRepository.findByIds(recordIds))
                .thenReturn(Map.of("record-1", first, "record-2", second));
        when(addressRecognitionRecordRepository.findMaxOrderByRouteNode("route-1", "node-1"))
                .thenReturn(4);

        service.bindAddressRecognitionRecords(recordIds, "route-1", "node-1", null);

        ArgumentCaptor<List<AddressRecognitionRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(addressRecognitionRecordRepository).findByIds(recordIds);
        verify(addressRecognitionRecordRepository).batchUpdate(captor.capture());
        verify(addressRecognitionRecordRepository, never()).findById(any());
        verify(addressRecognitionRecordRepository, never()).update(any());
        assertThat(captor.getValue()).containsExactly(first, second);
        assertThat(captor.getValue()).extracting(AddressRecognitionRecord::getRouteId)
                .containsExactly("route-1", "route-1");
        assertThat(captor.getValue()).extracting(AddressRecognitionRecord::getNodeId)
                .containsExactly("node-1", "node-1");
        assertThat(captor.getValue()).extracting(AddressRecognitionRecord::getOrder)
                .containsExactly(5, 6);
        assertThat(captor.getValue()).extracting(AddressRecognitionRecord::getStatus)
                .containsOnly(AddressRecognitionRecordStatus.ASSIGNED);
    }

    @Test
    void batchBindDoesNotUpdateWhenAnyAddressRecognitionRecordIsMissing() {
        AddressRecognitionRecord first = addressRecognitionRecord("record-1");
        List<String> recordIds = List.of("record-1", "missing");
        when(addressRecognitionRecordRepository.findByIds(recordIds))
                .thenReturn(Map.of("record-1", first));

        assertThatThrownBy(() -> service.bindAddressRecognitionRecords(recordIds, "route-1", "node-1", 1))
                .isInstanceOf(BusinessNotAllowException.class)
                .hasMessageContaining("地址识别记录不存在");

        verify(addressRecognitionRecordRepository, never()).batchUpdate(any());
        verify(addressRecognitionRecordRepository, never()).update(any());
    }

    private DeliveryRoute route(String id, RouteNode... nodes) {
        DeliveryRoute route = new DeliveryRoute();
        route.setId(id);
        route.setRouteNodes(new ArrayList<>(List.of(nodes)));
        return route;
    }

    private RouteNode node(String id, String name, Integer order) {
        RouteNode node = new RouteNode();
        node.setId(id);
        node.setName(name);
        node.setNodeOrder(order);
        return node;
    }

    private AddressRecognitionRecord addressRecognitionRecord(String id) {
        AddressRecognitionRecord record = new AddressRecognitionRecord();
        record.setId(id);
        return record;
    }
}
