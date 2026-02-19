package com.ssafy.s14p11c204.server.domain.social.mapper;

import com.ssafy.s14p11c204.server.domain.social.Friendship;
import com.ssafy.s14p11c204.server.domain.user.dto.ProfileSimpleResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface FriendMapper {
    /**
     * 관계 생성 또는 상태 업데이트 (PENDING, ACCEPTED, REJECTED)
     * 파라미터: 요청자(requester), 수신자(receiver), 상태
     */
    void upsert(@Param("requesterId") long requesterId,
                @Param("receiverId") long receiverId,
                @Param("status") Friendship.Status status);

    /**
     * 특정 관계 조회 (단건)
     * XML의 #{requesterId}, #{receiverId}와 매핑됩니다.
     */
    Optional<Friendship> showRelation(@Param("requesterId") long requesterId,
                                      @Param("receiverId") long receiverId);

    /**
     * 나(receiver)에게 온 '대기 중(PENDING)'인 친구 신청 목록 조회
     */
    List<ProfileSimpleResponse> findPending(@Param("myId") long myId);

    /**
     * 나(requester)와 맺어진 '친구(ACCEPTED)' 목록 조회
     * (친구 관계는 양방향이므로 로직에 따라 조회 쿼리가 달라질 수 있음 주의)
     */
    List<ProfileSimpleResponse> findFriends(@Param("myId") long myId);

    /**
     * 내가(requester) '차단(REJECTED)'한 유저 목록 조회
     */
    List<ProfileSimpleResponse> findBlocked(@Param("myId") long myId);

    /**
     * 특정 방향의 관계 삭제 (친구 끊기, 요청 취소 등)
     */
    void delete(@Param("requesterId") long requesterId,
                @Param("receiverId") long receiverId);

}