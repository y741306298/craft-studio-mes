package com.mes.application.command.typesetting.layout;

import com.mes.application.command.api.req.FormeGenerationRequest;
import com.mes.domain.manufacturer.typesetting.enums.TypesettingLayoutMode;

/**
 * 模式构建抽象基类。
 *
 * <p>封装各模式共享的低层拼装能力，避免重复代码：
 * createSize / createPosition / default outputs。
 */
public abstract class AbstractLayoutModeBuildService implements TypesettingLayoutModeBuildService {

    /** 构建尺寸对象。 */
    protected FormeGenerationRequest.Size createSize(int width, int height) {
        FormeGenerationRequest.Size size = new FormeGenerationRequest.Size();
        size.setWidth(width);
        size.setHeight(height);
        return size;
    }

    /** 构建坐标对象（坐标系原点：扩展矩形左上角）。 */
    protected FormeGenerationRequest.Position createPosition(int x, int y) {
        FormeGenerationRequest.Position position = new FormeGenerationRequest.Position();
        position.setX(x);
        position.setY(y);
        return position;
    }

    /**
     * 构建通用 outputs。
     *
     * <p>依据 mode 的 requireJson/requirePlt/requireSvg 决定输出项。
     */
    protected FormeGenerationRequest.Outputs buildDefaultOutputs(TypesettingLayoutMode mode, String businessId) {
        FormeGenerationRequest.Outputs outputs = new FormeGenerationRequest.Outputs();
        if (mode.isRequireJsonFile()) {
            FormeGenerationRequest.OutputConfig json = new FormeGenerationRequest.OutputConfig();
            json.setObjectName(businessId + ".json");
            FormeGenerationRequest.EnvConfig env = new FormeGenerationRequest.EnvConfig();
            env.setBasePath("d://test//");
            FormeGenerationRequest.DtpConfig dtp = new FormeGenerationRequest.DtpConfig();
            dtp.setNewpage("false");
            dtp.setShowmode("4");
            dtp.setAutoSaveFile("");
            dtp.setTpfSavePath("d:\\test\\" + businessId + ".tpf");
            env.setDtp(dtp);
            json.setEnv(env);
            outputs.setJson(json);
        }
        if (mode.isRequirePltFile()) {
            FormeGenerationRequest.OutputConfig plt = new FormeGenerationRequest.OutputConfig();
            plt.setDirection("h");
            plt.setObjectName(businessId + "-1.plt," + businessId + "-2.plt");
            outputs.setPlt(plt);
        }
        if (mode.isRequireSvgFile()) {
            FormeGenerationRequest.OutputConfig svg = new FormeGenerationRequest.OutputConfig();
            svg.setObjectName(businessId + ".svg");
            outputs.setFormeSvg(svg);
        }
        return outputs;
    }
}
