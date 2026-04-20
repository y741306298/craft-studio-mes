package com.mes.domain.base;

import com.mes.domain.base.repository.ApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.exception.BusinessNotAllowException;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class TreedCategory extends BaseEntity {
    private String parentId;
    private String name;
    private int level;
    public abstract int maxLevel();

    public boolean isTerminal() {
        return level==maxLevel();
    }
    public void validate(){
        if(level > maxLevel()){
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "最多支持："+(maxLevel()+1)+" 级分类");
        }
    }

    public void changeParentCategory(TreedCategory parentCategory) {
        boolean allow = parentCategory != null || parentId == null;
        if(parentCategory != null && parentCategory.level+1 != level){
            allow = false;
        }
        if(!allow){
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "分类等级无法改变，该分类当前等级：" + level);
        }
        this.parentId = parentCategory==null ? null : parentCategory.getId();
        this.level = parentCategory==null ? 0 : parentCategory.level+1;
        if(level>maxLevel()){
            throw new BusinessNotAllowException(ApiResponse.RepStatusCode.badParams, "最多支持："+(maxLevel()+1)+" 级分类");
        }
    }
    public boolean ended(){
        return level==maxLevel();
    }

}
