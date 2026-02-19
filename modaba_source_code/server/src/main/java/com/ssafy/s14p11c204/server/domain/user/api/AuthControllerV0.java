package com.ssafy.s14p11c204.server.domain.user.api;

import com.ssafy.s14p11c204.server.domain.user.dto.*;
import com.ssafy.s14p11c204.server.domain.user.service.AuthService;
import com.ssafy.s14p11c204.server.domain.user.dto.*;
import com.ssafy.s14p11c204.server.global.util.JwtTokenProvider;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v0/auth")
@Tag(name = "AuthController", description = "가입된 사용자의 인증을 위한 API")
@RestController
class AuthControllerV0 {
    private final AuthService authService;

    @PostMapping
    @Operation(summary = "회원가입")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "CREATED : 가입에 성공했습니다!"),
            @ApiResponse(responseCode = "409", description = "CONFLICT : 이미 가입된 이메일입니다."),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST : 잘못된 데이터가 들어왔습니다."),
    })
    ResponseEntity<?> signUp(@Valid @RequestBody SignupRequest signupRequest) {
        authService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "회원가입 완료"));
    }

    @PostMapping(value = "/log-in", consumes = "application/json")
    ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/log-out")
    @Operation(summary = "로그아웃", description = "서버의 리프레시 토큰을 삭제하고 인증을 해제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "NO CONTENT : 로그아웃이 완료되었습니다!"),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST : 잘못된 요청입니다. (예: 리프레시 토큰 누락)")
    })
    public ResponseEntity<Void> logOut(@Valid @RequestBody LogoutRequest logoutRequest) {
        authService.logout(logoutRequest.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send-code")
    @Operation(summary = "비밀번호 초기화 메일 요청", description = "비밀번호 초기화 인증코드가 포함된 메일을 전송.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "ACCEPTED : 이메일을 보냈습니다. (없는 이메일이더라도 사용자에게는 비밀)"),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST : 이메일 형식이 잘못됐습니다."),
    })
    ResponseEntity<?> sendCode(@Valid @RequestBody PwForgotRequest request) {
        // 성공/실패 여부와 관계없이 항상 202 반환
        authService.sendCode(request.email());
        return ResponseEntity.accepted()
                .body(Map.of("message", "요청이 처리되었습니다"));
    }

    @PostMapping("/verify-code")
    @Operation(summary = "비밀번호 초기화 인증 코드 검증", description = "비밀번호 초기화 페이지 링크가 포함된 메일을 요청합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "ACCEPTED : 초기화 인증 코드 검증 통과."),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST : 인증코드가 올바르지 않습니다."),
    })
    ResponseEntity<?> verifyCode(@Valid @RequestBody PwResetCodeRequest request) {
        try {
            authService.verifyCode(request.email(), request.code());
        } catch (Exception e) {
            log.error("비밀번호 초기화 인증코드 검증 실패", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            "verify-code-error",
                            e.getMessage()));
        }
        return ResponseEntity.accepted().body(Map.of("message", "비밀번호 초기화 인증코드 검증 통과"));
    }

    @PostMapping("/reset-pw")
    @Operation(summary = "비밀번호 실제 초기화", description = "사용자가 보내온 비밀번호를 새로운 비밀번호로 재설정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK : 비밀번호 재설정이 완료되었습니다!"),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST : 비밀번호 형식이 잘못됐습니다."),
    })
    ResponseEntity<?> resetPw(@Valid @RequestBody PwResetRequest request) {
        try {
            authService.resetPw(request.email(), request.code(), request.password());
        } catch (Exception e) {
            log.error("비밀번호 재설정 실패", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            "reset-password_error",
                            e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("message", "비밀번호 재설정 완료"));
    }

    @PostMapping("/reissue")
    @Operation(summary = "Access Token을 재발급받습니다.", description = "받은 Refresh Token이 정확한지 확인하고, 맞다면 현재 Refresh Token을 파기하고 새로운 Refresh Token과 Access Token을 발급해줍니다.")
    ResponseEntity<Map<String, String>> reissue(@RequestBody TokenReissueRequest request) {

        String accessToken = authService.reissue(request.refreshToken());
        return ResponseEntity.ok().body(Map.of("accessToken", accessToken));
    }
}
