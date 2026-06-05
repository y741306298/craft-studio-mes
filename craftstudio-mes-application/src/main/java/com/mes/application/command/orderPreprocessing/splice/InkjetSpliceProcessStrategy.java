package com.mes.application.command.orderPreprocessing.splice;

/** 喷绘拼接：30mm 出血。 */
public class InkjetSpliceProcessStrategy extends AbstractFixedBloodSpliceProcessStrategy {
    public InkjetSpliceProcessStrategy() {
        super("喷绘拼接", 30);
    }
}
