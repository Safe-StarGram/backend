package com.github.token;

import com.github.dto.TokenResponse;
import com.github.entity.UserEntity;
import com.github.jwt.JwtProperties;
import com.github.jwt.JwtTokenProvider;
import com.github.repository.UserJdbcRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler  implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwt;
    private final JwtProperties props;
    private final com.github.token.RefreshTokenStore store;
    private final UserJdbcRepository userRepository;
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) auth.getPrincipal();

        // 공급자별로 프로필 키가 다를 수 있으니 실제 응답에 맞춰 매핑
        String email = String.valueOf(oAuth2User.getAttributes().get("email"));  // google은 기본 제공, kakao는 권한 필요
        String name  = String.valueOf(oAuth2User.getAttributes().get("name"));   // 사용자 이름

        // 이메일로 기존 사용자 조회
        UserEntity existingUser = userRepository.findByEmail(email);
        long userId;
        
        if (existingUser != null) {
            // 기존 사용자가 있으면 해당 사용자 ID 사용
            userId = existingUser.getUserId();
        } else {
            // 새 사용자 생성
            UserEntity newUser = UserEntity.builder()
                    .email(email)
                    .name(name)
                    .password("") // OAuth2 사용자는 패스워드 없음
                    .build();
            Long newUserId = userRepository.save(newUser);
            userId = newUserId != null ? newUserId : 1L; // 기본값 설정
        }

        String role = "ROLE_ADMIN";  // 테스트용: 관리자 권한
        String access = jwt.generateAccessToken(userId, role);
        String jti = jwt.newJti();
        String refresh = jwt.generateRefreshToken(userId, role, jti);
        store.save(userId, jti);

        // RefreshToken을 쿠키로 설정
        Cookie cookie = new Cookie("refreshToken", refresh);
        cookie.setHttpOnly(true); // XSS 공격 방지 (백엔드에서만 접근 가능)
        cookie.setSecure(true); // HTTPS에서만 전송 (프로덕션 환경)
        cookie.setPath("/"); // 모든 경로에서 쿠키 사용 가능
        cookie.setMaxAge(60 * 60 * 24 * 13); // 13일 (refresh token 만료시간과 동일)
        res.addCookie(cookie);
        
        // 응답 헤더에 직접 쿠키 설정 (SameSite 속성 포함)
        res.addHeader("Set-Cookie", 
            "refreshToken=" + refresh + 
            "; Path=/; Max-Age=" + (60 * 60 * 24 * 13) + 
            "; HttpOnly=true; Secure=true; SameSite=None");
        
        System.out.println("OAuth2 로그인 성공 - Refresh Token 쿠키 설정 완료");
        System.out.println("쿠키 값 길이: " + refresh.length());

        TokenResponse body = TokenResponse.builder()
                .accessToken(access)
                .tokenType("Bearer")
                .expiresIn(props.getAccessTtl().toSeconds())
                .userId(userId)
                .build();

        res.setStatus(200);
        res.setContentType("application/json;charset=UTF-8");
        om.writeValue(res.getWriter(), body);
    }
}
