package com.github.exception;

import com.github.dto.ErrorResponse;
import com.github.dto.ValidationErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 공통 에러 응답 생성 메서드
    private ErrorResponse createErrorResponse(int status, String error, String message) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .build();
    }

    // HTTP 상태 코드에 따른 적절한 에러 메시지 반환
    private String getErrorMessageForStatus(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "Error";
        };
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex, WebRequest request) {
        
        log.error("ResponseStatusException: {}", ex.getReason(), ex);
        
        // HTTP 상태 코드에 따른 적절한 에러 메시지 설정
        String errorMessage = getErrorMessageForStatus(ex.getStatusCode().value());
        
        ErrorResponse errorResponse = createErrorResponse(
            ex.getStatusCode().value(),
            errorMessage,
            ex.getReason()
        );
        
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        log.error("IllegalArgumentException: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = createErrorResponse(
            400,
            "Bad Request",
            ex.getMessage()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        log.error("Validation error: {}", ex.getMessage(), ex);
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .message("입력값 검증에 실패했습니다.")
                .errors(errors)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        log.error("Authentication error: {}", ex.getMessage(), ex);
        
        String message = ex instanceof BadCredentialsException ? 
            "이메일 또는 비밀번호가 올바르지 않습니다." : "인증에 실패했습니다.";
        
        ErrorResponse errorResponse = createErrorResponse(401, "Unauthorized", message);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        log.error("Access denied: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = createErrorResponse(403, "Forbidden", "접근 권한이 없습니다.");
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // BadCredentialsException은 AuthenticationException의 하위 클래스이므로 별도 핸들러 불필요

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = createErrorResponse(500, "Internal Server Error", "서버 내부 오류가 발생했습니다.");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
