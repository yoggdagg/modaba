package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.game.dto.GameResultDto;

/**
 * 게임 결과 처리를 위한 서비스 인터페이스
 */
public interface GameResultService {
    /**
     * 게임 결과를 정산하고 알림을 전송합니다.
     * @param roomId 방 번호
     * @param winnerTeam 승리 팀 ("POLICE" 또는 "THIEF")
     * @return 정산된 게임 결과 DTO
     */
    GameResultDto processGameResult(Long roomId, String winnerTeam);
}
