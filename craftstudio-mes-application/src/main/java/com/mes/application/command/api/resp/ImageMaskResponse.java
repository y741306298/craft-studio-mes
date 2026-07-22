package com.mes.application.command.api.resp;

import lombok.Data;

import java.util.List;

@Data
public class ImageMaskResponse {
    private String error;
    private String status;
    private String id;
    private String orderItemId;
    private String preprocessRequestId;
    private String taskId;
    private String task_id;
    private List<Pair> pairs;

    @Data
    public static class Pair {
        private String group;
        private Integer seq;
        /**
         * 新版结构：正面结果
         */
        private SideResult normal;
        /**
         * 兼容部分算法回调直接返回的正面结果字段
         */
        private SideResult primaryResult;
        /**
         * 新版结构：反面结果
         */
        private SideResult mirror;

        /**
         * 兼容旧版结构字段
         */
        private String img;
        private String svg;
        private String previewImg;
        private String thumbnail;
        private Blood blood;

        public SideResult getPrimaryResult() {
            if (normal != null) {
                fillPairMetaIfNecessary(normal);
                return normal;
            }
            if (primaryResult != null) {
                fillPairMetaIfNecessary(primaryResult);
                return primaryResult;
            }
            if (img == null && svg == null && previewImg == null && thumbnail == null && blood == null) {
                return null;
            }
            SideResult compat = new SideResult();
            compat.setImg(img);
            compat.setSvg(svg);
            compat.setPreviewImg(previewImg);
            compat.setThumbnail(thumbnail);
            compat.setBlood(blood);
            compat.setGroup(group);
            compat.setSeq(seq);
            return compat;
        }

        private void fillPairMetaIfNecessary(SideResult sideResult) {
            if (sideResult.getGroup() == null) {
                sideResult.setGroup(group);
            }
            if (sideResult.getSeq() == null) {
                sideResult.setSeq(seq);
            }
        }
    }

    @Data
    public static class SideResult {
        private String img;
        private String svg;
        private String previewImg;
        private String thumbnail;
        private Blood blood;
        private String group;
        private Integer seq;
    }

    @Data
    public static class Blood {
        private Integer x;
        private Integer y;
    }
}
