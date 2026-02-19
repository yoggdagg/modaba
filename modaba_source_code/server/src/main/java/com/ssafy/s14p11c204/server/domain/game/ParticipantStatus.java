package com.ssafy.s14p11c204.server.domain.game;

public enum ParticipantStatus {
    READY,      // 준비
    MOVING,     // 이동 중
    ARRIVED,    // 도착 (탈출 성공?)
    IN_GAME,    // 게임 중
    ARRESTED    // 검거됨 (추가됨)
}