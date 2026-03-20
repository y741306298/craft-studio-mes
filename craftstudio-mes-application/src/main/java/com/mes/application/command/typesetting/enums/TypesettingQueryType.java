package com.mes.application.command.typesetting.enums;

public enum TypesettingQueryType {
    ALL("全部"),
    PART("零件"),
    TYPESETTING("排版");

    private final String description;

    TypesettingQueryType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static TypesettingQueryType fromDescription(String description) {
        for (TypesettingQueryType type : values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return ALL;
    }
}
