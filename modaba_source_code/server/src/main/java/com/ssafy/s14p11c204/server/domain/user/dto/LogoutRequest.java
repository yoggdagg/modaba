package com.ssafy.s14p11c204.server.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogoutRequest {
    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;
}
