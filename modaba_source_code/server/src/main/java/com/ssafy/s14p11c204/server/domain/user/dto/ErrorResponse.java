package com.ssafy.s14p11c204.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(description = "에러 코드", example = "access_denied") String error,

        @Schema(description = "에러 메시지", example = "사용자가 로그인을 거부했습니다.") String message) {
}
