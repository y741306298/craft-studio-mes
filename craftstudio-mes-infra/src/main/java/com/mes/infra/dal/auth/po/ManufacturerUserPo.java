package com.mes.infra.dal.auth.po;

import com.mes.domain.auth.entity.ManufacturerUser;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "manufacturerUser")
public class ManufacturerUserPo extends BasePO<ManufacturerUser> {
    private String account;
    private String password;
    private String manufacturerMetaId;
    private String name;
    private String phone;

    @Override
    public ManufacturerUser toDO() {
        ManufacturerUser user = new ManufacturerUser();
        copyBaseFieldsToDO(user);
        user.setAccount(this.account);
        user.setPassword(this.password);
        user.setManufacturerMetaId(this.manufacturerMetaId);
        user.setName(this.name);
        user.setPhone(this.phone);
        return user;
    }

    @Override
    protected BasePO<ManufacturerUser> fromDO(ManufacturerUser _do) {
        if (_do == null) {
            return null;
        }
        this.account = _do.getAccount();
        this.password = _do.getPassword();
        this.manufacturerMetaId = _do.getManufacturerMetaId();
        this.name = _do.getName();
        this.phone = _do.getPhone();
        return this;
    }
}
