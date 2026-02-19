package com.ssafy.s14p11c204.server.domain.user.api.kakaoOauth.vo;

import lombok.Getter;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@ToString
public class KakaoLoginProfile {
    private Long id;

    @JsonProperty("connected_at")
    private String connectedAt;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @ToString
    public static class KakaoAccount {
        private Profile profile;

        @JsonProperty("has_email")
        private Boolean hasEmail;

        private String email;

        @Getter
        @ToString
        public static class Profile {
            private String nickname;

            @JsonProperty("profile_image_url")
            private String profileImageUrl;
        }
    }

    // 편의 메서드
    public String getEmail() {
        return kakaoAccount != null ? kakaoAccount.getEmail() : null;
    }

    public String getNickname() {
        return kakaoAccount != null && kakaoAccount.getProfile() != null
                ? kakaoAccount.getProfile().getNickname()
                : null;
    }
}
