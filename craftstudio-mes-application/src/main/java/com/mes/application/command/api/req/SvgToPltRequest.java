package com.mes.application.command.api.req;

import lombok.Data;

@Data
public class SvgToPltRequest {
    private String svgUrl;
    private String direction;
}
