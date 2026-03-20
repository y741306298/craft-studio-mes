package com.mes.domain.shared.enums;

/**
 * 产品单位枚举
 */
public enum ProductUnit {
    
    SQUARE_METER("平方米", "m²", "面积单位"),           // 平方米
    SQUARE_CENTIMETER("平方厘米", "cm²", "面积单位"),   // 平方厘米
    SQUARE_MILLIMETER("平方毫米", "mm²", "面积单位"),   // 平方毫米
    
    CUBIC_METER("立方米", "m³", "体积单位"),            // 立方米
    CUBIC_CENTIMETER("立方厘米", "cm³", "体积单位"),    // 立方厘米
    CUBIC_MILLIMETER("立方毫米", "mm³", "体积单位"),    // 立方毫米
    
    METER("米", "m", "长度单位"),                      // 米
    CENTIMETER("厘米", "cm", "长度单位"),              // 厘米
    MILLIMETER("毫米", "mm", "长度单位"),              // 毫米
    KILOMETER("千米", "km", "长度单位"),               // 千米
    
    PERIMETER_METER("周长米", "m", "周长单位"),         // 周长米
    PERIMETER_CENTIMETER("周长厘米", "cm", "周长单位"), // 周长厘米
    
    KILOGRAM("千克", "kg", "重量单位"),                // 千克/公斤
    GRAM("克", "g", "重量单位"),                       // 克
    TON("吨", "t", "重量单位"),                        // 吨
    
    PIECE("件", "pcs", "数量单位"),                    // 件
    SET("套", "set", "数量单位"),                      // 套
    BOX("箱", "box", "数量单位"),                      // 箱
    CARTON("纸箱", "ctn", "数量单位"),                 // 纸箱
    PALLET("托盘", "pallet", "数量单位"),              // 托盘
    
    ROLL("卷", "roll", "数量单位"),                    // 卷
    SHEET("张", "sheet", "数量单位"),                  // 张
    BAR("根", "bar", "数量单位"),                      // 根
    BLOCK("块", "block", "数量单位"),                  // 块
    
    LITER("升", "L", "容量单位"),                      // 升
    MILLILITER("毫升", "mL", "容量单位"),              // 毫升
    
    WATT("瓦特", "W", "功率单位"),                     // 瓦特
    KILOWATT("千瓦", "kW", "功率单位"),                // 千瓦
    
    HOUR("小时", "h", "时间单位"),                     // 小时
    MINUTE("分钟", "min", "时间单位"),                 // 分钟
    DAY("天", "day", "时间单位");                      // 天
    
    private final String chineseName;
    private final String symbol;
    private final String unitType;
    
    ProductUnit(String chineseName, String symbol, String unitType) {
        this.chineseName = chineseName;
        this.symbol = symbol;
        this.unitType = unitType;
    }
    
    public String getChineseName() {
        return chineseName;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public String getUnitType() {
        return unitType;
    }
    
    /**
     * 根据符号获取枚举
     */
    public static ProductUnit getBySymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return null;
        }
        
        for (ProductUnit unit : values()) {
            if (unit.getSymbol().equalsIgnoreCase(symbol)) {
                return unit;
            }
        }
        
        return null;
    }
    
    /**
     * 根据中文名称获取枚举
     */
    public static ProductUnit getByChineseName(String chineseName) {
        if (chineseName == null || chineseName.trim().isEmpty()) {
            return null;
        }
        
        for (ProductUnit unit : values()) {
            if (unit.getChineseName().equals(chineseName)) {
                return unit;
            }
        }
        
        return null;
    }
    
    /**
     * 判断是否为面积单位
     */
    public boolean isAreaUnit() {
        return "面积单位".equals(this.unitType);
    }
    
    /**
     * 判断是否为体积单位
     */
    public boolean isVolumeUnit() {
        return "体积单位".equals(this.unitType);
    }
    
    /**
     * 判断是否为长度单位
     */
    public boolean isLengthUnit() {
        return "长度单位".equals(this.unitType);
    }
    
    /**
     * 判断是否为周长单位
     */
    public boolean isPerimeterUnit() {
        return "周长单位".equals(this.unitType);
    }
    
    /**
     * 判断是否为重量单位
     */
    public boolean isWeightUnit() {
        return "重量单位".equals(this.unitType);
    }
    
    /**
     * 判断是否为数量单位
     */
    public boolean isQuantityUnit() {
        return "数量单位".equals(this.unitType);
    }
}
