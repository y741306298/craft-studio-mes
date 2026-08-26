package com.mes.application.command.statistics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A factory participating in an order transfer. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferFactoryVO {
    private String manufacturerMetaId;
    private String name;
}
