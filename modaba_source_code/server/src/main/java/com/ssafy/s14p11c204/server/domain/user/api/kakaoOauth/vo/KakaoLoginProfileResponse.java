package com.ssafy.s14p11c204.server.domain.user.api.kakaoOauth.vo;

import lombok.Data;

@Data
public class KakaoLoginProfileResponse {

    // API 호출 결과 코드
    private String resultcode;

    // 호출 결과 메시지
    private String message;

    // Profile 상세
    private KakaoLoginProfile response;

}
