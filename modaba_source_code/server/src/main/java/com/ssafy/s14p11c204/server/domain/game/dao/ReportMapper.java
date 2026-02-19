package com.ssafy.s14p11c204.server.domain.game.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportMapper {
    // 리포트 저장
    void insertReport(@Param("sessionId") Long sessionId, 
                      @Param("userId") Long userId, 
                      @Param("content") String content, 
                      @Param("summary") String summary);
                      
    // 집중도 전체 기록 조회 (AI 분석용)
    String findFocusScores(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}
