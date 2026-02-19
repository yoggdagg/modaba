package com.ssafy.s14p11c204.server.domain.game.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.s14p11c204.server.domain.game.dto.ActivityRecord;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ActivityMapper {
    
    /**
     * 유저의 종합 활동 기록 저장
     */
    void insertActivity(ActivityRecord activity);

    /**
     * 특정 유저의 모든 활동 기록 조회
     */
    List<ActivityRecord> findByUserId(@Param("userId") Long userId);

    /**
     * 특정 게임 세션의 내 활동 상세 조회 (Optional 적용)
     */
    Optional<ActivityRecord> findBySessionAndUser(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}
