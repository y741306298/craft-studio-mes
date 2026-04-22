package com.mes.application.command.typesetting.layout;

import com.mes.domain.manufacturer.typesetting.entity.TypesettingInfo;
import lombok.Data;

import java.util.function.Function;
import java.util.function.Supplier;

@Data
public class FormeBuildContext {
    private TypesettingInfo typesettingInfo;
    private String businessId;
    private int nestedWidth;
    private int nestedHeight;
    private int marginHeight;
    private Supplier<String> plateNameSupplier;
    private Function<String, String> qrDataUriGenerator;
    private Function<TypesettingInfo, String> elementAResolver;
}
