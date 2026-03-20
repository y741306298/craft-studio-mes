package com.mes.infra.base;

import com.mes.domain.base.BaseEntity;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.lang.reflect.Constructor;
import java.util.Date;

@Data
public abstract class BasePO<DO extends BaseEntity> {
    @Id
    private String id;
    @CreatedDate
    private Date createTime;
    @LastModifiedDate
    private Date updateTime;
    private Date deleteAt; //软删除日期 null:未删除
    public abstract DO toDO();

    protected abstract BasePO<DO> fromDO(DO _do);

    protected void copyBaseFieldsToDO(BaseEntity targetDO) {
        targetDO.setId(this.id);
        targetDO.setCreateTime(this.createTime);
        targetDO.setUpdateTime(this.updateTime);
    }

    public static <DO extends BaseEntity, PO extends BasePO<DO>> PO fromDO(DO _do, Class<PO> poClazz){
        try{
            Constructor<PO> constructor = poClazz.getDeclaredConstructor();
            PO po = constructor.newInstance();
            po.fromDO(_do);
            po.setId(_do.getId());
            po.setCreateTime(_do.getCreateTime());
            po.setUpdateTime(_do.getUpdateTime());
            return po;
        }catch (Exception e){
            throw new RuntimeException("无法创建PO");
        }

    }
}
