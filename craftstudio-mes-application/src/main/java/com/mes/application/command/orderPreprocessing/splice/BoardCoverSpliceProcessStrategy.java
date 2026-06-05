package com.mes.application.command.orderPreprocessing.splice;

/** 覆板拼接：20mm 出血。 */
public class BoardCoverSpliceProcessStrategy extends AbstractFixedBloodSpliceProcessStrategy {
    public BoardCoverSpliceProcessStrategy() {
        super("覆板拼接", 20);
    }
}
