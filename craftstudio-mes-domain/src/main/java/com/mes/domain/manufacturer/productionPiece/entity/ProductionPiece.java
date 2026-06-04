package com.mes.domain.manufacturer.productionPiece.entity;

import com.mes.domain.base.BaseEntity;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.manufacturer.productionPiece.enums.ProductionPieceStatus;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import com.piliofpala.craftstudio.shared.domain.product.mtoproduct.vo.MaterialConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.w3c.dom.Node;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProductionPiece extends BaseEntity {

    private String productionPieceId;
    private String orderItemId;
    private String carrierId;
    private String manufacturerId;
    private String procedureFlowId;
    private String status;
    private String productionPieceType;
    private MaterialConfig materialConfig;
    private Integer quantity;
    private String templateCode;
    private String positionType;
    private String positionCode;
    private String routeImg;
    private String routeSvg;
    private ImageFile productImageFile;
    private ImageFile maskImageFile;
    /**
     * 生产工件附加标记资源。
     *
     * <p>结构参考 TypesettingInfo.marks，key 表示 mark 类型，value 表示 mark 文件的 OSS 地址。
     * 留白工艺会将与外扩矩形同宽高的黑色边框 PNG 保存到该 Map 中；四角打扣预处理也会
     * 保存扣点图片资源，便于后续排版、刀版或下载流程直接从生产工件上读取附加标记资源。</p>
     */
    private Map<String, String> marks;
    private Double width;
    private Double height;
    private Blood blood;
    private String group;
    private Integer seq;
    private String processingFlow;
    private ProcedureFlow procedureFlow;
    private List<DeliveryPkgInfo> deliveryPkgInfos;
    private List<MirrorConfig> mirrorConfigs;

}
