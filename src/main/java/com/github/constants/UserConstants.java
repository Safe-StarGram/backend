package com.github.constants;

/**
 * 사용자 관련 상수 정의
 */
public class UserConstants {
    
    // 역할 관련
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    public static final Integer ADMIN_ROLE_VALUE = 1;
    public static final Integer USER_ROLE_VALUE = 0;
    
    // 비밀번호 관련
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 100;
    
    // 프로필 관련
    public static final String DEFAULT_PROFILE_IMAGE = "default_profile.jpg";
    public static final String PROFILE_IMAGE_PREFIX = "profile_";
    
    // 이름 관련
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 50;
    
    private UserConstants() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}
