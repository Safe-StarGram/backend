package com.github.repository;

import com.github.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

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
            SELECT users_id, name, email, phone_number, radio_number, 
                   department, position, role, profile_photo_url, created_at, updated_at
            FROM users 
            ORDER BY users_id ASC 
            LIMIT ? OFFSET ?
            """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserEntity user = new UserEntity();
            user.setUserId(rs.getInt("users_id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setPhoneNumber(rs.getString("phone_number"));
            user.setRadioNumber(rs.getString("radio_number"));
            user.setDepartment(rs.getString("department"));
            user.setPosition(rs.getString("position"));
            user.setRole(rs.getInt("role"));
            user.setProfilePhotoUrl(rs.getString("profile_photo_url"));
            return user;
        }, size, page * size);
    }

    // 관리자 기능: 부서별 사용자 조회
    public List<UserEntity> findUsersByDepartment(String department, int page, int size) {
        String sql = """
            SELECT users_id, name, email, phone_number, radio_number, 
                   department, position, role, profile_photo_url, created_at, updated_at
            FROM users 
            WHERE department = ?
            ORDER BY users_id ASC 
            LIMIT ? OFFSET ?
            """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserEntity user = new UserEntity();
            user.setUserId(rs.getInt("users_id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setPhoneNumber(rs.getString("phone_number"));
            user.setRadioNumber(rs.getString("radio_number"));
            user.setDepartment(rs.getString("department"));
            user.setPosition(rs.getString("position"));
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


}
