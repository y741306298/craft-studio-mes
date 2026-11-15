package com.mes.application.command.typesetting.service;



/** 算法可能将正面 mask 的 mark 一起复制到反面；保留图片矩形与排版变换，去掉工艺标记。 */
public final class SublimationMirrorSvgCleaner {
    private SublimationMirrorSvgCleaner() { }

    public static String clean(String svg) {
        return com.mes.application.command.graphics.RemoteGraphics.svg("SUB_CLEAN", com.mes.application.command.graphics.RemoteGraphics.params("source", svg));
    }
}
