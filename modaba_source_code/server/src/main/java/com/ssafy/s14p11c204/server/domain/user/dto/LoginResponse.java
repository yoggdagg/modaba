package com.ssafy.s14p11c204.server.domain.user.dto;

import lombok.Builder;

@Builder
public record LoginResponse(
        String refreshToken,
        String accessToken,
        String nickname
) {
}
