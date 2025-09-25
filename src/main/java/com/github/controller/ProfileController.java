package com.github.controller;

import com.github.dto.ProfileResponse;
import com.github.dto.ProfileUpdateRequest;
import com.github.jwt.JwtTokenProvider;
import com.github.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final JwtTokenProvider jwtTokenProvider;

    // 내 프로필 조회
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserId(token);
        return ResponseEntity.ok(profileService.getMyProfile(userId.intValue()));
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            // 토큰 유효성 검증
            if (!jwtTokenProvider.validate(token)) {
                throw new RuntimeException("유효하지 않은 토큰입니다.");
            }
            return token;
        }
        throw new RuntimeException("인증 토큰이 필요합니다.");
    }

    // 내 프로필 수정
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> updateMyProfile(
            HttpServletRequest request,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "radioNumber", required = false) String radioNumber,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "position", required = false) String position,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws Exception {
        String token = extractTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserId(token);
        
        // ProfileUpdateRequest 객체 생성
        ProfileUpdateRequest req = ProfileUpdateRequest.builder()
                .name(name)
                .phoneNumber(phoneNumber)
                .radioNumber(radioNumber)
                .department(department)
                .position(position)
                .build();
        
        return ResponseEntity.ok(
                profileService.updateMyProfileAndPhoto(userId.intValue(), req, image)
        );
    }
}
