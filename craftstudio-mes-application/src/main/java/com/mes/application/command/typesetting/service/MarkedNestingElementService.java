package com.mes.application.command.typesetting.service;

import com.mes.application.command.api.req.NestingRequest;
import com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece;
import com.piliofpala.craftstudio.shared.domain.file.vo.ImageFile;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;


/**
 * 带 marks 的生产工件排版元素组装服务。
 *
 * <p>当生产工件携带 marks 时，预处理阶段已经生成可直接参与排版的印版轮廓资源。
 * 因此提交给算法时不能复用普通零件的 img/svg 组装，也不能走已有印版来源的拼接逻辑；这里单独把它重写成
 * {@code forme=true, counts=前端选择数量, img=svg=maskSvg/routeSvg} 的元素。</p>
 */
@Service
public class MarkedNestingElementService {

    /**
     * 如果当前生产工件携带 marks，则重写为算法侧印版元素；否则返回 {@code null}，由普通零件逻辑继续组装。
     */
    public NestingRequest.Element buildMarkedElement(ProductionPiece piece) {
        if (!hasMarks(piece)) {
            return null;
        }
        String markedSvg = resolveMarkedPieceSvg(piece);
        if (StringUtils.isBlank(markedSvg)) {
            String pieceId = StringUtils.isNotBlank(piece.getProductionPieceId()) ? piece.getProductionPieceId() : piece.getId();
            throw new IllegalArgumentException("带 marks 的生产工件缺少可参与排版的SVG地址：" + pieceId);
        }
        NestingRequest.Element element = new NestingRequest.Element();
        element.setId(markedElementId(piece));
        element.setCounts(piece.getQuantity() != null && piece.getQuantity() > 0 ? piece.getQuantity() : 1);
        element.setForme(Boolean.TRUE);
        element.setSvg(markedSvg);
        element.setImg(markedSvg);
        return element;
    }

    /**
     * 判断生产工件是否需要按特殊 forme element 参与排版。
     */
    public boolean hasMarks(ProductionPiece piece) {
        return piece != null && piece.getMarks() != null;
    }

    /**
     * 优先使用预处理回写到 productionPiece.maskImageFile.rawFile 的外扩 mask SVG。
     */
    private String resolveMarkedPieceSvg(ProductionPiece piece) {
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
     * 生成仅用于算法排版的特殊元素 ID。
     *
     * <p>预处理后的 mask SVG 内部已经保留生产工件原始 _id；如果外层算法元素也继续使用同一个 _id，
     * callback 后按 nestedSvg id 反查来源 cell 时会把同一次排版误计两次。
     * 因此这里给算法外层元素使用独立 ID，让后续来源识别仍只按 SVG 内部原生产工件 ID 走原有逻辑。</p>
     */
    private String markedElementId(ProductionPiece piece) {
        String pieceId = piece == null ? null : piece.getId();
        if (StringUtils.isBlank(pieceId)) {
            pieceId = piece == null ? null : piece.getProductionPieceId();
        }
        return "marked-nesting-" + (StringUtils.isBlank(pieceId) ? java.util.UUID.randomUUID() : pieceId);
    }

    private String rawFile(ImageFile imageFile) {
        return imageFile == null ? null : imageFile.getRawFile();
    }

}
