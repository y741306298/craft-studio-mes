package com.mes.application.dto.resp;

import lombok.Data;

@Data
public class ApiResponse<T> {
    public interface RepStatusCode {
        int success = 200;
        int badParams = 400;
        int serviceError = 500;
        int notFound = 404;
        int unauthorized = 401;
        int customize = 999;
    }

    private int code = 200;
    private String message = "success";
    private T data;
    private long timestamp = System.currentTimeMillis();

    // 成功-无定制返回数据，统一 "success" 字符串
    public static ApiResponse<String> success() {
        ApiResponse<String> response = new ApiResponse<>();
        response.setData("success");
        return response;
    }

    // 成功-定制返回数据
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setData(data);
        return response;
    }

    //失败
    public static ApiResponse<String> fail(int code, String message) {
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
