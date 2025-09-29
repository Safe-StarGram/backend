package com.github.service;

import com.github.dto.AdminUserResponse;
import com.github.entity.UserEntity;
import com.github.repository.UserJdbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserJdbcRepository userRepository;

    /**
     * 모든 사용자 목록을 조회합니다 (관리자 일람)
     * @param department 부서별 필터링 (선택사항)
     * @return 사용자 목록
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers(String department) {
        log.info("=== 관리자 일람 조회 시작 ===");
        log.info("Department: {}", department);
        
        List<UserEntity> users;
        if (department != null && !department.trim().isEmpty()) {
            users = userRepository.findUsersByDepartment(department, 0, 100); // 모든 사용자 조회
        } else {
            users = userRepository.findAllUsers(0, 100); // 모든 사용자 조회
        }
        
        List<AdminUserResponse> result = users.stream()
                .map(this::convertToAdminUserResponse)
                .collect(Collectors.toList());
        
        log.info("조회된 사용자 수: {}", result.size());
        log.info("=== 관리자 일람 조회 완료 ===");
        return result;
    }

    /**
     * 특정 사용자의 권한을 변경합니다
     * @param userId 사용자 ID
     * @param grantPermission true: 관리자 권한 부여, false: 관리자 권한 제거
     * @return 변경된 사용자 정보
     */
    @Transactional
    public AdminUserResponse updateUserPermission(int userId, boolean grantPermission) {
        log.info("=== 사용자 권한 변경 시작 ===");
        log.info("UserId: {}, GrantPermission: {}", userId, grantPermission);
        
        // 새로운 역할 설정 (1: 관리자, 0: 일반사용자)
        int newRole = grantPermission ? 1 : 0;
        
        // 권한 업데이트
        int updatedRows = userRepository.updateUserRole(userId, newRole);
        if (updatedRows == 0) {
            throw new RuntimeException("사용자를 찾을 수 없습니다: " + userId);
        }
        
        // 업데이트된 사용자 정보 조회
        UserEntity updatedUser = userRepository.findById(userId);
        if (updatedUser == null) {
            throw new RuntimeException("사용자 정보를 조회할 수 없습니다: " + userId);
        }
        
        AdminUserResponse result = convertToAdminUserResponse(updatedUser);
        log.info("권한 변경 완료: {} -> {}", 
                grantPermission ? "일반사용자" : "관리자", 
                grantPermission ? "관리자" : "일반사용자");
        log.info("=== 사용자 권한 변경 완료 ===");
        
        return result;
    }


    /**
     * UserEntity를 AdminUserResponse로 변환합니다
     */
    private AdminUserResponse convertToAdminUserResponse(UserEntity user) {
        boolean hasAdminPermission = user.getRole() != null && user.getRole() == 1;
        
        return AdminUserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .radioNumber(user.getRadioNumber())
                .department(user.getDepartment())
                .position(user.getPosition())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .hasAdminPermission(hasAdminPermission)
                .role(user.getRole()) // role 정보 추가
                .build();
    }

    /**
     * 사용자 삭제 (관리자 권한 필요)
     * 참조 데이터도 함께 삭제됩니다.
     */
    @Transactional
    public void deleteUser(int userId) {
        // 사용자 존재 여부 확인
        UserEntity user = userRepository.findById(userId);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다: " + userId);
        }

        // 참조 데이터 확인
        UserJdbcRepository.UserReferenceInfo refInfo = userRepository.getUserReferenceInfo(userId);
        if (refInfo.hasReferences()) {
            System.out.println("User " + userId + " 삭제 시 함께 삭제될 데이터:");
            System.out.println("- Comments: " + refInfo.getCommentCount() + "개");
            System.out.println("- Posts: " + refInfo.getPostCount() + "개");
            System.out.println("- Checked Posts: " + refInfo.getCheckedPostCount() + "개");
            System.out.println("- Action Posts: " + refInfo.getActionPostCount() + "개");
        }

        // 계단식 삭제 실행
        userRepository.deleteUser(userId);
        
        System.out.println("User " + userId + " 삭제 완료");
    }

    /**
     * 사용자 삭제 전 참조 데이터 정보 조회
     */
    public UserJdbcRepository.UserReferenceInfo getUserReferenceInfo(int userId) {
        return userRepository.getUserReferenceInfo(userId);
    }
}
