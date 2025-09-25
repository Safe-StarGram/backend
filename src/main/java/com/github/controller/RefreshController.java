package com.github.controller;

import com.github.dto.TokenResponse;
import com.github.jwt.JwtTokenProvider;
import com.github.token.RefreshTokenStore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RefreshController {

    private final JwtTokenProvider jwtProvider;
    private final RefreshTokenStore tokenStore;

    @PostMapping("/auto-refresh")
    public ResponseEntity<TokenResponse> autoRefresh(@RequestBody(required = false) Map<String, String> requestBody, HttpServletRequest request, HttpServletResponse response) {
        try {
            System.out.println("=== 자동 토큰 갱신 시작 ===");
            
            // 먼저 요청 본문에서 refresh token 추출 시도
            String refreshToken = null;
            if (requestBody != null) {
                refreshToken = requestBody.get("refresh");
            }
            
            // 요청 본문에 refresh token이 없으면 쿠키에서 추출
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                System.out.println("요청 본문에 refresh 토큰이 없습니다. 쿠키에서 추출을 시도합니다.");
                refreshToken = extractTokenFromCookie(request, "refreshToken");
            }
            
            // 쿠키에도 refresh token이 없으면 에러 반환
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                System.out.println("쿠키에도 refresh 토큰이 없습니다.");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh 토큰이 필요합니다. 다시 로그인해주세요.");
            }

            System.out.println("리프레시 토큰 발견: " + refreshToken.substring(0, 20) + "...");

            // refresh token 검증 (JTI 포함)
            boolean isValid = jwtProvider.validateRefreshToken(refreshToken);
            if (!isValid) {
                System.out.println("유효하지 않은 리프레시 토큰입니다.");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다. 다시 로그인해주세요.");
            }

            // 사용자 정보 추출
            long userId = jwtProvider.getUserId(refreshToken);
            String role = jwtProvider.getRole(refreshToken);
            String jti = jwtProvider.getJti(refreshToken);

            System.out.println("사용자 ID: " + userId + ", 역할: " + role + ", JTI: " + jti);

            // Redis에서 refresh token 확인
            if (!tokenStore.exists(userId, jti)) {
                System.out.println("Redis에서 리프레시 토큰을 찾을 수 없습니다.");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다. 다시 로그인해주세요.");
            }

            // 기존 refresh token은 유지하고 새로운 access token만 생성
            String newAccessToken = jwtProvider.generateAccessToken(userId, role);
            
            // refresh token은 그대로 유지 (삭제하지 않음)
            // 새로운 refresh token 생성 및 저장하지 않음

            System.out.println("새로운 액세스 토큰 생성 완료");
            System.out.println("=== 자동 토큰 갱신 완료 ===");

            // Access token을 응답 헤더에 설정
            response.setHeader("Authorization", "Bearer " + newAccessToken);
            
            return ResponseEntity.ok(TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .tokenType("Bearer")
                    .expiresIn(600) // 10분 (600초)
                    .userId(userId)
                    .build());

        } catch (ResponseStatusException e) {
            System.out.println("토큰 갱신 실패: " + e.getReason());
            throw e;
        } catch (Exception e) {
            System.out.println("토큰 갱신 중 예상치 못한 오류: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 갱신 중 오류가 발생했습니다.");
        }
    }

    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }


    @GetMapping("/debug-cookies")
    public ResponseEntity<Map<String, Object>> debugCookies(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> cookies = new HashMap<>();
        
        Cookie[] cookieArray = request.getCookies();
        if (cookieArray != null) {
            for (Cookie cookie : cookieArray) {
                cookies.put(cookie.getName(), cookie.getValue());
                System.out.println("쿠키 발견: " + cookie.getName() + " = " + cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "...");
            }
        } else {
            System.out.println("쿠키가 없습니다.");
        }
        
        result.put("cookies", cookies);
        result.put("cookieCount", cookies.size());
        result.put("hasRefreshToken", cookies.containsKey("refreshToken"));
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            System.out.println("=== 로그아웃 시작 ===");
            
            // 쿠키에서 refresh token 추출
            String refreshToken = extractTokenFromCookie(request, "refreshToken");
            
            if (refreshToken != null && !refreshToken.trim().isEmpty()) {
                // refresh token에서 사용자 정보 추출
                try {
                    long userId = jwtProvider.getUserId(refreshToken);
                    String jti = jwtProvider.getJti(refreshToken);
                    
                    // refresh token 삭제
                    tokenStore.delete(userId, jti);
                    System.out.println("Refresh token 삭제 완료: userId=" + userId + ", jti=" + jti);
                } catch (Exception e) {
                    System.out.println("Refresh token 파싱 실패: " + e.getMessage());
                }
            }
            
            // 쿠키 삭제
            Cookie cookie = new Cookie("refreshToken", "");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(0); // 즉시 삭제
            response.addCookie(cookie);
            
            // 응답 헤더에도 쿠키 삭제 설정
            response.addHeader("Set-Cookie", 
                "refreshToken=; Path=/; Max-Age=0; HttpOnly=true; Secure=true; SameSite=None");
            
            System.out.println("=== 로그아웃 완료 ===");
            
            Map<String, String> result = new HashMap<>();
            result.put("message", "로그아웃되었습니다.");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.out.println("로그아웃 중 오류: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "로그아웃 중 오류가 발생했습니다.");
        }
    }
}
