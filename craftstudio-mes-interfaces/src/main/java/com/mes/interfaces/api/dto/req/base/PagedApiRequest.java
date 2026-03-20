package com.mes.interfaces.api.dto.req.base;

import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PagedApiRequest extends ApiRequest{
    private int current;
    private int size;
    @Override
    public boolean isValid(){
        return current>0 && size>0 && size<=100;
    }

    @Override
    public String getValidationMessage() {
        if (size <= 0 || size > 100) return "每页大小(size)必须在1-100之间";
        return "";
    }

    public PagedQuery toPagedQuery() {
        return new PagedQuery(current, size);
    }
}
