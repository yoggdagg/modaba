package com.ssafy.s14p11c204.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TokenReissueRequest(
        @Schema(description = "기존의 refreshToken")
        @NotBlank String refreshToken
) {
}
