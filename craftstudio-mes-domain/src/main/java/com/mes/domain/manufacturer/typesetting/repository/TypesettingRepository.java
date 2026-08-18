package com.mes.domain.manufacturer.typesetting.repository;

import com.mes.domain.base.repository.BaseRepository;
import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;

import java.util.Collection;

public interface TypesettingRepository extends BaseRepository<TypesettingInfo> {
    void batchUpdateCallbackFailure(Collection<String> ids, String status, String remark);
}
