package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.game.dto.ActivityRecord;
import java.util.List;
import java.util.Optional;

/**
 * 유저의 활동량(이동 거리, 속도, 궤적 등)을 기록하고 분석하는 서비스
 */
public interface ActivityService {
    
    /**
     * 유저의 현재 위치를 실시간으로 기록 (Redis에 누적)
     */
    void recordMovement(Long sessionId, Long userId, double lat, double lng);

    /**
     * 게임 종료 시 누적된 활동 데이터를 분석하여 DB에 최종 저장
     */
    void finalizeActivity(Long sessionId, Long userId);

    /**
     * 유저의 과거 활동 기록 리스트 조회
     */
    List<ActivityRecord> getUserActivities(Long userId);

    /**
     * 특정 게임 세션의 상세 활동 기록 조회
     * @throws IllegalArgumentException 기록을 찾을 수 없는 경우
     */
    ActivityRecord getActivityDetail(Long sessionId, Long userId);
}
