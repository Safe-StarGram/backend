package com.github.repository;

import com.github.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

//    private UserEntity mapRow(ResultSet rs, int rowNum) {
//
//        return UserEntity.builder()
//                .email(rs.getString("email"))
//                .name(rs.getString("name"))
//                .password(rs.getString("password"))
//                .build();
//    }


    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                email
        );
        return count != null && count > 0;
    }

    public Long save(UserEntity userEntity) {
        String sql = "INSERT INTO users(email, name , password) VALUES (?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps =
                    con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, userEntity.getEmail());
            ps.setString(2, userEntity.getName());
            ps.setString(3, userEntity.getPassword());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();


    }

    public UserEntity findByEmail(String email) {
        String sql = "SELECT users_id, email, name, password, role FROM users WHERE email = ?";
        var list = jdbcTemplate.query(sql, (rs, rn) -> {
            UserEntity u = new UserEntity();
            u.setUserId(rs.getInt("users_id"));   // PK 컬럼명에 맞게
            u.setEmail(rs.getString("email"));
            u.setName(rs.getString("name"));
            u.setPassword(rs.getString("password"));
            u.setRole(rs.getInt("role"));  // role 정보 추가
            return u;
        }, email);

        return list.isEmpty() ? null : list.get(0);
    }

    public UserEntity findById(Integer userId) {
        try {
            String sql = """
                SELECT u.users_id, u.name, u.phone_number, u.radio_number, u.profile_photo_url,
                       u.department_id, u.position_id,
                       COALESCE(d.name, 'N/A') as department_name, 
                       COALESCE(p.name, 'N/A') as position_name
                FROM users u
                LEFT JOIN department d ON u.department_id = d.department_id  
                LEFT JOIN position p ON u.position_id = p.position_id
                WHERE u.users_id = ?
                """;

            var list = jdbcTemplate.query(sql, (rs, rn) -> {
                UserEntity u = new UserEntity();
                u.setUserId(rs.getInt("users_id"));
                u.setName(rs.getString("name"));
                u.setPhoneNumber(rs.getString("phone_number"));
                u.setRadioNumber(rs.getString("radio_number"));
                u.setProfilePhotoUrl(rs.getString("profile_photo_url"));
                // ID 값을 String으로 저장 (나중에 ProfileService에서 Integer로 변환)
                u.setDepartment(String.valueOf(rs.getInt("department_id")));
                u.setPosition(String.valueOf(rs.getInt("position_id")));
                return u;
            }, userId);

            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            System.err.println("Error finding user by ID: " + userId + ", Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void updateProfile(int userId,
                              String phoneNumber,
                              String radioNumber,
                              String profilePhotoUrl) {
        String sql = "UPDATE users " +
                "SET phone_number = ?, " +
                "    radio_number = ?, " +
                "    profile_photo_url = ?, " +
                "    updated_at = NOW() " +
                "WHERE users_id = ?";

        jdbcTemplate.update(sql,
                phoneNumber,
                radioNumber,
                profilePhotoUrl,
                userId
        );
    }

    // 모든 프로필 정보 업데이트 (name, department_id, position_id 포함)
    public void updateAllProfile(int userId,
                                String name,
                                String phoneNumber,
                                String radioNumber,
                                String department,
                                String position,
                                String profilePhotoUrl) {
        String sql = "UPDATE users " +
                "SET name = ?, " +
                "    phone_number = ?, " +
                "    radio_number = ?, " +
                "    department_id = ?, " +
                "    position_id = ?, " +
                "    profile_photo_url = ?, " +
                "    updated_at = NOW() " +
                "WHERE users_id = ?";

        // String을 Integer로 변환
        Integer departmentId = convertToInteger(department);
        Integer positionId = convertToInteger(position);

        jdbcTemplate.update(sql,
                name,
                phoneNumber,
                radioNumber,
                departmentId,
                positionId,
                profilePhotoUrl,
                userId
        );
    }

    // String을 Integer로 변환하는 헬퍼 메서드
    private Integer convertToInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null; // 변환 실패시 null 반환
        }
    }

