package com.ssafy.s14p11c204.server.domain.user.api;

import com.ssafy.s14p11c204.server.domain.user.api.kakaoOauth.KakaoLoginController;
import com.ssafy.s14p11c204.server.domain.user.api.naverOauth.NaverLoginController;
import com.ssafy.s14p11c204.server.domain.user.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(assignableTypes = {
        AuthControllerV0.class,
        NaverLoginController.class, // ✅ 네이버 로그인 추가
        KakaoLoginController.class // ✅ 카카오 로그인 추가
})
public class AuthExceptionHandler {

    /**
     * [400 BAD REQUEST]
     * 
     * @Valid 검증 실패 시 발생
     *        실제 검증 실패한 필드와 메시지를 반환합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        // ✅ 실제 검증 실패 메시지 추출
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("입력 데이터 검증 실패: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("validation_error", message));
    }

    /**
     * [400 BAD REQUEST]
     * 잘못된 인자 전달 시 발생
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.warn("잘못된 요청: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("invalid_request", ex.getMessage()));
    }

    /**
     * [401 UNAUTHORIZED]
     * 로그인 실패 시 발생
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex) {

        log.warn("로그인 실패: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "invalid_credentials",
                        "아이디 또는 비밀번호가 일치하지 않습니다."));
    }

    /**
     * [409 CONFLICT]
     * 이미 가입된 이메일 등 비즈니스 로직 충돌 시 발생
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            IllegalStateException ex) {

        log.warn("리소스 충돌: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("resource_conflict", ex.getMessage()));
    }

    /**
     * [500 INTERNAL SERVER ERROR]
     * 위에서 정의하지 않은 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllException(Exception ex) {
        log.error("예상치 못한 서버 오류 발생", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "internal_error",
                        "서버 내부 오류가 발생했습니다."));
    }

    // AuthExceptionHandler 클래스 안에 추가

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            org.springframework.web.server.ResponseStatusException ex) {

        log.warn("응답 상태 예외 발생: {}", ex.getReason());

        return ResponseEntity.status(ex.getStatusCode())
                .body(new ErrorResponse("resource_conflict", ex.getReason()));
    }
}
