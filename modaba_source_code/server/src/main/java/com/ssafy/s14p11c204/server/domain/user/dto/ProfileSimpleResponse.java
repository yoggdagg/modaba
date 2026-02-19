package com.ssafy.s14p11c204.server.domain.user.dto;

import lombok.Builder;

/**
 *
 * @param nickname 유저의 닉네임
 * @param imageLink 서버에 저장된 유저의 프로필 사진 다운로드 링크입니다.
 */
@Builder
public record ProfileSimpleResponse(
        Long id,
        String nickname,
        String imageLink
) {
}
