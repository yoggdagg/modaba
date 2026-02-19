package com.ssafy.s14p11c204.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

public record PwForgotRequest(
        @Schema(description = "사용자 이메일", example = "example@example.com")
        @Email String email) {
}
