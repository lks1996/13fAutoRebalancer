package com.autoRebalancer.Common;


import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final String status; // SUCCESS, FAIL, ERROR
    private final T data;
    private final String message;

    private ApiResponse(String status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    // 성공 응답을 만드는 정적 팩토리 메서드
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", data, null);
    }

    // 실패 응답을 만드는 정적 팩토리 메서드
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>("FAIL", null, message);
    }

    // 에러 응답을 만드는 정적 팩토리 메서드
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("ERROR", null, message);
    }
}

