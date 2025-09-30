package com.github.constants;

/**
 * 데이터베이스 관련 상수 정의
 */
public class DatabaseConstants {
    
    // 테이블명
    public static final String POST_TABLE = "post";
    public static final String USER_TABLE = "users";
    public static final String AREA_TABLE = "area";
    public static final String SUB_AREA_TABLE = "sub_area";
    public static final String COMMENT_TABLE = "comment";
    public static final String DEPARTMENT_TABLE = "department";
    public static final String POSITION_TABLE = "position";
    public static final String POST_PHOTOS_TABLE = "post_photos";
    
    // 컬럼명
    public static final String POST_ID_COLUMN = "post_id";
    public static final String USER_ID_COLUMN = "users_id";
    public static final String AREA_ID_COLUMN = "area_id";
    public static final String SUB_AREA_ID_COLUMN = "sub_area_id";
    public static final String COMMENT_ID_COLUMN = "comment_id";
    public static final String DEPARTMENT_ID_COLUMN = "department_id";
    public static final String POSITION_ID_COLUMN = "position_id";
    
    // 공통 컬럼명
    public static final String CREATED_AT_COLUMN = "created_at";
    public static final String UPDATED_AT_COLUMN = "updated_at";
    public static final String NAME_COLUMN = "name";
    public static final String EMAIL_COLUMN = "email";
    public static final String PASSWORD_COLUMN = "password";
    public static final String ROLE_COLUMN = "role";
    
    // Post 테이블 컬럼명
    public static final String REPORTER_ID_COLUMN = "reporter_id";
    public static final String CHECKER_ID_COLUMN = "checker_id";
    public static final String ACTION_TAKER_ID_COLUMN = "action_taker_id";
    public static final String TITLE_COLUMN = "title";
    public static final String CONTENT_COLUMN = "content";
    public static final String REPORTER_RISK_COLUMN = "reporter_risk";
    public static final String MANAGER_RISK_COLUMN = "manager_risk";
    public static final String IS_CHECKED_COLUMN = "is_checked";
    public static final String IS_ACTION_TAKEN_COLUMN = "is_action_taken";
    public static final String POST_PHOTO_URL_COLUMN = "post_photo_url";
    public static final String CHECKED_AT_COLUMN = "checked_at";
    public static final String ACTION_TAKEN_AT_COLUMN = "action_taken_at";
    
    // User 테이블 컬럼명
    public static final String PROFILE_PHOTO_URL_COLUMN = "profile_photo_url";
    public static final String PHONE_NUMBER_COLUMN = "phone_number";
    public static final String RADIO_NUMBER_COLUMN = "radio_number";
    
    private DatabaseConstants() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}
