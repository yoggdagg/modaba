package com.ssafy.s14p11c204.server.domain.game.dao;

import com.ssafy.s14p11c204.server.domain.game.dto.GameResultDto;
import com.ssafy.s14p11c204.server.domain.user.dto.MmrHistoryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ssafy.s14p11c204.server.domain.game.dto.PlayerRole;
import com.ssafy.s14p11c204.server.domain.game.dto.GameResultDto;
import com.ssafy.s14p11c204.server.domain.user.dto.MmrHistoryDto;

@Mapper
public interface GameMapper {
    // 참가자 역할 조회
    Optional<PlayerRole> findParticipantRole(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // 게임 방 상세 정보 조회
    Optional<com.ssafy.s14p11c204.server.domain.game.dto.GameRoomDetailDto> findGameRoomDetail(@Param("roomId") Long roomId);

    // 게임 방 참가자 상세 목록 조회 (이름 변경: findDetailedParticipants)
    List<com.ssafy.s14p11c204.server.domain.game.dto.GameParticipantDto> findDetailedParticipants(@Param("roomId") Long roomId);

    // 게임 참가자 정보 조회 (결과 정산용 - 기존 유지)
    List<GameResultDto.PlayerResultDto> findGameParticipants(@Param("roomId") Long roomId);

    // 유저 MMR 업데이트 (Integer -> Long으로 변경)
    void updateUserMmr(@Param("userId") Long userId, @Param("newMmr") Integer newMmr);

    // MMR 변동 이력 저장
    void insertMmrHistory(MmrHistoryDto historyDto);

    // 게임 세션 종료 (평균 MMR 저장 등)
    void updateGameSession(@Param("roomId") Long roomId, @Param("avgMmr") Integer avgMmr, @Param("winnerTeam") String winnerTeam);

    // 게임 세션 생성 (추가됨)
    void createGameSession(@Param("roomId") Long roomId);

    // 현재 진행 중인 세션 ID 조회
    Optional<Long> findCurrentSessionId(@Param("roomId") Long roomId);
    
    // 세션 ID로 방 ID 조회 (추가됨)
    Optional<Long> findRoomIdBySessionId(@Param("sessionId") Long sessionId);
    
    // 현재 진행 중인 세션의 시작 시간 조회 (추가됨)
    Optional<LocalDateTime> findCurrentSessionStartTime(@Param("roomId") Long roomId);

    // 액션 로그 기록 (태그, 체포 등)
    void insertActionLog(@Param("sessionId") Long sessionId, @Param("actorId") Long actorId, 
                        @Param("targetId") Long targetId, @Param("type") String type);
}
