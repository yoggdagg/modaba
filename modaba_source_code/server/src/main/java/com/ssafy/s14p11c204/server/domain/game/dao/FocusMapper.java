package com.ssafy.s14p11c204.server.domain.game.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FocusMapper {
    /**
     * 특정 세션에 대한 유저의 집중도 기록을 추가하거나 업데이트합니다.
     */
    void upsertFocusLog(@Param("sessionId") Long sessionId, 
                        @Param("userId") Long userId, 
                        @Param("scoreEntryJson") String scoreEntryJson);

    /**
     * 세션 종료 시 평균 점수를 계산하여 업데이트합니다.
     */
    void updateAverageScore(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}
