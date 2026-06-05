package com.mes.application.command.orderPreprocessing.splice;

/** 超幅拼接：保持原有 20mm 出血策略不变。 */
public class SuperWidthSpliceProcessStrategy extends AbstractFixedBloodSpliceProcessStrategy {
    public SuperWidthSpliceProcessStrategy() {
        super("超幅拼接", 20, true);
    }
}
