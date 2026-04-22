package com.mes.domain.manufacturer.typesetting.service;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingPrintTask;
import com.mes.domain.manufacturer.typesetting.repository.TypesettingPrintTaskRepository;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class TypesettingPrintTaskService {

    @Autowired
    private TypesettingPrintTaskRepository typesettingPrintTaskRepository;

    public TypesettingPrintTask findByTypesettingInfoId(String typesettingInfoId) {
        if (StringUtils.isBlank(typesettingInfoId)) {
            return null;
        }
        Map<String, String> filters = Collections.singletonMap("typesettingInfoId", typesettingInfoId);
        List<TypesettingPrintTask> taskList = typesettingPrintTaskRepository.fuzzySearch(filters, 1, 1);
        return taskList.isEmpty() ? null : taskList.get(0);
    }

    public void saveOrUpdate(TypesettingPrintTask task) {
        if (task == null || StringUtils.isBlank(task.getTypesettingInfoId())) {
            throw new IllegalArgumentException("打印任务不能为空，且必须包含 typesettingInfoId");
        }
        TypesettingPrintTask exist = findByTypesettingInfoId(task.getTypesettingInfoId());
        if (exist == null) {
            typesettingPrintTaskRepository.add(task);
            return;
        }
        task.setId(exist.getId());
        typesettingPrintTaskRepository.update(task);
    }
}
