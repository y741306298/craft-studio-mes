package com.mes.application.command.orderPreprocessing.splice;

/** 背胶拼接：20mm 出血。 */
public class AdhesiveSpliceProcessStrategy extends AbstractFixedBloodSpliceProcessStrategy {
    public AdhesiveSpliceProcessStrategy() {
        super("背胶拼接", 20);
    }
}
