package com.mes.domain.delivery.packagingBox.repository;

import com.mes.domain.delivery.packagingBox.entity.PackagingBox;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PackagingBoxRepository {
    PackagingBox add(PackagingBox box);

    PackagingBox findById(String id);

    boolean updateActive(PackagingBox box, PackagingBox existing);

    boolean deleteActive(String id);

    PagedResult<PackagingBox> list(long current, int size, Map<String, Object> filters);

    List<BigDecimal> listCardboardHeights();

    List<BigDecimal> listCardboardWidths();
}
