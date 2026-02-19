package com.ssafy.s14p11c204.server.domain.chat.dto;

public enum MessageType {
    // 1. 기본 시스템 및 채팅
    ENTER,      // 입장
    QUIT,       // 퇴장
    TALK,       // 일반 대화
    EVENT,      // 기타 이벤트 (시스템 알림 등)
    LOCATION_REQUEST, // 위치 공유 요청 (스터디 시작 1시간 전)

    // 2. 게임 로직 (경도/Smart Tag Game)
    GAME_START, // 게임 시작
    CAPTURE,    // 체포 (술래 -> 도망자)
    UNLEASH,    // 탈옥 (도망자 구출)
    GAME_END,   // 게임 종료

    // 3. WebRTC 시그널링 (보이스/영상 채팅용)
    OFFER,      // WebRTC Offer
    ANSWER,     // WebRTC Answer
    CANDIDATE   // WebRTC ICE Candidate
}