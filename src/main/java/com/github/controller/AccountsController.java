package com.github.controller;


import com.github.dto.LoginRequest;
import com.github.dto.SignUpDto;
import com.github.service.AuthService;
import com.github.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "사용자 인증 관련 API")
public class AccountsController {

    private final AuthService authService;

    @PostMapping("/join")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    public ResponseEntity<String> join(@Valid @RequestBody SignUpDto signUpDto) {
        authService.join(signUpDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공입니다.");
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return authService.login(request, response);
    }


}
