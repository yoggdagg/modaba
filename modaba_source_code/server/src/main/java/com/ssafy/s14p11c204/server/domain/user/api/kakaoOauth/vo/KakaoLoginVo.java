package com.ssafy.s14p11c204.server.domain.user.api.kakaoOauth.vo;

import lombok.Getter;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@ToString
public class KakaoLoginVo {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("refresh_token_expires_in")
    private Integer refreshTokenExpiresIn;
}
