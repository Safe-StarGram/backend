package com.github.constants;

/**
 * 에러 메시지 상수 정의
 */
public class ErrorMessages {
    
    // 엔티티 찾을 수 없음
    public static final String POST_NOT_FOUND = "게시물을 찾을 수 없습니다";
    public static final String USER_NOT_FOUND = "사용자를 찾을 수 없습니다";
    public static final String AREA_NOT_FOUND = "현장을 찾을 수 없습니다";
    public static final String COMMENT_NOT_FOUND = "댓글을 찾을 수 없습니다";
    public static final String SUB_AREA_NOT_FOUND = "세부 현장을 찾을 수 없습니다";
    
    // 권한 관련
    public static final String INSUFFICIENT_PERMISSION = "권한이 없습니다";
    public static final String ADMIN_PERMISSION_REQUIRED = "관리자 권한이 필요합니다";
    public static final String REPORTER_PERMISSION_REQUIRED = "작성자 권한이 필요합니다";
    public static final String CANNOT_DELETE_SELF = "자기 자신을 삭제할 수 없습니다";
    
    // 입력 검증
    public static final String INVALID_INPUT = "잘못된 입력입니다";
    public static final String INVALID_EMAIL_FORMAT = "올바른 이메일 형식이 아닙니다";
    public static final String INVALID_PASSWORD_LENGTH = "비밀번호는 8자 이상 100자 이하여야 합니다";
    public static final String INVALID_NAME_LENGTH = "이름은 2자 이상 50자 이하여야 합니다";
    public static final String INVALID_RISK_SCORE = "위험도 점수는 1-5 사이여야 합니다";
    
    // 인증 관련
    public static final String EMAIL_ALREADY_EXISTS = "이미 존재하는 이메일입니다";
    public static final String INVALID_CREDENTIALS = "잘못된 인증 정보입니다";
    public static final String TOKEN_EXPIRED = "토큰이 만료되었습니다";
    public static final String TOKEN_INVALID = "유효하지 않은 토큰입니다";
    
    // 파일 관련
    public static final String FILE_UPLOAD_FAILED = "파일 업로드에 실패했습니다";
    public static final String FILE_SIZE_EXCEEDED = "파일 크기가 너무 큽니다 (최대 10MB)";
    public static final String INVALID_FILE_TYPE = "지원하지 않는 파일 형식입니다";
    
    // 데이터베이스 관련
    public static final String DATABASE_ERROR = "데이터베이스 오류가 발생했습니다";
    public static final String FOREIGN_KEY_CONSTRAINT = "참조 관계로 인해 삭제할 수 없습니다";
    
    // 비즈니스 로직
    public static final String POST_ALREADY_CHECKED = "이미 확인된 게시물입니다";
    public static final String POST_ALREADY_ACTION_TAKEN = "이미 조치가 완료된 게시물입니다";
    public static final String CANNOT_UPDATE_OTHERS_POST = "다른 사용자의 게시물을 수정할 수 없습니다";
    
    private ErrorMessages() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}
