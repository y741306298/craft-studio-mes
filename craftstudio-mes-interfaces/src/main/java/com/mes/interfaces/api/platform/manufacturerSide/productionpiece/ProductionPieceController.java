package com.mes.interfaces.api.platform.manufacturerSide.productionpiece;

import com.mes.application.command.productionPiece.AppProductionPieceService;
import com.mes.interfaces.api.dto.req.productionpiece.ProductionPieceListRequest;
import com.mes.interfaces.api.dto.resp.productionpiece.ProductionPieceResponse;
import com.mes.interfaces.api.dto.resp.ApiResponse;
import com.mes.interfaces.api.dto.resp.PagedApiResponse;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedQuery;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manufacturerSide/productionPiece")
@RequiredArgsConstructor
public class ProductionPieceController {

    @Autowired
    private AppProductionPieceService appProductionPieceService;

    /**
     * 分页查询生产工件列表
     * @param request 查询请求参数
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<ProductionPieceResponse> listProductionPieces(
            @Valid @RequestBody ProductionPieceListRequest request) {
        
        PagedQuery query = new PagedQuery(request.getCurrent(), request.getSize());
        
        PagedResult<com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece> result = 
                appProductionPieceService.findProductionPieces(
                        request.getStatus(),
                        request.getMaterial(),
                        request.getNodeName(),
                        query);
        
        java.util.List<ProductionPieceResponse> responses = result.items().stream()
                .map(ProductionPieceResponse::from)
                .toList();
        
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }

    /**
     * 根据 productionPieceId 查询详情
     * @param productionPieceId 生产工件 ID
     * @return 生产工件详情
     */
    @GetMapping("/{productionPieceId}")
    public ApiResponse<ProductionPieceResponse> getProductionPieceById(
            @PathVariable String productionPieceId) {
        
        if (StringUtils.isBlank(productionPieceId)) {
            ApiResponse<ProductionPieceResponse> failResponse = new ApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage("生产工件 ID 不能为空");
            return failResponse;
        }
        
        com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece piece = 
                appProductionPieceService.findByProductionPieceId(productionPieceId);
        
        if (piece == null) {
            ApiResponse<ProductionPieceResponse> failResponse = new ApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage("生产工件不存在");
            return failResponse;
        }
        
        return ApiResponse.success(ProductionPieceResponse.from(piece));
    }

    /**
     * 根据订单项目 ID 分页查询生产工件
     * @param orderItemId 订单项目 ID
     * @param current 当前页码
     * @param size 每页大小
     * @return 分页查询结果
     */
    @GetMapping("/byOrderItem/{orderItemId}")
    public PagedApiResponse<ProductionPieceResponse> listProductionPiecesByOrderItem(
            @PathVariable String orderItemId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        
        if (StringUtils.isBlank(orderItemId)) {
            PagedApiResponse<ProductionPieceResponse> failResponse = new PagedApiResponse<>();
            failResponse.setCode(ApiResponse.RepStatusCode.badParams);
            failResponse.setMessage("订单项目 ID 不能为空");
            return failResponse;
        }
        
        PagedQuery query = new PagedQuery(current, size);
        
        PagedResult<com.mes.domain.manufacturer.productionPiece.entity.ProductionPiece> result = 
                appProductionPieceService.findProductionPiecesByOrderItemId(orderItemId, query);
        
        java.util.List<ProductionPieceResponse> responses = result.items().stream()
                .map(ProductionPieceResponse::from)
                .toList();
        
        return PagedApiResponse.success(responses, query.getCurrent(), query.getSize(), result.total());
    }
}
