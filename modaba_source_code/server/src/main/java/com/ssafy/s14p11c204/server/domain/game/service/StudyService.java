package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.game.dto.StudyStateResponseDto;

public interface StudyService {
    /**
     * 사용자의 현재 위치를 확인하여 스터디 목적지 10m 이내인지 검증하고 상태를 ARRIVED로 변경합니다.
     */
    boolean verifyArrival(Long roomId, String nickname, Double lat, Double lng);

    /**
     * 사용자 간 태그를 기록합니다. (공부 시작의 트리거)
     * @return 생성된 또는 현재 진행 중인 세션 ID
     */
    Long tagUser(Long roomId, String actorNickname, String targetNickname);

    /**
     * 집중도 점수를 기록하고 팀원들에게 실시간으로 전파합니다.
     */
    void addFocusLog(Long sessionId, String nickname, Double score);

    /**
     * 세션 생존 신고(Heartbeat)를 처리합니다.
     */
    void heartbeat(Long sessionId, String nickname);

    /**
     * 현재 사용자가 진행 중인 스터디 정보를 조회합니다 (복구용).
     */
    StudyStateResponseDto getCurrentState(String nickname);

    /**
     * 스터디를 종료하고 AI 리포트를 생성합니다.
     * @return 생성된 리포트 ID
     */
    Long endStudy(Long roomId, String nickname);
}
