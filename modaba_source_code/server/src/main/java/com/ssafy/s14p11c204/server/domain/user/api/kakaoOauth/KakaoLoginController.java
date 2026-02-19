package com.ssafy.s14p11c204.server.domain.user.api.kakaoOauth;

import com.ssafy.s14p11c204.server.domain.user.api.kakaoOauth.vo.KakaoLoginProfile;
import com.ssafy.s14p11c204.server.domain.user.api.kakaoOauth.vo.KakaoLoginVo;
import com.ssafy.s14p11c204.server.domain.user.dto.ErrorResponse;
import com.ssafy.s14p11c204.server.domain.user.dto.LoginResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v0/oauth/kakao")
@RequiredArgsConstructor
@Tag(name = "Kakao OAuth", description = "카카오 소셜 로그인 API")
public class KakaoLoginController {

        private final KakaoLoginService service;

        @Operation(summary = "헬스 체크", description = "카카오 OAuth API 상태 확인용 엔드포인트")
        @ApiResponse(responseCode = "200", description = "정상 응답", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class, example = "Kakao OAuth API is running")))
        @GetMapping("/")
        public String index() {
                return "Kakao OAuth API is running";
        }

        @Operation(summary = "카카오 로그인 콜백", description = """
                        카카오 OAuth 인증 후 리다이렉트되는 콜백 엔드포인트입니다.

                        **처리 과정:**
                        1. 카카오로부터 인증 코드(code)를 받습니다
                        2. 인증 코드로 액세스 토큰을 발급받습니다
                        3. 액세스 토큰으로 사용자 프로필 정보를 조회합니다
                        4. 신규 사용자는 자동 회원가입, 기존 사용자는 로그인 처리됩니다
                        5. JWT 토큰(액세스/리프레시)을 반환합니다

                        **주의:** 이 엔드포인트는 카카오 로그인 버튼 클릭 후 자동으로 호출됩니다.
                        **카카오는 state 파라미터를 사용하지 않습니다.**
                        """)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = LoginResponse.class), examples = @ExampleObject(name = "로그인 성공 예시", value = """
                                        {
                                          "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                          "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                          "nickname": "카카오사용자"
                                        }
                                        """))),
                        @ApiResponse(responseCode = "400", description = "카카오 인증 실패 (사용자 거부 또는 잘못된 요청)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "사용자 거부", value = """
                                        {
                                          "error": "access_denied",
                                          "message": "사용자가 로그인을 거부했습니다."
                                        }
                                        """))),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류 (토큰 발급 실패 또는 프로필 조회 실패)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class), examples = {
                                        @ExampleObject(name = "토큰 발급 실패", value = """
                                                        {
                                                          "error": "token_error",
                                                          "message": "액세스 토큰 발급에 실패했습니다."
                                                        }
                                                        """),
                                        @ExampleObject(name = "프로필 조회 실패", value = """
                                                        {
                                                          "error": "profile_error",
                                                          "message": "사용자 정보 조회에 실패했습니다."
                                                        }
                                                        """)
                        }))
        })
        @GetMapping("/callback")
        public ResponseEntity<?> kakaoLoginCallback( // ✅ 메서드명 수정
                        @Parameter(description = """
                                        카카오 OAuth 콜백 파라미터
                                        - code: 인증 코드 (필수)
                                        - error: 에러 코드 (인증 실패 시)
                                        - error_description: 에러 설명 (인증 실패 시)

                                        ※ 카카오는 state 파라미터를 사용하지 않습니다.
                                        """, required = true, schema = @Schema(type = "object", example = "{\"code\": \"xYzAbC123456789\"}")) @RequestParam Map<String, String> resValue) {

                log.info("카카오 OAuth 콜백 수신 - code 존재 여부: {}", resValue.containsKey("code"));

                // 1. 에러 체크 (사용자 거부 또는 인증 실패)
                if (resValue.containsKey("error")) {
                        String error = resValue.get("error");
                        String errorDesc = resValue.get("error_description");
                        log.error("카카오 로그인 실패 - error: {}, description: {}", error, errorDesc);

                        return ResponseEntity
                                        .status(HttpStatus.BAD_REQUEST)
                                        .body(new ErrorResponse(
                                                        error,
                                                        errorDesc != null ? errorDesc : "카카오 로그인에 실패했습니다."));
                }

                // 2. Access Token 발급
                KakaoLoginVo kakaoLoginVo; // ✅ 카카오 VO 사용
                try {
                        kakaoLoginVo = service.requestKakaoLoginAccessToken(
                                        resValue,
                                        "authorization_code");
                } catch (Exception e) {
                        log.error("카카오 액세스 토큰 발급 중 예외 발생", e);
                        return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new ErrorResponse(
                                                        "token_error",
                                                        "액세스 토큰 발급에 실패했습니다."));
                }

                // Null 체크
                if (kakaoLoginVo == null || kakaoLoginVo.getAccessToken() == null) {
                        log.error("카카오 액세스 토큰이 null입니다.");
                        return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new ErrorResponse(
                                                        "token_error",
                                                        "액세스 토큰 발급에 실패했습니다."));
                }

                log.info("카카오 액세스 토큰 발급 완료");

                // 3. 사용자 프로필 조회
                KakaoLoginProfile kakaoLoginProfile; // ✅ 카카오 Profile 사용
                try {
                        kakaoLoginProfile = service.requestKakaoLoginProfile(kakaoLoginVo);
                } catch (Exception e) {
                        log.error("카카오 프로필 조회 중 예외 발생", e);
                        return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new ErrorResponse(
                                                        "profile_error",
                                                        "사용자 정보 조회에 실패했습니다."));
                }

                // Null 체크
                if (kakaoLoginProfile == null || kakaoLoginProfile.getEmail() == null) {
                        log.error("카카오 프로필 정보가 null입니다.");
                        return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new ErrorResponse(
                                                        "profile_error",
                                                        "사용자 정보 조회에 실패했습니다. 이메일 제공 동의가 필요합니다."));
                }

                // 4. 회원가입 또는 로그인 처리
                LoginResponse response;
                try {
                        response = service.signupOrLogin(kakaoLoginProfile);
                } catch (Exception e) {
                        log.error("카카오 소셜 로그인 처리 중 예외 발생", e);
                        return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new ErrorResponse(
                                                        "login_error",
                                                        e.getMessage()));
                }

                log.info("카카오 소셜 로그인 완료 - email: {}, nickname: {}",
                                kakaoLoginProfile.getEmail(), response.nickname());

                return ResponseEntity.ok(response);
        }
}
