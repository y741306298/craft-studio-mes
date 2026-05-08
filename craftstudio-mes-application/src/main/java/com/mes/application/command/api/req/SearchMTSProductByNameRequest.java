package com.mes.application.command.api.req;

import lombok.Data;

@Data
public class SearchMTSProductByNameRequest {

    private String name;
    private Integer current;
    private Integer size;
    private String rmfId;
}