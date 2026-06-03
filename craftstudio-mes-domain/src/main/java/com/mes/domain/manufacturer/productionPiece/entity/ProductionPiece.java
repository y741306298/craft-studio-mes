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
     * 生产工件关联的 mark 图。
     *
     * <p>留白工艺会生成一张与外扩矩形同宽高的黑色边框 PNG，并将该 PNG 作为 mark 保存到这里，
     * 方便后续排版、刀版或下载流程直接从生产工件上读取留白外框资源。</p>
     */
    private ImageFile markImageFile;
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
