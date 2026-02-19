package com.ssafy.s14p11c204.server.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "카카오 OAuth 토큰 응답")
public class KakaoTokenResponse {

    @JsonProperty("access_token")
    @Schema(description = "카카오 API 호출용 액세스 토큰", example = "pGXWHh3FiUd8Lkdvq5vt6lHS5NmF09D9AAAAAQoNCB4AAAGcA1CKx", requiredMode = Schema.RequiredMode.REQUIRED)
    private String access_token;

    @JsonProperty("token_type")
    @Schema(description = "토큰 타입 (Bearer)", example = "bearer", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token_type;

    @JsonProperty("refresh_token")
    @Schema(description = "액세스 토큰 갱신용 리프레시 토큰", example = "vcsCAdUYvpCs4xCcvVgV0VMl5z8GE8J2AAAAAgoNCB4AAAGcA1CKx")
    private String refresh_token;

    @JsonProperty("expires_in")
    @Schema(description = "액세스 토큰 유효 시간 (초 단위, 약 6시간)", example = "21599", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer expires_in;

    @JsonProperty("scope")
    @Schema(description = "획득한 권한 범위 (OpenID Connect 등)", example = "openid")
    private String scope;

    @JsonProperty("refresh_token_expires_in")
    @Schema(description = "리프레시 토큰 유효 시간 (초 단위, 약 60일)", example = "5183999")
    private Integer refresh_token_expires_in;

    @JsonProperty("id_token")
    @Schema(description = "사용자 정보가 담긴 JWT 토큰 (OpenID Connect ID Token)", example = "eyJraWQiOiIzZjk2OTgwMzgxZTQ1MWVmYWQwZDJkZGQzMGUzZDMiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9...")
    private String id_token;
}
