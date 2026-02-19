package com.ssafy.s14p11c204.server.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 카카오 로그인 성공 후 서비스 자체 토큰과 정보를 반환하는 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class KakaoLoginResponse {

    private String accessToken;
    private String refreshToken;
    private long userId;
    private String email;
    private String nickname;

}