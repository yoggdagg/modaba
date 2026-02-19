package com.ssafy.s14p11c204.server.domain.social.service;

import com.ssafy.s14p11c204.server.domain.social.Friendship;
import com.ssafy.s14p11c204.server.domain.social.mapper.FriendMapper;
import com.ssafy.s14p11c204.server.domain.user.dto.ProfileSimpleResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
class FriendServiceImpl implements FriendService {
    private final FriendMapper friendMapper;

    /**
     * 친구 신청 로직
     * 1. 상대가 나한테 이미 보낸 게 있는지 확인
     * 2. 있다면 -> 친구 수락 (ACCEPTED)
     * 3. 없다면 -> 내가 신청 (PENDING)
     */
    @Override
    public void sendRequest(long requesterId, long receiverId) {
        // 상대방(receiver)이 나(requester)에게 보낸 요청이 있는지 확인
        friendMapper.showRelation(receiverId, requesterId).ifPresentOrElse(
                relation -> {
                    switch (relation.status()) {
                        // 상대가 보낸 요청이 대기 중이면 -> 쌍방 수락 처리
                        case PENDING -> this.updateRelation(requesterId, receiverId, Friendship.Status.ACCEPTED);
                        // 이미 친구 상태라면 -> 내 쪽도 확실하게 ACCEPTED로 갱신 (멱등성)
                        case ACCEPTED -> friendMapper.upsert(requesterId, receiverId, Friendship.Status.ACCEPTED);
                        // 상대가 나를 차단(REJECTED)했다면 -> 아무것도 하지 않음 (조용히 실패)
                    }
                },
                // 아무 관계도 없으면 -> 내가 친구 신청 (PENDING) 생성
                () -> friendMapper.upsert(requesterId, receiverId, Friendship.Status.PENDING)
        );
    }

    /**
     * 관계 조회 (단건)
     */
    @Transactional(readOnly = true)
    public Friendship showRelation(long requesterId, long receiverId) {
        return friendMapper.showRelation(requesterId, receiverId)
                .orElseThrow(() -> new IllegalArgumentException("해당 친구 관계가 없습니다."));
    }

    /**
     * 관계 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProfileSimpleResponse> showRelations(long myId, Friendship.@NotNull Status status) {
        return Collections.unmodifiableList(switch (status) {
            case PENDING -> friendMapper.findPending(myId);  // 나에게 온 요청
            case ACCEPTED -> friendMapper.findFriends(myId); // 친구 목록
            case REJECTED -> friendMapper.findBlocked(myId); // 차단 목록
        });
    }

    /**
     * 관계 상태 업데이트 (수락, 삭제, 차단)
     */
    @Override
    public void updateRelation(long requesterId, long targetUserId, Friendship.@NotNull Status status) {
        switch (status) {
            case ACCEPTED -> {
                // 양쪽 다 ACCEPTED 상태로 Upsert (친구 성립)
                friendMapper.upsert(requesterId, targetUserId, Friendship.Status.ACCEPTED);
                friendMapper.upsert(targetUserId, requesterId, Friendship.Status.ACCEPTED);
            }
            case PENDING ->
                // [주석 유지] 친구 요청 방치(거절) & 친구 삭제
                // PENDING 상태로 돌리는 게 아니라, 관계를 아예 지워버림
                    friendMapper.delete(requesterId, targetUserId);

            case REJECTED -> {
                // 내 쪽에서는 상대를 차단(REJECTED) 상태로 저장
                friendMapper.upsert(requesterId, targetUserId, Friendship.Status.REJECTED);

                // [주석 유지] 그런데 그냥 친구였던 적 없이 차단하는 것도 멀쩡해야 됨
                // 상대방이 나를 향해 가진 관계(친구였거나, 신청했거나)를 삭제
                friendMapper.delete(targetUserId, requesterId);
            }
        }
    }
}