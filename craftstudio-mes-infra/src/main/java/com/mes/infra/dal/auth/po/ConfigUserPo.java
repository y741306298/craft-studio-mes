package com.mes.infra.dal.auth.po;

import com.mes.domain.auth.entity.ConfigUser;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "configUser")
public class ConfigUserPo extends BasePO<ConfigUser> {
    private String account;
    private String password;
    private String name;
    private String phone;
    private Boolean isAdmin;

    @Override
    public ConfigUser toDO() {
        ConfigUser user = new ConfigUser();
        copyBaseFieldsToDO(user);
        user.setAccount(this.account);
        user.setPassword(this.password);
        user.setName(this.name);
        user.setPhone(this.phone);
        user.setIsAdmin(this.isAdmin);
        return user;
    }

    @Override
    protected BasePO<ConfigUser> fromDO(ConfigUser _do) {
        if (_do == null) {
            return null;
        }
        this.account = _do.getAccount();
        this.password = _do.getPassword();
        this.name = _do.getName();
        this.phone = _do.getPhone();
        this.isAdmin = _do.getIsAdmin();
        return this;
    }
}
