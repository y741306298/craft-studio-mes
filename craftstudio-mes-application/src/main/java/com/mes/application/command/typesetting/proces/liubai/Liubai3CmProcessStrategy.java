package com.mes.application.command.typesetting.proces.liubai;

import com.mes.application.command.typesetting.support.OssTagUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * “留白3cm”实体策略。
 *
 * <p>业务规则：</p>
 * <ul>
 *     <li>留白3cm等价于在矩形 mask 外侧增加 30mm 留白区域。</li>
 *     <li>无超幅拼接的直接路线：四边都外扩 30mm。</li>
 *     <li>存在超幅拼接的 callback 路线：复用固定尺寸留白基类逻辑，只外扩非出血边。</li>
 * </ul>
 */
@Service
public class Liubai3CmProcessStrategy extends AbstractFixedLiubaiProcessStrategy {
    /**
     * 留白3cm对应的毫米外扩值。
     */
    private static final double EXPAND_MM = 30D;

    /**
     * 留白3cm支持的工艺节点名/参数文本关键字。
     */
    private static final String[] MATCH_KEYWORDS = {
            "留白3cm", "留白3CM", "留白3厘米", "留白30mm", "留白30毫米", "3cm", "30mm", "30毫米"
    };

    /**
     * 构造留白3cm实体策略。
     *
     * @param restTemplate 远程 SVG 拉取客户端
     * @param ossTagUploadService SVG 上传服务
     */
    public Liubai3CmProcessStrategy(RestTemplate restTemplate, OssTagUploadService ossTagUploadService) {
        super(restTemplate, ossTagUploadService);
    }

    /**
     * 当前策略规格名称，用于生成 SVG 分组 id。
     *
     * @return 固定返回 3cm
     */
    @Override
    protected String specName() {
        return "3cm";
    }

    /**
     * 当前策略外扩毫米数。
     *
     * @return 固定返回 30mm
     */
    @Override
    protected double expandMm() {
        return EXPAND_MM;
    }

    /**
     * 当前策略可识别的留白3cm关键字。
     *
     * @return 留白3cm关键字数组
     */
    @Override
    protected String[] matchKeywords() {
        return MATCH_KEYWORDS;
    }
}
