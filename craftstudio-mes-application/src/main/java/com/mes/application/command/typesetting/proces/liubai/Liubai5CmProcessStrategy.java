package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * “留白5cm”实体策略。
 *
 * <p>业务规则：</p>
 * <ul>
 *     <li>留白5cm等价于在矩形 mask 外侧增加 50mm 留白区域。</li>
 *     <li>无超幅拼接的直接路线：四边都外扩 50mm。</li>
 *     <li>存在超幅拼接的 callback 路线：复用固定尺寸留白基类逻辑，只外扩非出血边。</li>
 * </ul>
 */
@Service
public class Liubai5CmProcessStrategy extends AbstractFixedLiubaiProcessStrategy {
    /**
     * 留白5cm对应的毫米外扩值。
     */
    private static final double EXPAND_MM = 50D;

    /**
     * 留白5cm支持的工艺节点名/参数文本关键字。
     */
    private static final String[] MATCH_KEYWORDS = {
            "留白5cm", "留白5CM", "留白5厘米", "留白50mm", "留白50毫米", "5cm", "50mm", "50毫米"
    };

    /**
     * 构造留白5cm实体策略。
     *
     * @param restTemplate 远程 SVG 拉取客户端
     * @param ossTagUploadService SVG 上传服务
     */
    public Liubai5CmProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(restTemplate, ossTagUploadService);
    }

    /**
     * 当前策略规格名称，用于生成 SVG 分组 id。
     *
     * @return 固定返回 5cm
     */
    @Override
    protected String specName() {
        return "5cm";
    }

    /**
     * 当前策略外扩毫米数。
     *
     * @return 固定返回 50mm
     */
    @Override
    protected double expandMm() {
        return EXPAND_MM;
    }

    /**
     * 当前策略可识别的留白5cm关键字。
     *
     * @return 留白5cm关键字数组
     */
    @Override
    protected String[] matchKeywords() {
        return MATCH_KEYWORDS;
    }
}
