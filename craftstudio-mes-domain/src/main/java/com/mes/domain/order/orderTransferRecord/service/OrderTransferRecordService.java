package com.mes.domain.order.orderTransferRecord.service;

import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.order.orderTransferRecord.entity.OrderTransferRecord;
import com.mes.domain.order.orderTransferRecord.repository.OrderTransferRecordRepository;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class OrderTransferRecordService {

    @Autowired
    private OrderTransferRecordRepository orderTransferRecordRepository;

    public Collection<OrderTransferRecord> batchAdd(List<OrderTransferRecord> records) {
        if (records == null || records.isEmpty()) {
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "转出订单记录不能为空");
        }
        return orderTransferRecordRepository.batchAdd(records);
    }
}
