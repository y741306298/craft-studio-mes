package com.mes.domain.base.repository;


import com.mes.domain.base.BaseEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BaseRepository<T extends BaseEntity> {
    T add(T t);
    Collection<T> batchAdd(List<T> items);
    void delete(T t);
    void update(T t);
    void batchUpdate(List<T> items);

    T findById(String id);
    Map<String,T> findByIds(Collection<String> ids);
    List<T> list(long current, int size);
    List<T> filterList(long current, int size, Map<String, Object> filters);
    List<T> fuzzySearch(Map<String, String> searchFilters,long current, int size);
    long totalByFuzzySearch(Map<String, String> searchFilters);

    long total();
    long filterTotal(Map<String, Object> filters);
    boolean existById(String id);
    boolean allExistByIds(List<String> ids);


}
