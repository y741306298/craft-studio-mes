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
import java.math.BigDecimal;
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
        private String mirrorUrl;
        private ImageProperties imageProperties;
    }


    @Data
    public static class Slice {
        private List<Coordinate> xs;
        private List<Coordinate> ys;
    }

    @Data
    public static class Coordinate {
        private Integer value;
        private Integer blood;
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
        imageProperties.setColorSpace(oriImageProperties != null && oriImageProperties.getColorSpace() != null ? oriImageProperties.getColorSpace() : "CMYK");
        if (oriImageProperties != null) {
            imageProperties.setDpiX(oriImageProperties.getDpiX());
            imageProperties.setDpiY(oriImageProperties.getDpiY());
        }
        if (oriImageProperties != null) {
            imageProperties.setWidth((int) oriImageProperties.getWidth());
            imageProperties.setHeight((int) oriImageProperties.getHeight());
        }

        rawImage.setImageProperties(imageProperties);

        imageMaskRequest.setRawImage(rawImage);

        if (hasSpecialShape || hasCutting) {
            ImageFile maskImgFile = orderItem.getMaskImgFile();
            if (maskImgFile == null || maskImgFile.getFilePreview() == null || maskImgFile.getFilePreview().getRaw() == null) {
                throw new RuntimeException("存在异形工艺但蒙版图片不存在：" + orderItem.getOrderItemId());
            }
            imageMaskRequest.setMaskSvgUrl(maskImgFile.getRawFile());
        }
        if (hasCutting){
            Slice slice = buildSliceFromProcessingNodes(processingNodes, orderItem, rawImage);
            if (slice != null) {
                imageMaskRequest.setSlice(slice);
            }
        }
        return imageMaskRequest;
    }

    private static Slice buildSliceFromProcessingNodes(List<ProcedureFlowNode> processingNodes, OrderItem orderItem, RawImage rawImage) {
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
                        resolveCoordinates(paramValue, xs, ys, orderItem, rawImage);
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

    private static void resolveCoordinates(Object paramValue, List<Coordinate> xs, List<Coordinate> ys, OrderItem orderItem, RawImage rawImage) {
        if (paramValue == null) {
            return;
        }

        if (paramValue instanceof Map<?, ?> mapValue) {
            addCoordinatesFromList(mapValue.get("xs"), xs, 10, "xs", orderItem, rawImage);
            addCoordinatesFromList(mapValue.get("ys"), ys, 20, "ys", orderItem, rawImage);
            return;
        }

        if (paramValue instanceof List<?> coordinates) {
            for (int i = 0; i < coordinates.size(); i += 2) {
                if (i + 1 < coordinates.size()) {
                    xs.add(buildCoordinate(coordinates.get(i), 10, "xs", orderItem, rawImage));
                    ys.add(buildCoordinate(coordinates.get(i + 1), 20, "ys", orderItem, rawImage));
                }
            }
            return;
        }

        // 兜底：参数可能是 ProcessParamDTO 等对象，尝试通过 getter 反射获取 xs/ys
        Object xsValue = invokeGetter(paramValue, "getXs");
        Object ysValue = invokeGetter(paramValue, "getYs");
        addCoordinatesFromList(xsValue, xs, 10, "xs", orderItem, rawImage);
        addCoordinatesFromList(ysValue, ys, 20, "ys", orderItem, rawImage);
    }

    private static void addCoordinatesFromList(Object listObj, List<Coordinate> target, Integer defaultBlood, String axis, OrderItem orderItem, RawImage rawImage) {
        if (!(listObj instanceof List<?> values)) {
            return;
        }
        for (Object value : values) {
            target.add(buildCoordinate(value, defaultBlood, axis, orderItem, rawImage));
        }
    }

    private static Coordinate buildCoordinate(Object rawValue, Integer defaultBlood, String axis, OrderItem orderItem, RawImage rawImage) {
        Coordinate coordinate = new Coordinate();
        coordinate.setBlood(convertBloodMmToPx(defaultBlood, axis, rawImage));

        if (rawValue == null) {
            return coordinate;
        }
        if (rawValue instanceof Number || rawValue instanceof String) {
            Integer value = parseInteger(rawValue);
            coordinate.setValue(convertValueToPx(value, axis, orderItem, rawImage));
            return coordinate;
        }
        if (rawValue instanceof Map<?, ?> valueMap) {
            Object value = valueMap.get("value");
            Object blood = valueMap.get("blood");
            Integer bloodValue = parseInteger(blood);
            Integer coordinateValue = parseInteger(value);
            coordinate.setValue(convertValueToPx(coordinateValue != null ? coordinateValue : parseInteger(rawValue), axis, orderItem, rawImage));
            coordinate.setBlood(convertBloodMmToPx(bloodValue != null ? bloodValue : defaultBlood, axis, rawImage));
            return coordinate;
        }

        Object value = invokeGetter(rawValue, "getValue");
        Object blood = invokeGetter(rawValue, "getBlood");
        Integer bloodValue = parseInteger(blood);
        Integer coordinateValue = value != null ? parseInteger(value) : null;
        coordinate.setValue(convertValueToPx(coordinateValue != null ? coordinateValue : parseInteger(rawValue), axis, orderItem, rawImage));
        coordinate.setBlood(convertBloodMmToPx(bloodValue != null ? bloodValue : defaultBlood, axis, rawImage));
        return coordinate;
    }


    private static Integer convertBloodMmToPx(Integer bloodMm, String axis, RawImage rawImage) {
        if (bloodMm == null) {
            return null;
        }
        double dpi = resolveAxisDpi(axis, rawImage);
        if (dpi <= 0) {
            return bloodMm;
        }
        return (int) Math.round((bloodMm / 25.4D) * dpi);
    }

    private static Integer convertValueToPx(Integer value, String axis, OrderItem orderItem, RawImage rawImage) {
        if (value == null) {
            return null;
        }
        Integer rawSize = resolveAxisRawSize(axis, rawImage);
        Double usageSize = resolveAxisUsageSizeMm(axis, orderItem);
        if (rawSize == null || rawSize <= 0 || usageSize == null || usageSize <= 0) {
            return value;
        }
        return (int) Math.round((value / usageSize) * rawSize);
    }

    private static double resolveAxisDpi(String axis, RawImage rawImage) {
        if (rawImage == null || rawImage.getImageProperties() == null) {
            return 0D;
        }
        if ("ys".equalsIgnoreCase(axis)) {
            return rawImage.getImageProperties().getDpiY();
        }
        return rawImage.getImageProperties().getDpiX();
    }

    private static Integer resolveAxisRawSize(String axis, RawImage rawImage) {
        if (rawImage == null || rawImage.getImageProperties() == null) {
            return null;
        }
        return "ys".equalsIgnoreCase(axis) ? rawImage.getImageProperties().getHeight() : rawImage.getImageProperties().getWidth();
    }

    private static Double resolveAxisUsageSizeMm(String axis, OrderItem orderItem) {
        if (orderItem == null || orderItem.getMaterial() == null) {
            return null;
        }
        Object usageSize3D = orderItem.getMaterial().getUsageSize3D();
        if (usageSize3D == null) {
            return null;
        }
        Object sizeValue = invokeGetter(usageSize3D, "ys".equalsIgnoreCase(axis) ? "getHeight" : "getWidth");
        if (sizeValue instanceof Number number) {
            return number.doubleValue();
        }
        if (sizeValue != null) {
            try {
                return new BigDecimal(String.valueOf(sizeValue)).doubleValue();
            } catch (Exception ignore) {
                return null;
            }
        }
        return null;
    }
    private static Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            String valueStr = String.valueOf(value).trim();
            if (valueStr.isEmpty()) {
                return null;
            }
            return new BigDecimal(valueStr).intValue();
        } catch (NumberFormatException ex) {
            return null;
        }
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
