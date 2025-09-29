package com.github.service;

import com.github.dto.LoginRequest;
import com.github.dto.SignUpDto;
import com.github.entity.UserEntity;
import com.github.exception.EmailAlreadyExistsException;
import com.github.repository.UserJdbcRepository;
import com.github.token.RefreshTokenStore;
import com.github.dto.TokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.jwt.JwtProperties;
import com.github.jwt.JwtTokenProvider;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserJdbcRepository userJdbcRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt
    private final JwtTokenProvider jwt;
    private final JwtProperties props;
    private final RefreshTokenStore refreshStore;


    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true); // XSS 공격 방지 (백엔드에서만 접근 가능)
        cookie.setSecure(true); // HTTPS에서만 전송 (프로덕션 환경)
        cookie.setPath("/"); // 모든 경로에서 쿠키 사용 가능
        cookie.setMaxAge(60 * 60 * 24 * 13); // 13일 (refresh token 만료시간과 동일)
        
        // 기본 쿠키 설정
        response.addCookie(cookie);
        
        // 응답 헤더에 직접 쿠키 설정 (SameSite 속성 포함)
        response.addHeader("Set-Cookie", 
            "refreshToken=" + refreshToken + 
            "; Path=/; Max-Age=" + (60 * 60 * 24 * 13) + 
            "; HttpOnly=true; Secure=true; SameSite=None");
        
        System.out.println("AuthService - Refresh Token 쿠키 설정 완료");
        System.out.println("쿠키 이름: " + cookie.getName());
        System.out.println("쿠키 값 길이: " + refreshToken.length());
        System.out.println("쿠키 MaxAge: " + cookie.getMaxAge());
        System.out.println("쿠키 Path: " + cookie.getPath());
        System.out.println("쿠키 설정: HttpOnly=true, Secure=true, SameSite=None");
    }




    @Transactional
    public void join(SignUpDto signUpDto) {

        if (userJdbcRepository.existsByEmail(signUpDto.getEmail())) {
            throw new EmailAlreadyExistsException("이미 존재하는 이메일입니다.");
        }

        UserEntity userEntity = UserEntity.builder()
                .email(signUpDto.getEmail())
                .name(signUpDto.getName())
                .password(passwordEncoder.encode(signUpDto.getPassword()))
                .build();

        userJdbcRepository.save(userEntity);
    }


    public TokenResponse login(LoginRequest request, HttpServletResponse response) {
        UserEntity user = userJdbcRepository.findByEmail(request.getEmail());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        long uid = user.getUserId().longValue();
        
        // 데이터베이스의 role 정보를 사용 (1: 관리자, 0 또는 null: 일반사용자)
        String role;
        if (user.getRole() != null && user.getRole() == 1) {
            role = "ROLE_ADMIN";
        } else {
            role = "ROLE_USER";
        }

        String access = jwt.generateAccessToken(uid, role);
        String jti = jwt.newJti();
        String refresh = jwt.generateRefreshToken(uid, role, jti);
        refreshStore.save(uid, jti);

        // RefreshToken을 쿠키로 설정
        setRefreshTokenCookie(response, refresh);

        return TokenResponse.builder()
                .accessToken(access)
                .tokenType("Bearer")
                .expiresIn(props.getAccessTtl().toSeconds())
                .userId(user.getUserId().longValue())
                .role(role)  // 응답에 role 정보 추가
                .build();
    }


}

