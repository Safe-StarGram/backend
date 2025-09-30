package com.github.constants;

/**
 * API 관련 상수 정의
 */
public class ApiConstants {
    
    // API 버전
    public static final String API_VERSION = "/api/v1";
    
    // 엔드포인트
    public static final String AUTH_ENDPOINT = "/auth";
    public static final String ADMIN_ENDPOINT = "/api/admin";
    public static final String NOTICES_ENDPOINT = "/notices";
    public static final String USERS_ENDPOINT = "/api/users";
    public static final String SITES_ENDPOINT = "/sites";
    
    // 응답 메시지
    public static final String SUCCESS_MESSAGE = "성공";
    public static final String ERROR_MESSAGE = "오류가 발생했습니다";
    public static final String UNAUTHORIZED_MESSAGE = "인증이 필요합니다";
    public static final String FORBIDDEN_MESSAGE = "권한이 없습니다";
    
    // HTTP 상태 코드
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_INTERNAL_SERVER_ERROR = 500;
    
    private ApiConstants() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}
