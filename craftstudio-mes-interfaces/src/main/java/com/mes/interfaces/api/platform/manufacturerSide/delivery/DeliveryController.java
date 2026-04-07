package com.mes.interfaces.api.platform.manufacturerSide.delivery;

import com.mes.application.command.delivery.vo.DeliveryPkgPieceVO;

import com.mes.application.dto.req.delivery.DeliveryPkgRequest;
import com.mes.application.dto.resp.PagedApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public class DeliveryController {

    /**
     * 统一查询排版和生产工件列表
     * @param request 查询请求参数
     * @return 分页查询结果
     */
    @PostMapping("/list")
    public PagedApiResponse<DeliveryPkgPieceVO> listTypesettingAndProductionPieces(@RequestBody DeliveryPkgRequest request) {



        return null;
    }

}
