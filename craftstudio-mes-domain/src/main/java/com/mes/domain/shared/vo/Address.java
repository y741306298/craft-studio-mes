package com.mes.domain.shared.vo;

import lombok.Data;

@Data
public class Address {
    private String terminalRegionCode;//最后一级行政编码,前缀表示国家，比如CN-110101001表示：中国北京东城区东华门街道
    private String detailAddress;//用户自由编辑的详细地址，比如小区单元，门牌号等

    public boolean valid(){
        return terminalRegionCode!=null && detailAddress!=null;
    }
}
