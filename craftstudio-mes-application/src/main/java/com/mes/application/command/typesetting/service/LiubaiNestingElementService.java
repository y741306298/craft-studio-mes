package com.mes.application.command.typesetting.service;

import com.alibaba.fastjson2.JSON;
import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

/**
 * 留白生产工件排版元素组装服务。
 *
 * <p>留白零件虽然来源仍是生产工件，但预处理阶段已经把 mask SVG 外扩成可直接排版的印版轮廓。
 * 因此提交给算法时不能复用普通零件的 img/svg 组装，也不能走已有印版来源的拼接逻辑；这里单独把它重写成
 * {@code forme=true, counts=前端选择数量, img=svg=留白maskSvg} 的元素。</p>
 */
@Service
public class LiubaiNestingElementService {

    /**
     * 如果当前生产工件是留白零件，则重写为算法侧印版元素；否则返回 {@code null}，由普通零件逻辑继续组装。
     */
    public NestingRequest.Element buildLiubaiElement(ProductionPiece piece) {
        if (!isLiubaiPiece(piece)) {
            return null;
        }
        String liubaiSvg = resolveLiubaiSvg(piece);
        if (StringUtils.isBlank(liubaiSvg)) {
            String pieceId = StringUtils.isNotBlank(piece.getProductionPieceId()) ? piece.getProductionPieceId() : piece.getId();
            throw new IllegalArgumentException("留白生产工件缺少留白SVG地址：" + pieceId);
        }
        NestingRequest.Element element = new NestingRequest.Element();
        element.setId(liubaiElementId(piece));
        element.setCounts(piece.getQuantity() != null && piece.getQuantity() > 0 ? piece.getQuantity() : 1);
        element.setForme(Boolean.TRUE);
        element.setSvg(liubaiSvg);
        element.setImg(liubaiSvg);
        return element;
    }

    /**
     * 判断生产工件是否带有“留白xxx”工艺或已经生成留白资源。
     */
    public boolean isLiubaiPiece(ProductionPiece piece) {
        if (piece == null) {
            return false;
        }
        if (piece.getMarks() != null && piece.getMarks().entrySet().stream()
                .filter(Objects::nonNull)
                .anyMatch(entry -> isLiubaiText(entry.getKey()) || isLiubaiText(entry.getValue()))) {
            return true;
        }
        if (isLiubaiText(piece.getProcessingFlow())
                || isLiubaiText(piece.getRouteSvg())
                || isLiubaiText(piece.getRouteImg())
                || isLiubaiText(rawFile(piece.getMaskImageFile()))) {
            return true;
        }
        ProcedureFlow procedureFlow = piece.getProcedureFlow();
        if (procedureFlow == null || procedureFlow.getNodes() == null) {
            return false;
        }
        for (ProcedureFlowNode node : procedureFlow.getNodes()) {
            if (node == null) {
                continue;
            }
            if (isLiubaiText(node.getNodeName())) {
                return true;
            }
            if (node.getParamConfigs() != null && isLiubaiText(JSON.toJSONString(node.getParamConfigs()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 优先使用留白预处理回写到 productionPiece.maskImageFile.rawFile 的外扩 mask SVG。
     */
    private String resolveLiubaiSvg(ProductionPiece piece) {
        String maskSvg = rawFile(piece == null ? null : piece.getMaskImageFile());
        if (StringUtils.isNotBlank(maskSvg)) {
            return maskSvg;
        }
        if (piece != null && StringUtils.isNotBlank(piece.getRouteSvg())) {
            return piece.getRouteSvg();
        }
        return null;
    }

    /**
     * 生成仅用于算法排版的留白元素 ID。
     *
     * <p>留白 mask SVG 内部已经保留生产工件原始 _id；如果外层算法元素也继续使用同一个 _id，
     * callback 后按 nestedSvg id 反查来源 cell 时会把同一次排版误计两次。
     * 因此这里给算法外层元素使用独立 ID，让后续来源识别仍只按 SVG 内部原生产工件 ID 走原有逻辑。</p>
     */
    private String liubaiElementId(ProductionPiece piece) {
        String pieceId = piece == null ? null : piece.getId();
        if (StringUtils.isBlank(pieceId)) {
            pieceId = piece == null ? null : piece.getProductionPieceId();
        }
        return "liubai-nesting-" + (StringUtils.isBlank(pieceId) ? java.util.UUID.randomUUID() : pieceId);
    }

    private String rawFile(ImageFile imageFile) {
        return imageFile == null ? null : imageFile.getRawFile();
    }

    /**
     * 判断文本或资源路径是否表示留白资源/工艺。
     */
    private boolean isLiubaiText(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return value.contains("留白") || value.toLowerCase(Locale.ROOT).contains("liubai");
    }
}
