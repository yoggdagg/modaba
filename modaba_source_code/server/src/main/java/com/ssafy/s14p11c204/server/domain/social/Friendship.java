package com.ssafy.s14p11c204.server.domain.social;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 친구 관계 도메인 객체 (Refactored)
 * - 복합키 사용으로 id 필드 제거됨
 * - from/to -> requester/receiver 로 명칭 변경
 */
@Builder
public record Friendship(
        Long requesterId, // [변경] fromUserId -> requesterId
        Long receiverId,  // [변경] toUserId -> receiverId
        Status status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public enum Status {
        PENDING, // 대기중
        ACCEPTED, // 수락
        REJECTED, // 거절 (차단)
    }
}