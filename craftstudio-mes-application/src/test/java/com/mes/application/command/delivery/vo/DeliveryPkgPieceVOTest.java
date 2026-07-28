package com.mes.application.command.delivery.vo;

import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.mes.domain.order.enums.OrderChannelType;
import com.mes.domain.order.orderInfo.vo.OrderChannelInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class DeliveryPkgPieceVOTest {

    @Test
    void shouldCopyProductionPieceChannel() {
        OrderChannelInfo channel = new OrderChannelInfo();
        channel.setType(OrderChannelType.GATHER_PLATFORM);
        channel.setCode("test-platform");
        channel.setOrderId("platform-order-1");
        ProductionPiece piece = new ProductionPiece();
        piece.setChannel(channel);

        DeliveryPkgPieceVO result = DeliveryPkgPieceVO.fromProductionPiece(piece);

        assertSame(channel, result.getChannel());
    }
}
