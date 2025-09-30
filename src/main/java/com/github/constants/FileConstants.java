package com.github.constants;

/**
 * 파일 업로드 관련 상수 정의
 */
public class FileConstants {
    
    // 업로드 디렉토리
    public static final String UPLOAD_DIR = "./uploads";
    
    // 파일 크기 제한
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final long MAX_REQUEST_SIZE = 10 * 1024 * 1024; // 10MB
    
    // 허용되는 이미지 타입
    public static final String[] ALLOWED_IMAGE_TYPES = {"jpg", "jpeg", "png", "gif"};
    
    // 파일 접두사
    public static final String PROFILE_IMAGE_PREFIX = "profile_";
    public static final String POST_IMAGE_PREFIX = "post_";
    
    // 기본 이미지
    public static final String DEFAULT_PROFILE_IMAGE = "default_profile.jpg";
    public static final String DEFAULT_POST_IMAGE = "";
    
    private FileConstants() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}
