package com.ssafy.s14p11c204.server.domain.social.service;

import com.ssafy.s14p11c204.server.domain.social.Friendship;
import com.ssafy.s14p11c204.server.domain.user.dto.ProfileSimpleResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated // 유효성 검사 활성화
public interface FriendService {

    /**
     * 친구 신청 보내기
     * @param requesterId 요청자 ID (나)
     * @param receiverId 수신자 ID (상대방)
     */
    void sendRequest(long requesterId, long receiverId);

    /**
     * 특정 상태의 관계 목록 조회
     * (예: 내가 신청한 목록, 나랑 친구인 목록 등)
     * @param requesterId 기준 유저 ID (나)
     * @param status 조회할 상태 (ACCEPTED 등)
     */
    List<ProfileSimpleResponse> showRelations(long requesterId, Friendship.@NotNull Status status);

    /**
     * 관계 상태 변경 (수락/거절/차단)
     * @param requesterId 변경을 시도하는 주체 (나)
     * @param targetUserId 대상 유저 (상대방)
     * @param status 변경할 상태
     */
    void updateRelation(long requesterId, long targetUserId, Friendship.@NotNull Status status);
}