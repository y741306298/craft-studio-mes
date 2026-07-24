package com.mes.domain.order.preOrderLabelTask.service;

import com.mes.domain.order.orderInfo.entity.OrderInfo;
import com.mes.domain.order.preOrderLabelTask.entity.PreOrderLabelTask;
import com.mes.domain.order.preOrderLabelTask.enums.PreOrderLabelTaskStatus;
import com.mes.domain.order.preOrderLabelTask.repository.PreOrderLabelTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PreOrderLabelTaskService {
    private static final int DEFAULT_PENDING_PAGE_SIZE = 100;

    @Autowired
    private PreOrderLabelTaskRepository preOrderLabelTaskRepository;

    public PreOrderLabelTask createFromOrderInfo(OrderInfo orderInfo) {
        if (orderInfo == null) {
            return null;
        }
        PreOrderLabelTask task = new PreOrderLabelTask();
        task.setOrderId(orderInfo.getOrderId());
        task.setChannel(orderInfo.getChannel());
        task.setStatus(PreOrderLabelTaskStatus.PENDING);
        task.setLogisticsCarrierInfo(orderInfo.getLogisticsCarrierInfo());
        return preOrderLabelTaskRepository.add(task);
    }

    public List<PreOrderLabelTask> findPendingTasks() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("status", PreOrderLabelTaskStatus.PENDING.getCode());
        return preOrderLabelTaskRepository.filterList(1, DEFAULT_PENDING_PAGE_SIZE, filters);
    }

    public void markProcessed(PreOrderLabelTask task, String kuaidiNum) {
        if (task == null) {
            return;
        }
        task.setStatus(PreOrderLabelTaskStatus.PROCESSED);
        task.setKuaidiNum(kuaidiNum);
        preOrderLabelTaskRepository.update(task);
    }
}
