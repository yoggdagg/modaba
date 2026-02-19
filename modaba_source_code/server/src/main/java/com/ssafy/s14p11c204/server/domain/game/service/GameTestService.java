package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.game.ParticipantStatus;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.GameResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameTestService {

    private final RoomMapper roomMapper;
    private final GameResultService gameResultService;

    // 경찰 승리 시나리오 (강제 검거)
    @Transactional
    public GameResultDto forceArrestAllThieves(Long roomId) {
        log.info("TEST API: Force arrest all thieves in room {}", roomId);
        
        // 1. 모든 도둑의 상태를 ARRESTED로 변경
        roomMapper.updateAllThievesStatus(roomId, ParticipantStatus.ARRESTED);
        
        // 2. 경찰 승리로 게임 종료 처리
        return gameResultService.processGameResult(roomId, "POLICE");
    }

    // 도둑 승리 시나리오 (시간 초과)
    @Transactional
    public GameResultDto forceTimeout(Long roomId) {
        log.info("TEST API: Force timeout in room {}", roomId);
        
        // 1. (선택) 도둑들의 상태를 ARRIVED(탈출 성공) 등으로 바꿀 수도 있음
        // roomMapper.updateAllThievesStatus(roomId, ParticipantStatus.ARRIVED);

        // 2. 도둑 승리로 게임 종료 처리
        return gameResultService.processGameResult(roomId, "THIEF");
    }
}