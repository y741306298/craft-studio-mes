package com.mes.infra.mq;

import com.piliofpala.craftstudio.shared.infra.mq.imp.rocketmq.RocketMqProducer;
import org.springframework.stereotype.Component;

@Component
public class LogisticsOrderProducer extends RocketMqProducer<LogisticsOrderInfo> {
}


