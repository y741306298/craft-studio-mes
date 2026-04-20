package com.mes.application.dto.resp;

import com.mes.domain.base.repository.ApiResponse;
import lombok.Data;

import java.util.List;

@Data
public class PagedApiResponse<T> extends ApiResponse<PagedApiResponse.PageData<T>> {
    @Data
    public static class PageData<T> {
        private List<T> items;       // 当前页数据列表
        private long current;          // 当前页码
        private long size;             // 每页大小
        private long total;            // 总记录数

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
}
