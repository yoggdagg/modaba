package com.ssafy.s14p11c204.server.domain.user.dto;

import com.ssafy.s14p11c204.server.global.format.PasswordFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Builder;

@Builder
public record LoginRequest(
        @Schema(description = "사용자 이메일", example = "example@example.com")
        @Email String email,
        @Schema(description = "사용자 비밀번호", example = "p455!")
        @PasswordFormat String password
) {
}