//    사진업로드
    public int updateProfilePhoto(int userId, String photoUrl) {
        String sql = "UPDATE users SET profile_photo_url = ?, updated_at = NOW() WHERE users_id = ?";
        return jdbcTemplate.update(sql, photoUrl, userId);
    }

    // 관리자 기능: 모든 사용자 목록 조회 (페이지네이션)
    public List<UserEntity> findAllUsers(int page, int size) {
        String sql = """
            SELECT u.users_id, u.name, u.email, u.phone_number, u.radio_number, 
                   u.department_id, u.position_id, u.role, u.profile_photo_url, u.created_at, u.updated_at,
                   COALESCE(d.name, 'N/A') as department_name, 
                   COALESCE(p.name, 'N/A') as position_name
            FROM users u
            LEFT JOIN department d ON u.department_id = d.department_id  
            LEFT JOIN position p ON u.position_id = p.position_id
            ORDER BY u.users_id ASC 
            LIMIT ? OFFSET ?
            """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserEntity user = new UserEntity();
            user.setUserId(rs.getInt("users_id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setPhoneNumber(rs.getString("phone_number"));
            user.setRadioNumber(rs.getString("radio_number"));
            // ID 값을 String으로 저장 (나중에 AdminService에서 Integer로 변환)
            user.setDepartment(String.valueOf(rs.getInt("department_id")));
            user.setPosition(String.valueOf(rs.getInt("position_id")));
            user.setRole(rs.getInt("role"));
            user.setProfilePhotoUrl(rs.getString("profile_photo_url"));
            return user;
        }, size, page * size);
    }

    // 관리자 기능: 부서별 사용자 조회
    public List<UserEntity> findUsersByDepartment(String department, int page, int size) {
        String sql = """
            SELECT u.users_id, u.name, u.email, u.phone_number, u.radio_number, 
                   u.department_id, u.position_id, u.role, u.profile_photo_url, u.created_at, u.updated_at,
                   COALESCE(d.name, 'N/A') as department_name, 
                   COALESCE(p.name, 'N/A') as position_name
            FROM users u
            LEFT JOIN department d ON u.department_id = d.department_id  
            LEFT JOIN position p ON u.position_id = p.position_id
            WHERE d.name = ?
            ORDER BY u.users_id ASC 
            LIMIT ? OFFSET ?
            """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserEntity user = new UserEntity();
            user.setUserId(rs.getInt("users_id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setPhoneNumber(rs.getString("phone_number"));
            user.setRadioNumber(rs.getString("radio_number"));
            // ID 값을 String으로 저장 (나중에 AdminService에서 Integer로 변환)
            user.setDepartment(String.valueOf(rs.getInt("department_id")));
            user.setPosition(String.valueOf(rs.getInt("position_id")));
            user.setRole(rs.getInt("role"));
            user.setProfilePhotoUrl(rs.getString("profile_photo_url"));
            return user;
        }, department, size, page * size);
    }

    // 관리자 기능: 사용자 권한 업데이트
    public int updateUserRole(int userId, int newRole) {
        String sql = "UPDATE users SET role = ?, updated_at = NOW() WHERE users_id = ?";
        return jdbcTemplate.update(sql, newRole, userId);
    }

    /**
     * 사용자 삭제 (계단식 삭제)
     * 1. 해당 사용자의 모든 댓글 삭제
     * 2. 해당 사용자가 작성한 모든 게시물 삭제
     * 3. 해당 사용자가 확인/조치한 게시물의 참조 정보 초기화
     * 4. 사용자 삭제
     */
    @Transactional
    public void deleteUser(int userId) {
        // 1. 해당 사용자의 모든 댓글 삭제
        String deleteCommentsSql = "DELETE FROM comment WHERE user_id = ?";
        int deletedComments = jdbcTemplate.update(deleteCommentsSql, userId);
        System.out.println("Deleted " + deletedComments + " comments for user " + userId);
        
        // 2. 해당 사용자가 작성한 모든 게시물 삭제
        String deletePostsSql = "DELETE FROM post WHERE reporter_id = ?";
        int deletedPosts = jdbcTemplate.update(deletePostsSql, userId);
        System.out.println("Deleted " + deletedPosts + " posts for user " + userId);
        
        // 3. 해당 사용자가 확인/조치한 게시물의 참조 정보 초기화
        String updateCheckedPostsSql = "UPDATE post SET is_checked_id = NULL, is_checked_at = NULL WHERE is_checked_id = ?";
        int updatedCheckedPosts = jdbcTemplate.update(updateCheckedPostsSql, userId);
        System.out.println("Updated " + updatedCheckedPosts + " checked posts for user " + userId);
        
        String updateActionPostsSql = "UPDATE post SET action_taker_id = NULL, is_action_taken_at = NULL WHERE action_taker_id = ?";
        int updatedActionPosts = jdbcTemplate.update(updateActionPostsSql, userId);
        System.out.println("Updated " + updatedActionPosts + " action posts for user " + userId);
        
        // 4. 사용자 삭제
        String deleteUserSql = "DELETE FROM users WHERE users_id = ?";
        int deletedUser = jdbcTemplate.update(deleteUserSql, userId);
        System.out.println("Deleted " + deletedUser + " user with id " + userId);
        
        if (deletedUser == 0) {
            throw new RuntimeException("사용자를 찾을 수 없습니다: " + userId);
        }
    }

    /**
     * 사용자 삭제 전 참조 데이터 확인
     */
    public UserReferenceInfo getUserReferenceInfo(int userId) {
        // 댓글 개수 확인
        String checkCommentsSql = "SELECT COUNT(*) FROM comment WHERE user_id = ?";
        Integer commentCount = jdbcTemplate.queryForObject(checkCommentsSql, Integer.class, userId);
        
        // 작성한 게시물 개수 확인
        String checkPostsSql = "SELECT COUNT(*) FROM post WHERE reporter_id = ?";
        Integer postCount = jdbcTemplate.queryForObject(checkPostsSql, Integer.class, userId);
        
        // 확인한 게시물 개수 확인
        String checkCheckedPostsSql = "SELECT COUNT(*) FROM post WHERE is_checked_id = ?";
        Integer checkedPostCount = jdbcTemplate.queryForObject(checkCheckedPostsSql, Integer.class, userId);
        
        // 조치한 게시물 개수 확인
        String checkActionPostsSql = "SELECT COUNT(*) FROM post WHERE action_taker_id = ?";
        Integer actionPostCount = jdbcTemplate.queryForObject(checkActionPostsSql, Integer.class, userId);
        
        return new UserReferenceInfo(
            commentCount != null ? commentCount : 0,
            postCount != null ? postCount : 0,
            checkedPostCount != null ? checkedPostCount : 0,
            actionPostCount != null ? actionPostCount : 0
        );
    }

    /**
     * 참조 정보를 담는 내부 클래스
     */
    public static class UserReferenceInfo {
        private final int commentCount;
        private final int postCount;
        private final int checkedPostCount;
        private final int actionPostCount;
        
        public UserReferenceInfo(int commentCount, int postCount, int checkedPostCount, int actionPostCount) {
            this.commentCount = commentCount;
            this.postCount = postCount;
            this.checkedPostCount = checkedPostCount;
            this.actionPostCount = actionPostCount;
        }
        
        public int getCommentCount() { return commentCount; }
        public int getPostCount() { return postCount; }
        public int getCheckedPostCount() { return checkedPostCount; }
        public int getActionPostCount() { return actionPostCount; }
        public boolean hasReferences() { 
            return commentCount > 0 || postCount > 0 || checkedPostCount > 0 || actionPostCount > 0; 
        }
    }

    /**
     * 사용자 정보를 부서명과 직책명과 함께 조회
     */
    public UserEntity findByIdWithDepartmentAndPosition(int userId) {
        String sql = """
            SELECT u.users_id, u.name, u.department_id, u.position_id,
                   COALESCE(d.name, 'N/A') as department_name, 
                   COALESCE(p.name, 'N/A') as position_name
            FROM users u
            LEFT JOIN department d ON u.department_id = d.department_id  
            LEFT JOIN position p ON u.position_id = p.position_id
            WHERE u.users_id = ?
            """;
        
        var list = jdbcTemplate.query(sql, (rs, rn) -> {
            UserEntity user = new UserEntity();
            user.setUserId(rs.getInt("users_id"));
            user.setName(rs.getString("name"));
            user.setDepartment(String.valueOf(rs.getInt("department_id")));
            user.setPosition(String.valueOf(rs.getInt("position_id")));
            user.setDepartmentName(rs.getString("department_name"));
            user.setPositionName(rs.getString("position_name"));
            return user;
        }, userId);
        
        return list.isEmpty() ? null : list.get(0);
    }

}
