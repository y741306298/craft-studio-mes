package com.mes.domain.delivery.packagingBox.service;

import com.mes.domain.delivery.packagingBox.entity.PackagingBox;
import com.mes.domain.delivery.packagingBox.repository.PackagingBoxRepository;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Service
public class PackagingBoxService {
    private final PackagingBoxRepository repository;

    public PackagingBoxService(PackagingBoxRepository repository) {
        this.repository = repository;
    }

    public PackagingBox add(PackagingBox box) {
        validate(box);
        box.setId(null);
        box.setCreateTime(null);
        box.setUpdateTime(null);
        return repository.add(box);
    }

    public PackagingBox findById(String id) {
        requireId(id);
        PackagingBox box = repository.findById(id);
        if (box == null) {
            throw new IllegalArgumentException("包装箱不存在");
        }
        return box;
    }

    public void update(PackagingBox box) {
        validate(box);
        PackagingBox existing = findById(box.getId());
        if (!repository.updateActive(box, existing)) {
            throw new IllegalArgumentException("包装箱不存在");
        }
    }

    public void delete(String id) {
        requireId(id);
        if (!repository.deleteActive(id)) {
            throw new IllegalArgumentException("包装箱不存在");
        }
    }

    public PagedResult<PackagingBox> list(String boxName, String code, BigDecimal cardboardHeight,
                                          BigDecimal cardboardWidth, int current, int size) {
        if (current < 1 || size < 1 || size > 100) {
            throw new IllegalArgumentException("页码(current)必须大于0，每页大小(size)必须在1-100之间");
        }
        Map<String, Object> filters = new HashMap<>();
        if (boxName != null && !boxName.isBlank()) {
            filters.put("boxName_like", boxName.strip());
        }
        if (code != null && !code.isBlank()) {
            filters.put("code", code.strip());
        }
        requirePositive(cardboardHeight, "纸板长度(cardboardHeight)");
        requirePositive(cardboardWidth, "纸板宽度(cardboardWidth)");
        if (cardboardHeight != null) {
            filters.put("cardboardHeight", cardboardHeight);
        }
        if (cardboardWidth != null) {
            filters.put("cardboardWidth", cardboardWidth);
        }
        return repository.list(current, size, filters);
    }

    public List<BigDecimal> listCardboardHeights() {
        return List.copyOf(new TreeSet<>(repository.listCardboardHeights()));
    }

    public List<BigDecimal> listCardboardWidths() {
        return List.copyOf(new TreeSet<>(repository.listCardboardWidths()));
    }

    private void validate(PackagingBox box) {
        if (box == null) {
            throw new IllegalArgumentException("包装箱不能为空");
        }
        if (box.getBoxName() == null || box.getBoxName().isBlank()) {
            throw new IllegalArgumentException("箱名(boxName)不能为空");
        }
        if (box.getCode() == null || box.getCode().isBlank()) {
            throw new IllegalArgumentException("编码(code)不能为空");
        }
        requirePositive(box.getCardboardDepth(), "纸板厚度(cardboardDepth)");
        requirePositive(box.getCardboardHeight(), "纸板长度(cardboardHeight)");
        requirePositive(box.getCardboardWidth(), "纸板宽度(cardboardWidth)");
        requirePositive(box.getBoxLength(), "箱长(boxLength)");
        requirePositive(box.getBoxWidth(), "箱宽(boxWidth)");
        requirePositive(box.getHeight(), "箱高(height)");
        box.setBoxName(box.getBoxName().strip());
        box.setCode(box.getCode().strip());
    }

    private void requirePositive(BigDecimal value, String field) {
        if (value != null && value.signum() <= 0) {
            throw new IllegalArgumentException(field + "必须大于0");
        }
    }

    private void requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("包装箱ID不能为空");
        }
    }
}
