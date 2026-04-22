package com.mes.domain.auth.repository;

import com.mes.domain.auth.entity.ManufacturerUser;
import com.mes.domain.base.repository.BaseRepository;

public interface ManufacturerUserRepository extends BaseRepository<ManufacturerUser> {
    ManufacturerUser findByAccount(String account);
}
