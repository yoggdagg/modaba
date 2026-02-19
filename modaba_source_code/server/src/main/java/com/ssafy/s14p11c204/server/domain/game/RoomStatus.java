package com.ssafy.s14p11c204.server.domain.game;

public enum RoomStatus {
    WAITING,   // 대기 중
    SCHEDULED, // 시작 예정 (달리기)
    PLAYING,   // 게임 중
    FINISHED   // 게임 종료
}
