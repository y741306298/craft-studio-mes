package com.mes.application.command.orderPreprocessing.splice;

/** 板材拼接：0mm 出血。 */
public class PanelSpliceProcessStrategy extends AbstractFixedBloodSpliceProcessStrategy {
    public PanelSpliceProcessStrategy() {
        super("板材拼接", 0);
    }
}
