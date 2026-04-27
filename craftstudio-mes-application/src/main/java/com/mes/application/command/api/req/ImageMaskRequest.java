package com.mes.application.command.api.req;

import com.mes.application.command.api.vo.CallbackConfig;
import com.mes.application.command.api.vo.UploadConfig;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.order.orderInfo.entity.OrderItem;
import com.piliofpala.craftstudio.shared.application.product.mtoproduct.dto.MTOProductSpecDTO;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageProperties;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                        resolveCoordinates(paramValue, xs, ys);
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

    private static void resolveCoordinates(Object paramValue, List<Coordinate> xs, List<Coordinate> ys) {
        if (paramValue == null) {
            return;
        }

        if (paramValue instanceof Map<?, ?> mapValue) {
            addCoordinatesFromList(mapValue.get("xs"), xs, "10");
            addCoordinatesFromList(mapValue.get("ys"), ys, "20");
            return;
        }

        if (paramValue instanceof List<?> coordinates) {
            for (int i = 0; i < coordinates.size(); i += 2) {
                if (i + 1 < coordinates.size()) {
                    xs.add(buildCoordinate(coordinates.get(i), "10"));
                    ys.add(buildCoordinate(coordinates.get(i + 1), "20"));
                }
            }
            return;
        }

        // 兜底：参数可能是 ProcessParamDTO 等对象，尝试通过 getter 反射获取 xs/ys
        Object xsValue = invokeGetter(paramValue, "getXs");
        Object ysValue = invokeGetter(paramValue, "getYs");
        addCoordinatesFromList(xsValue, xs, "10");
        addCoordinatesFromList(ysValue, ys, "20");
    }

    private static void addCoordinatesFromList(Object listObj, List<Coordinate> target, String defaultBlood) {
        if (!(listObj instanceof List<?> values)) {
            return;
        }
        for (Object value : values) {
            target.add(buildCoordinate(value, defaultBlood));
        }
    }

    private static Coordinate buildCoordinate(Object rawValue, String defaultBlood) {
        Coordinate coordinate = new Coordinate();
        coordinate.setBlood(defaultBlood);

        if (rawValue == null) {
            return coordinate;
        }
        if (rawValue instanceof Number || rawValue instanceof String) {
            coordinate.setValue(String.valueOf(rawValue));
            return coordinate;
        }
        if (rawValue instanceof Map<?, ?> valueMap) {
            Object value = valueMap.get("value");
            Object blood = valueMap.get("blood");
            coordinate.setValue(value != null ? String.valueOf(value) : null);
            coordinate.setBlood(blood != null ? String.valueOf(blood) : defaultBlood);
            return coordinate;
        }

        Object value = invokeGetter(rawValue, "getValue");
        Object blood = invokeGetter(rawValue, "getBlood");
        coordinate.setValue(value != null ? String.valueOf(value) : String.valueOf(rawValue));
        coordinate.setBlood(blood != null ? String.valueOf(blood) : defaultBlood);
        return coordinate;
    }

    private static Object invokeGetter(Object target, String getterName) {
        if (target == null) {
            return null;
        }
        try {
            Method getter = target.getClass().getMethod(getterName);
            return getter.invoke(target);
        } catch (Exception ignore) {
            return null;
        }
    }


}
