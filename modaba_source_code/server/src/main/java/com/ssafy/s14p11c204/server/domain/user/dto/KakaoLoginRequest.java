package com.ssafy.s14p11c204.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*; // Lombok 전체 import

@Getter
@Setter // [수정 1] Setter 추가 (필수!)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카카오 로그인 요청 정보")
public class KakaoLoginRequest {

    // (참고) 현재 JSON으로 "accessToken"만 보내고 계시므로,
    // kakaoToken에 붙은 @NotBlank 때문에 나중에 @Valid 추가 시 에러가 날 수 있습니다.
    // 사용하지 않는 필드라면 삭제하거나 검증을 푸는 게 좋습니다.

    @Schema(description = "카카오 ID Token")
    private String kakaoToken;

    @Schema(description = "카카오 Access Token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessToken;
}