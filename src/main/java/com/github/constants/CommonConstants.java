package com.github.constants;

/**
 * 공통 상수 정의
 */
public class CommonConstants {
    
    // 페이지네이션 관련
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final String MAX_PAGE_SIZE = "100";
    public static final String DEFAULT_SORT_ORDER = "DESC";
    
    // 기본값
    public static final String EMPTY_STRING = "";
    public static final String NULL_VALUE = "null";
    
    // 날짜 형식
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd/HH:mm:ss";
    
    // 파일 관련
    public static final String DEFAULT_PROFILE_IMAGE = "default_profile.jpg";
    public static final String DEFAULT_POST_IMAGE = "";
    
    private CommonConstants() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}
