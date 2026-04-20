package com.mes.application.command.delivery.enums;

public enum Kuaidi100MethodEnum {

    PRINT("order", "打印"),
    REPRINT("printOld", "复打"),
    CANCEL("cancel", "取消");

    private final String code;
    private final String description;

    Kuaidi100MethodEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取枚举
     * @param code 方法代码
     * @return 对应的枚举值
     */
    public static Kuaidi100MethodEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (Kuaidi100MethodEnum method : values()) {
            if (method.getCode().equals(code)) {
                return method;
            }
        }
        return null;
    }
}