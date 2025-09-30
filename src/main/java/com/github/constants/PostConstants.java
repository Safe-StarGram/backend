package com.github.constants;

/**
 * 게시물 관련 상수 정의
 */
public class PostConstants {
    
    // 상태 관련
    public static final String STATUS_CHECKED = "1";
    public static final String STATUS_UNCHECKED = "0";
    public static final String STATUS_ACTION_TAKEN = "1";
    public static final String STATUS_ACTION_NOT_TAKEN = "0";
    
    // 위험도 점수 관련
    public static final int MIN_RISK_SCORE = 1;
    public static final int MAX_RISK_SCORE = 5;
    
    // 파일 관련
    public static final String POST_IMAGE_PREFIX = "post_";
    public static final String DEFAULT_POST_IMAGE = "";
    
    // 제목/내용 길이 제한
    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_CONTENT_LENGTH = 1000;
    
    private PostConstants() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}
