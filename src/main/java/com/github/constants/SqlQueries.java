package com.github.constants;

/**
 * SQL 쿼리 상수 정의
 */
public class SqlQueries {
    
    // 사용자 관련 쿼리
    public static final String FIND_USER_BY_EMAIL = 
        "SELECT users_id, email, name, password, role FROM users WHERE email = ?";
    
    public static final String FIND_USER_BY_ID = 
        "SELECT users_id, email, name, password, role FROM users WHERE users_id = ?";
    
    public static final String FIND_USER_WITH_DEPARTMENT_AND_POSITION = 
        "SELECT u.users_id, u.name, u.email, u.phone_number, u.radio_number, " +
        "u.department_id, u.position_id, u.role, u.profile_photo_url, u.created_at, u.updated_at, " +
        "COALESCE(d.name, 'N/A') as department_name, " +
        "COALESCE(p.name, 'N/A') as position_name " +
        "FROM users u " +
        "LEFT JOIN department d ON u.department_id = d.department_id " +
        "LEFT JOIN position p ON u.position_id = p.position_id " +
        "WHERE u.users_id = ?";
    
    public static final String FIND_ALL_USERS = 
        "SELECT u.users_id, u.name, u.email, u.phone_number, u.radio_number, " +
        "u.department_id, u.position_id, u.role, u.profile_photo_url, u.created_at, u.updated_at, " +
        "COALESCE(d.name, 'N/A') as department_name, " +
        "COALESCE(p.name, 'N/A') as position_name " +
        "FROM users u " +
        "LEFT JOIN department d ON u.department_id = d.department_id " +
        "LEFT JOIN position p ON u.position_id = p.position_id " +
        "ORDER BY u.users_id ASC LIMIT ? OFFSET ?";
    
    public static final String FIND_USERS_BY_DEPARTMENT = 
        "SELECT u.users_id, u.name, u.email, u.phone_number, u.radio_number, " +
        "u.department_id, u.position_id, u.role, u.profile_photo_url, u.created_at, u.updated_at, " +
        "COALESCE(d.name, 'N/A') as department_name, " +
        "COALESCE(p.name, 'N/A') as position_name " +
        "FROM users u " +
        "LEFT JOIN department d ON u.department_id = d.department_id " +
        "LEFT JOIN position p ON u.position_id = p.position_id " +
        "WHERE d.name = ? " +
        "ORDER BY u.users_id ASC LIMIT ? OFFSET ?";
    
    // 게시물 관련 쿼리
    public static final String FIND_POST_BY_ID = 
        "SELECT p.post_id, p.sub_area_id, p.area_id, p.reporter_id, p.checker_id, p.action_taker_id, " +
        "p.title, p.content, p.reporter_risk, p.manager_risk, p.is_checked, p.is_action_taken, " +
        "p.post_photo_url, p.created_at, p.updated_at, p.checked_at, p.action_taken_at, " +
        "u.name as reporter_name, u.position_id as reporter_position, u.department_id as reporter_department, " +
        "c.name as checker_name, c.position_id as checker_position, c.department_id as checker_department, " +
        "a.name as action_taker_name, a.position_id as action_taker_position, a.department_id as action_taker_department " +
        "FROM post p " +
        "LEFT JOIN users u ON p.reporter_id = u.users_id " +
        "LEFT JOIN users c ON p.checker_id = c.users_id " +
        "LEFT JOIN users a ON p.action_taker_id = a.users_id " +
        "WHERE p.post_id = ?";
    
    public static final String FIND_POSTS_BY_SUB_AREA = 
        "SELECT p.post_id, p.sub_area_id, p.area_id, p.reporter_id, p.checker_id, p.action_taker_id, " +
        "p.title, p.content, p.reporter_risk, p.manager_risk, p.is_checked, p.is_action_taken, " +
        "p.post_photo_url, p.created_at, p.updated_at, p.checked_at, p.action_taken_at, " +
        "u.name as reporter_name, u.position_id as reporter_position, u.department_id as reporter_department, " +
        "c.name as checker_name, c.position_id as checker_position, c.department_id as checker_department, " +
        "a.name as action_taker_name, a.position_id as action_taker_position, a.department_id as action_taker_department " +
        "FROM post p " +
        "LEFT JOIN users u ON p.reporter_id = u.users_id " +
        "LEFT JOIN users c ON p.checker_id = c.users_id " +
        "LEFT JOIN users a ON p.action_taker_id = a.users_id " +
        "WHERE p.sub_area_id = ? " +
        "ORDER BY p.created_at DESC LIMIT ? OFFSET ?";
    
    public static final String INSERT_POST = 
        "INSERT INTO post (sub_area_id, area_id, reporter_id, title, content, reporter_risk, post_photo_url, is_checked, is_action_taken, created_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    public static final String UPDATE_POST = 
        "UPDATE post SET %s WHERE post_id = ?";
    
    public static final String DELETE_POST = 
        "DELETE FROM post WHERE post_id = ?";
    
    public static final String COUNT_POSTS_BY_SUB_AREA = 
        "SELECT COUNT(*) FROM post WHERE sub_area_id = ?";
    
    public static final String COUNT_ACTION_TAKEN_BY_SUB_AREA = 
        "SELECT COUNT(*) FROM post WHERE sub_area_id = ? AND is_action_taken = '1'";
    
    // 현장 관련 쿼리
    public static final String FIND_AREA_BY_ID = 
        "SELECT area_id, name, description, created_at, updated_at FROM area WHERE area_id = ?";
    
    public static final String FIND_ALL_AREAS = 
        "SELECT area_id, name, description, created_at, updated_at FROM area ORDER BY area_id ASC";
    
    public static final String FIND_SUB_AREAS_BY_AREA_ID = 
        "SELECT sub_area_id, area_id, name, description, created_at, updated_at FROM sub_area WHERE area_id = ? ORDER BY sub_area_id ASC";
    
    public static final String INSERT_AREA = 
        "INSERT INTO area (name, description, created_at, updated_at) VALUES (?, ?, ?, ?)";
    
    public static final String UPDATE_AREA = 
        "UPDATE area SET name = ?, description = ?, updated_at = ? WHERE area_id = ?";
    
    public static final String DELETE_AREA = 
        "DELETE FROM area WHERE area_id = ?";
    
    public static final String DELETE_POSTS_BY_AREA_ID = 
        "DELETE FROM post WHERE area_id = ?";
    
    public static final String DELETE_SUB_AREAS_BY_AREA_ID = 
        "DELETE FROM sub_area WHERE area_id = ?";
    
    // 댓글 관련 쿼리
    public static final String FIND_COMMENTS_BY_POST_ID = 
        "SELECT c.comment_id, c.post_id, c.user_id, c.content, c.created_at, c.updated_at, " +
        "u.name as user_name " +
        "FROM comment c " +
        "LEFT JOIN users u ON c.user_id = u.users_id " +
        "WHERE c.post_id = ? " +
        "ORDER BY c.created_at ASC";
    
    public static final String FIND_COMMENT_BY_ID = 
        "SELECT comment_id, post_id, user_id, content, created_at, updated_at FROM comment WHERE comment_id = ?";
    
    public static final String INSERT_COMMENT = 
        "INSERT INTO comment (post_id, user_id, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
    
    public static final String UPDATE_COMMENT = 
        "UPDATE comment SET content = ?, updated_at = ? WHERE comment_id = ?";
    
    public static final String DELETE_COMMENT = 
        "DELETE FROM comment WHERE comment_id = ?";
    
    private SqlQueries() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}
