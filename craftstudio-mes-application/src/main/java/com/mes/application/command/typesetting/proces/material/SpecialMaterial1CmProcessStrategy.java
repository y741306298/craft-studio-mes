package com.mes.application.command.typesetting.proces.material;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import com.mes.domain.manufacturer.manufacturerMaterialLayoutSpecCfg.service.ManufacturerMaterialLayoutSpecCfgService;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * “3P布/软膜”特殊材料处理策略。
 *
 * <p>材料名称包含“3P布”或“软膜”（例如“软膜（xxx）”）时，在无显式留白工艺的前提下，
 * 按留白 1cm 的规则执行四周外扩，并在留白区域写入订单项 ID 后 5 位、文件名、尺寸和工艺流信息。</p>
 *
 * <p>本策略复用固定留白 SVG 生成逻辑，生成的留白 mark 与四边标签 mark {@code <g>} 会统一写入
 * {@code require-plt="false"}，避免这些辅助标记进入 PLT。</p>
 */
@Service
public class SpecialMaterial1CmProcessStrategy extends AbstractMaterialProcessStrategy {
    private static final int SPECIAL_MATERIAL_EXPAND_CM = 1;

    public SpecialMaterial1CmProcessStrategy(RestTemplate restTemplate,
                                             OssTagUploadService ossTagUploadService,
                                             ManufacturerMaterialLayoutSpecCfgService layoutSpecCfgService) {
        super(SPECIAL_MATERIAL_EXPAND_CM, restTemplate, ossTagUploadService, layoutSpecCfgService);
    }

    @Override
    protected boolean matchesMaterialName(String materialName) {
        return StringUtils.isNotBlank(materialName)
                && (materialName.contains("3P布") || materialName.contains("软膜"));
    }
}
