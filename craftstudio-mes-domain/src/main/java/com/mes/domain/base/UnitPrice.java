package com.mes.domain.base;

import com.mes.domain.shared.enums.ProductUnit;
import lombok.Data;

@Data
public class UnitPrice{

    private Double price;                       // 价格
    private String unit;                        // 单位（使用 ProductUnit 的 symbol）

    /**
     * 获取产品单位枚举
     */
    public ProductUnit getProductUnit() {
        if (this.unit == null || this.unit.isEmpty()) {
            return null;
        }
        return ProductUnit.getBySymbol(this.unit);
    }

    /**
     * 设置产品单位
     */
    public void setProductUnit(ProductUnit productUnit) {
        if (productUnit != null) {
            this.unit = productUnit.getSymbol();
        }
    }

}
