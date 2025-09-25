package com.github.service;

import com.github.dto.ProfileResponse;
import com.github.dto.ProfileUpdateRequest;
import com.github.entity.UserEntity;
import com.github.repository.UserJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserJdbcRepository userJdbcRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${api.base-url}")
    private String baseUrl;

    public ProfileResponse getMyProfile(int userId) {

        UserEntity u = userJdbcRepository.findById(userId); //userId로 엔티티 조회
        if (u == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        return ProfileResponse.builder()
                .userId(u.getUserId())
                .name(u.getName())
                .phoneNumber(u.getPhoneNumber())
                .radioNumber(u.getRadioNumber())
                .department(convertToInteger(u.getDepartment()))
                .position(convertToInteger(u.getPosition()))
                .profilePhotoUrl(convertToFullUrl(u.getProfilePhotoUrl()))
                .build();
    }


    /** (기존) 사진만 업로드 — 내부 공통 저장 함수를 사용하도록 보완 */
    public String uploadProfilePhoto(int userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }
        UserEntity exists = userJdbcRepository.findById(userId);
        if (exists == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }
        String publicUrl = saveProfilePhoto(userId, file); // ★ 공통 함수 사용
        userJdbcRepository.updateProfilePhoto(userId, publicUrl);
        return publicUrl;
    }

    /** ✅ 통합: JSON(프로필) + 파일(선택) 한 번에 처리 */
    @Transactional
    public ProfileResponse updateMyProfileAndPhoto(int userId, ProfileUpdateRequest req, MultipartFile file) throws IOException {
        // 사용자 존재 여부 확인
        UserEntity exists = userJdbcRepository.findById(userId);
        if (exists == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }
        
        // 권한 확인: 사용자가 자신의 프로필만 수정할 수 있도록 보장
        validateUserPermission(userId, exists);

        // 1) 파일이 왔다면 먼저 저장하고 URL 갱신
        String publicUrlFromFile = null;
        if (file != null && !file.isEmpty()) {
            publicUrlFromFile = saveProfilePhoto(userId, file);
        }

        // 2) 모든 필드 갱신 (req가 null이어도 기존 값 유지)
        String name = (req != null && req.getName() != null) ? req.getName() : exists.getName();
        String phoneNumber = (req != null && req.getPhoneNumber() != null) ? req.getPhoneNumber() : exists.getPhoneNumber();
        String radioNumber = (req != null && req.getRadioNumber() != null) ? req.getRadioNumber() : exists.getRadioNumber();
        String department = (req != null && req.getDepartment() != null) ? req.getDepartment() : exists.getDepartment();
        String position = (req != null && req.getPosition() != null) ? req.getPosition() : exists.getPosition();
        String finalPhotoUrl = (publicUrlFromFile != null) ? publicUrlFromFile : 
                              (req != null && req.getProfilePhotoUrl() != null) ? req.getProfilePhotoUrl() : exists.getProfilePhotoUrl();

        userJdbcRepository.updateAllProfile(
                userId,
                name,
                phoneNumber,
                radioNumber,
                department,
                position,
                finalPhotoUrl
        );

        // 3) 최종 상태 재조회 후 반환
        UserEntity u = userJdbcRepository.findById(userId);
        return ProfileResponse.builder()
                .userId(u.getUserId())
                .name(u.getName())
                .phoneNumber(u.getPhoneNumber())
                .radioNumber(u.getRadioNumber())
                .department(convertToInteger(u.getDepartment()))
                .position(convertToInteger(u.getPosition()))
                .profilePhotoUrl(convertToFullUrl(u.getProfilePhotoUrl()))
                .build();
    }

    /** 내부 공통: 실제 파일 저장 + 공개 URL 생성 */
    private String saveProfilePhoto(int userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String storedName = "profile_" + userId + "_" + UUID.randomUUID() + ext;
        Path target = baseDir.resolve(storedName);

        // ★ 같은 이름 우연 충돌 방지: 덮어쓰기 옵션 추가
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // 완전한 URL로 반환 (도메인 + 경로)
        return baseUrl + "/uploads/" + storedName;
    }

    /** 상대 경로를 완전한 URL로 변환 */
    private String convertToFullUrl(String photoUrl) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return null;
        }
        
        // 이미 완전한 URL인 경우 그대로 반환
        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            return photoUrl;
        }
        
        // 상대 경로인 경우 완전한 URL로 변환
        if (photoUrl.startsWith("/uploads/")) {
            return baseUrl + photoUrl;
        }
        
        // 다른 형태의 경로인 경우 기본 도메인 추가
        return baseUrl + "/uploads/" + photoUrl;
    }

    /** 권한 확인: 사용자가 자신의 프로필만 수정할 수 있도록 보장 */
    private void validateUserPermission(int userId, UserEntity user) {
        if (user.getUserId() == null || !user.getUserId().equals(userId)) {
            throw new RuntimeException("자신의 프로필만 수정할 수 있습니다.");
        }
    }

    /** String을 Integer로 변환하는 헬퍼 메서드 */
    private Integer convertToInteger(String value) {
        if (value == null || value.trim().isEmpty() || "N/A".equals(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null; // 변환 실패시 null 반환
        }
    }

}