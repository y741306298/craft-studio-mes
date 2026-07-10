package com.mes.application.dto.resp;

import com.mes.domain.base.repository.ApiResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PagedApiResponse<T> extends ApiResponse<PagedApiResponse.PageData<T>> {
    @Data
    public static class PageData<T> {
        private List<T> items;       // 当前页数据列表
        private long current;          // 当前页码
        private long size;             // 每页大小
        private long total;            // 总记录数
        private Long totalOrderCount;  // 查询日期的订单统计总订单数
        private BigDecimal totalArea;  // 查询日期的订单统计总面积（平方米）
        private BigDecimal totalAmount;// 查询日期的订单统计总金额

        public PageData(List<T> records, long current, long size, long total) {
            this.items = records;
            this.current = current;
            this.size = size;
            this.total = total;
        }
    }

    // 快速创建成功分页响应的方法
    public static <T> PagedApiResponse<T> success(List<T> records, long current, long size, long total) {
        PagedApiResponse<T> response = new PagedApiResponse<>();
        response.setData(new PageData<>(records, current, size, total));
        return response;
    }

    public static <T> PagedApiResponse<T> success(List<T> records,
                                                  long current,
                                                  long size,
                                                  long total,
                                                  Long totalOrderCount,
                                                  BigDecimal totalArea,
                                                  BigDecimal totalAmount) {
        PagedApiResponse<T> response = success(records, current, size, total);
        response.getData().setTotalOrderCount(totalOrderCount);
        response.getData().setTotalArea(totalArea);
        response.getData().setTotalAmount(totalAmount);
        return response;
    }
}
