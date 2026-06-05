package com.mes.application.command.orderPreprocessing.splice;

/** 写真拼接：0mm 出血。 */
public class PhotoSpliceProcessStrategy extends AbstractFixedBloodSpliceProcessStrategy {
    public PhotoSpliceProcessStrategy() {
        super("写真拼接", 0);
    }
}
