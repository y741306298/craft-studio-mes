package com.mes.interfaces.api.platform.configSide.manufacturerMeta;

import com.mes.application.command.order.AppOrderService;
import com.mes.domain.base.repository.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/configSide/order")
public class OrderController {

    @Autowired
    private AppOrderService appOrderService;

    /**
     * 重新处理订单项。
     * 根据 orderItemId 删除该订单项已生成的生产工件并重新提交预处理；
     * 根据 orderId 删除该订单下所有订单项已生成的生产工件并重新提交预处理。
     *
     * @param orderItemId 订单项 ID
     * @param orderId 订单 ID
     * @return 操作结果
     */
    @PostMapping("/item/reprocess")
    public ApiResponse<String> reprocessOrderItem(@RequestParam(required = false) String orderItemId,
                                                  @RequestParam(required = false) String orderId) {
        long deletedCount = appOrderService.reprocessOrderItem(orderItemId, orderId);
        return ApiResponse.success("重新处理任务已提交，已删除生产工件数量：" + deletedCount);
    }
}
