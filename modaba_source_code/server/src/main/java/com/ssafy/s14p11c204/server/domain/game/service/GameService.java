package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.game.dto.GameBoundaryDto;
import com.ssafy.s14p11c204.server.domain.game.dto.MyPosition;
import com.ssafy.s14p11c204.server.domain.game.dto.TagRequest;

public interface GameService {

    void startGame(Long roomId);
    void saveBoundary(Long roomId, GameBoundaryDto boundaryDto);

    // 기존 기능 유지
    void gameStart(Long roomId, Long userId);
    
    // 구역 정보를 함께 받는 새로운 게임 시작 메서드
    void gameStart(Long roomId, Long userId, GameBoundaryDto boundaryDto);
    
    void ready(Long roomId, Long userId, boolean isReady);
    void tagProcess(Long roomId, Long userId, TagRequest tagRequest);
    void refreshPosition(Long roomId, Long userId, MyPosition myPosition);

    com.ssafy.s14p11c204.server.domain.game.dto.GameRoomDetailDto getRoomDetail(Long roomId);

    // 검거 처리
    void handleArrest(Long roomId, String thiefNickname);
    
    // 탈옥 처리
    void handleUnleash(Long roomId, String rescuerNickname);
    
    // 살아있는 도둑 수 조회 (추가됨)
    int getAliveThiefCount(Long roomId);

    // 게임 종료 처리
    void endGame(Long roomId, String winnerTeam);

    // 현재 세션 ID 조회 (추가됨)
    Long getCurrentSessionId(Long roomId);
}
