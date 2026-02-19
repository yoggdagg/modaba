package com.ssafy.s14p11c204.server.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.AssertTrue;

/**
 * 수정 불가: id, email, nickname, role, provider, createdAt
 * 수정 가능: password, imageLink, isActive, mmr, deviceToken
 */
public record ProfileUpdateRequest(
        @Nullable String password,
        @Nullable String imageLink,
        @Nullable String deviceToken) {
    @JsonIgnore
    @AssertTrue(message = "수정할 정보를 최소 하나 이상 입력해주세요.")
    public boolean isAnyFieldValid() {
        return password != null
                || imageLink != null
                || deviceToken != null;
    }
}