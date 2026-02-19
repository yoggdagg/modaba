package com.ssafy.s14p11c204.server.domain.user.dto;

import com.ssafy.s14p11c204.server.global.format.PasswordFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PwResetRequest(
        @Schema(description = "사용자 이메일", example = "test@ssafy.com") @Email String email,
        @Schema(description = "메일로 받은 인증코드") @NotBlank String code,
        @Schema(description = "사용자 비밀번호", example = "p45dgd5!") @PasswordFormat String password) {
}
