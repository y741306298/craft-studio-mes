package com.mes.application.command.api.req;

import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.vo.UploadConfig;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImageMaskRequest {
    private RawImage rawImage;
    private String maskSvgUrl;
    private Slice slice;
    private UploadConfig uploadConfig;
    private CallbackConfig callbackConfig;

    @Data
    public static class RawImage {
        private String url;
        private ImageProperties imageProperties;
    }


    @Data
    public static class Slice {
        private List<Coordinate> xs;
        private List<Coordinate> ys;
    }

    @Data
    public static class Coordinate {
        private String value;
        private String blood;
    }


    @Data
    public static class OssConfig {
        private StsToken stsToken;
        private String bucket;
        private String region;
    }

    @Data
    public static class StsToken {
        private String accessKeyId;
        private String accessKeySecret;
        private String expiration;
        private String securityToken;
    }

    public static ImageMaskRequest processWithCutting(OrderItem orderItem, List<ProcedureFlowNode> processingNodes, boolean hasSpecialShape,boolean hasCutting) {
        ImageMaskRequest imageMaskRequest = new ImageMaskRequest();

        if (orderItem == null) {
            throw new RuntimeException("订单项不能为空");
        }

        ImageFile productionImgFile = orderItem.getProductionImgFile();
        if (productionImgFile == null || productionImgFile.getFilePreview() == null || productionImgFile.getFilePreview().getRaw() == null) {
            throw new RuntimeException("生产图片不存在");
        }

        RawImage rawImage = new RawImage();
        rawImage.setUrl(productionImgFile.getRawFile());
        com.piliofpala.craftstudio.shared.domain.file.vo.ImageProperties oriImageProperties = productionImgFile.getImageProperties();

        ImageProperties imageProperties = new ImageProperties();
        imageProperties.setColorSpace(oriImageProperties.getColorSpace() != null ? oriImageProperties.getColorSpace() : "CMYK");
        imageProperties.setDpiX((int) oriImageProperties.getDpiX());
        imageProperties.setDpiY((int) oriImageProperties.getDpiY());
        imageProperties.setWidth(oriImageProperties.getWidth());
        imageProperties.setHeight(oriImageProperties.getHeight());

        rawImage.setImageProperties(imageProperties);

        imageMaskRequest.setRawImage(rawImage);

        if (hasSpecialShape) {
            ImageFile maskImgFile = orderItem.getMaskImgFile();
            if (maskImgFile == null || maskImgFile.getFilePreview() == null || maskImgFile.getFilePreview().getRaw() == null) {
                throw new RuntimeException("存在异形工艺但蒙版图片不存在：" + orderItem.getOrderItemId());
            }
            imageMaskRequest.setMaskSvgUrl(maskImgFile.getRawFile());
        }
        if (hasCutting){
            Slice slice = buildSliceFromProcessingNodes(processingNodes);
            if (slice != null) {
                imageMaskRequest.setSlice(slice);
            }
        }
        return imageMaskRequest;
    }

    private static Slice buildSliceFromProcessingNodes(List<ProcedureFlowNode> processingNodes) {
        if (processingNodes == null || processingNodes.isEmpty()) {
            return null;
        }

        Slice slice = new Slice();
        List<Coordinate> xs = new ArrayList<>();
        List<Coordinate> ys = new ArrayList<>();

        for (ProcedureFlowNode node : processingNodes) {
            if ("超幅拼接".equals(node.getNodeName())) {
                List<MTOProductSpecDTO.ProcessParamConfigDTO> paramConfigs = node.getParamConfigs();
                if (paramConfigs != null) {
                    for (MTOProductSpecDTO.ProcessParamConfigDTO config : paramConfigs) {
                        Object paramValue = config.getParam();
                        if (paramValue instanceof List) {
                            List<Double> coordinates = (List<Double>) paramValue;
                            for (int i = 0; i < coordinates.size(); i += 2) {
                                if (i + 1 < coordinates.size()) {
                                    Coordinate xCoord = new Coordinate();
                                    xCoord.setValue(String.valueOf(coordinates.get(i)));
                                    xCoord.setBlood("10");
                                    xs.add(xCoord);

                                    Coordinate yCoord = new Coordinate();
                                    yCoord.setValue(String.valueOf(coordinates.get(i + 1)));
                                    yCoord.setBlood("20");
                                    ys.add(yCoord);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!xs.isEmpty()) {
            slice.setXs(xs);
        }
        if (!ys.isEmpty()) {
            slice.setYs(ys);
        }

        return (!xs.isEmpty() || !ys.isEmpty()) ? slice : null;
    }


}
