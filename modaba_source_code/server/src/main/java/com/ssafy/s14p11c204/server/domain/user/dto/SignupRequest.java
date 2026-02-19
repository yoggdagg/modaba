package com.ssafy.s14p11c204.server.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.s14p11c204.server.global.format.NicknameFormat;
import com.ssafy.s14p11c204.server.global.format.PasswordFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

public record SignupRequest(
                @Email @Schema(description = "사용자 이메일", example = "example@example.com") @JsonProperty("email") String email,
                @NicknameFormat @Schema(description = "사용자 닉네임 (2~10자, 특수문자 불가)", example = "포돌이123") @JsonProperty("nickname") String nickname,
                @PasswordFormat @Schema(description = "사용자 비밀번호", example = "p455w0rd!") @JsonProperty("password") String password) {
}
