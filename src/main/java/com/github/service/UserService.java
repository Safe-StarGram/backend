package com.github.service;

import com.github.dto.UserInfoResponse;
import com.github.entity.UserEntity;
import com.github.repository.UserJdbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserJdbcRepository userRepository;

    /**
     * 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Integer userId) {
        log.info("사용자 정보 조회 시작: userId={}", userId);
        
        UserEntity user = userRepository.findByIdWithDepartmentAndPosition(userId);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다: " + userId);
        }
        
        UserInfoResponse response = UserInfoResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .position(convertToInteger(user.getPosition()))
                .department(convertToInteger(user.getDepartment()))
                .build();
        
        log.info("사용자 정보 조회 완료: userId={}, name={}", userId, user.getName());
        return response;
    }

    /**
     * String을 Integer로 변환하는 헬퍼 메서드
     */
    private Integer convertToInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
