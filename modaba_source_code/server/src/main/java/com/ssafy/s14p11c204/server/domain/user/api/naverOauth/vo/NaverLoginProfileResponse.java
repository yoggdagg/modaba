package com.ssafy.s14p11c204.server.domain.user.api.naverOauth.vo;
import lombok.Data;

@Data
public class NaverLoginProfileResponse {

    // API 호출 결과 코드
    private String resultcode;

    // 호출 결과 메시지
    private String message;

    // Profile 상세
    private NaverLoginProfile response;

}
