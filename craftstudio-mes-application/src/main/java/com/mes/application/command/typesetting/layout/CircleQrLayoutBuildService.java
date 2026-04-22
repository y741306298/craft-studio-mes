package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

@Service
public class CircleQrLayoutBuildService extends AbstractLayoutModeBuildService {
    @Override
    public TypesettingLayoutMode supportMode() {
        return TypesettingLayoutMode.SHAPED_CUTTING_PLT_QR_CIRCLE;
    }

    @Override
    public FormeLayoutBuildResult build(FormeBuildContext context) {
        FormeLayoutBuildResult result = new FormeLayoutBuildResult();
        int marginHeight = context.getMarginHeight();
        int marginLeft = 0;
        int marginTop = marginHeight;
        int marginRight = 0;
        int marginBottom = marginHeight;
        int elementOriginX = marginLeft;
        int elementOriginY = marginTop;

        FormeGenerationRequest.Margin margin = new FormeGenerationRequest.Margin();
        margin.setLeft(marginLeft);
        margin.setTop(marginTop);
        margin.setRight(marginRight);
        margin.setBottom(marginBottom);
        result.setMargin(margin);

        String elementA = context.getElementAResolver().apply(context.getTypesettingInfo());
        String elementB = context.getPlateNameSupplier().get();
        String elementC = context.getQrDataUriGenerator().apply(elementB);
        String elementF = buildTagStripDataUri(elementA, elementB, elementC, context.getNestedWidth(), marginHeight);

        FormeGenerationRequest.Mark top = new FormeGenerationRequest.Mark();
        top.setImg(elementF);
        top.setSize(createSize(context.getNestedWidth(), marginHeight));
        top.setPosition(createPosition(elementOriginX, 0));

        FormeGenerationRequest.Mark bottom = new FormeGenerationRequest.Mark();
        bottom.setImg(elementF);
        bottom.setSize(createSize(context.getNestedWidth(), marginHeight));
        bottom.setPosition(createPosition(elementOriginX, elementOriginY + context.getNestedHeight()));
        result.setMarks(Arrays.asList(top, bottom));

        int sideOffset = 30;
        int topY = marginTop / 2;
        int bottomY = elementOriginY + context.getNestedHeight() + (marginBottom / 2);
        int rightX = Math.max(elementOriginX + context.getNestedWidth() - sideOffset - 10, elementOriginX + sideOffset);
        String circleSvgUrl = "https://craftstudio-ordering-test.oss-cn-hangzhou.aliyuncs.com/basetag/circle.svg";

        FormeGenerationRequest.AnchorPoint tl = new FormeGenerationRequest.AnchorPoint();
        tl.setImg("circle.png");
        tl.setSvg(circleSvgUrl);
        tl.setSize(createSize(10, 10));
        tl.setPosition(createPosition(elementOriginX + sideOffset, topY));

        FormeGenerationRequest.AnchorPoint tr = new FormeGenerationRequest.AnchorPoint();
        tr.setImg("circle.png");
        tr.setSvg(circleSvgUrl);
        tr.setSize(createSize(10, 10));
        tr.setPosition(createPosition(rightX, topY));

        FormeGenerationRequest.AnchorPoint bl = new FormeGenerationRequest.AnchorPoint();
        bl.setImg("circle.png");
        bl.setSvg(circleSvgUrl);
        bl.setSize(createSize(10, 10));
        bl.setPosition(createPosition(elementOriginX + sideOffset, bottomY));

        FormeGenerationRequest.AnchorPoint br = new FormeGenerationRequest.AnchorPoint();
        br.setImg("circle.png");
        br.setSvg(circleSvgUrl);
        br.setSize(createSize(10, 10));
        br.setPosition(createPosition(rightX, bottomY));
        result.setAnchorPoints(Arrays.asList(tl, tr, bl, br));

        result.setOutputs(buildDefaultOutputs(supportMode(), context.getBusinessId()));
        result.setUploadPath("printingplate/" + context.getBusinessId() + "/");
        return result;
    }

    private String buildTagStripDataUri(String elementA,
                                        String elementB,
                                        String qrDataUri,
                                        int stripWidth,
                                        int stripHeight) {
        int spacing = 40;
        int qrSize = Math.max(stripHeight - 20, 20);
        int textY = (stripHeight / 2) + 8;
        int qrY = (stripHeight - qrSize) / 2;
        int bX = spacing + qrSize + spacing;
        int aX = bX + 300 + spacing;

        String stripSvg = "<svg xmlns='http://www.w3.org/2000/svg' width='" + stripWidth + "' height='" + stripHeight + "'>"
                + "<rect width='100%' height='100%' fill='white'/>"
                + "<image href='" + qrDataUri + "' x='" + spacing + "' y='" + qrY + "' width='" + qrSize + "' height='" + qrSize + "'/>"
                + "<text x='" + bX + "' y='" + textY + "' font-size='32' fill='black'>" + escapeXml(elementB) + "</text>"
                + "<text x='" + aX + "' y='" + textY + "' font-size='32' fill='black'>" + escapeXml(elementA) + "</text>"
                + "</svg>";
        String base64 = Base64.getEncoder().encodeToString(stripSvg.getBytes(StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + base64;
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
