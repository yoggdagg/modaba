package com.ssafy.s14p11c204.server.domain.user;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password")
public class User {

    private Long id;
    private String email;
    private String nickname;
    private String password;
    private Role role;
    private String imageLink;
    private Provider provider;
    private boolean isActive;
    private long mmr;
    private LocalDateTime createdAt;
    private String deviceToken;

    public enum Role {
        USER,
        ADMIN
    }

    public enum Provider {
        LOCAL,
        KAKAO,
        GOOGLE,
        NAVER
    }
}