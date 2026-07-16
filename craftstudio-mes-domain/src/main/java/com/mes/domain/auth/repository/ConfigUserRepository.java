package com.mes.domain.auth.repository;

import com.mes.domain.auth.entity.ConfigUser;
import com.mes.domain.base.repository.BaseRepository;

public interface ConfigUserRepository extends BaseRepository<ConfigUser> {
    ConfigUser findByAccount(String account);

    ConfigUser findByPhone(String phone);
}
