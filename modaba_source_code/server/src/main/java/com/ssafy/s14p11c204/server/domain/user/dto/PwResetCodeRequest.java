package com.ssafy.s14p11c204.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

public record PwResetCodeRequest(
                @Schema(description = "사용자 이메일", example = "test@ssafy.com") @Email String email,
                @Schema(description = "비밀번호 재설정 인증코드", example = "123456") String code) {
}
