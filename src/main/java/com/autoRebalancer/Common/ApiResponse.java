package com.autoRebalancer.Common;

public record ApiResponse<T> (
        String status
        , T data
        , String message
){

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

