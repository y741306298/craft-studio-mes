package com.mes.domain.base.repository;


import com.mes.domain.base.TreedCategory;

import java.util.List;

public interface TreedCategoryRepository<DO extends TreedCategory> extends BaseRepository<DO> {
    List<DO> findFirstLevelCategoryList();
    List<DO> findByParentId(String parentId);
    boolean existByParentId(String parentId);
}
