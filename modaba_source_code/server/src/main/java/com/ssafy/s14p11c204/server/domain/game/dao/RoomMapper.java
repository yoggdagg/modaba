package com.ssafy.s14p11c204.server.domain.game.dao;

import com.ssafy.s14p11c204.server.domain.game.ParticipantStatus;
import com.ssafy.s14p11c204.server.domain.game.RoomStatus;
import com.ssafy.s14p11c204.server.domain.game.dto.GameStartResponseDto;
import com.ssafy.s14p11c204.server.domain.game.dto.PlayerRole;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomRequestDto;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface RoomMapper {
    // Rooms 테이블에 데이터 삽입
    void insertRoom(@Param("dto") RoomRequestDto dto, @Param("hostId") Long hostId, @Param("status") RoomStatus status);

    // 참가자 추가
    void insertParticipant(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("status") ParticipantStatus status);

    // 유저가 참여 중인 방 ID 조회
    Optional<Long> findRoomIdByUserId(@Param("userId") Long userId);

    // 유저가 현재 실제 진행(공부) 중인 방 ID 조회
    Optional<Long> findActiveRoomIdByUserId(@Param("userId") Long userId);

    // 방장 ID 조회
    Optional<Long> findHostIdByRoomId(@Param("roomId") Long roomId);

    // 참가자 삭제
    void deleteParticipant(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // 남은 참가자 수 조회
    int countParticipants(@Param("roomId") Long roomId);

    // 다음 방장 후보 조회 (가장 먼저 들어온 사람)
    Optional<Long> findOldestParticipant(@Param("roomId") Long roomId);

    // 방장 변경
    void updateRoomHost(@Param("roomId") Long roomId, @Param("newHostId") Long newHostId);

    // 방 삭제
    void deleteRoom(@Param("roomId") Long roomId);

    // 게임 세션 삭제
    void deleteGameSessionsByRoomId(@Param("roomId") Long roomId);

    // 방 정보 조회
    Optional<RoomRequestDto> findRoomById(@Param("roomId") Long roomId);

    // 이미 참가 중인지 확인
    boolean existsParticipant(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // 테스트 유저 생성
    void insertTestUser(@Param("email") String email, @Param("nickname") String nickname);

    // 테스트 지역 생성
    void insertTestRegion(@Param("regionId") Integer regionId, @Param("city") String city, @Param("district") String district, @Param("neighborhood") String neighborhood);

    // 이메일로 유저 ID 조회
    Optional<Long> findUserIdByEmail(@Param("email") String email);

    // 닉네임으로 유저 ID 조회
    Optional<Long> findUserIdByNickname(@Param("nickname") String nickname);

    // 유저 MMR 조회
    Optional<Integer> findUserMmr(@Param("userId") Long userId);

    // 내가 참여 중인 방 목록 조회
    List<RoomResponseDto> findMyRooms(@Param("userId") Long userId);

    // 참여하지 않은 방 목록 조회
    List<RoomResponseDto> findAvailableRooms(@Param("userId")Long userId);

    // 지역별 방 검색 (동적 쿼리)
    List<RoomResponseDto> findRoomsByRegion(@Param("city") String city, @Param("district") String district, @Param("neighborhood") String neighborhood);

    // 곧 시작할 방 조회 (1시간 전)
    List<RoomResponseDto> findRoomsStartingSoon(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // 알림 발송 상태 업데이트
    void updateNotiSent(@Param("roomId") Long roomId, @Param("isNotiSent") boolean isNotiSent);

    // 위치 공유 상태 업데이트
    void updateLocationSharingEnabled(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("enabled") boolean enabled);

    // 방 상태 변경
    void updateRoomStatus(@Param("roomId") Long roomId, @Param("status") String status);

    // MMR 기반 방 검색
    List<RoomResponseDto> findRoomsByMmr(@Param("minMmr") int minMmr, @Param("maxMmr") int maxMmr);

    // 방 참가자 목록 조회 (ID만)
    List<Long> findParticipantIds(@Param("roomId") Long roomId);

    // 참가자 역할 업데이트
    void updateParticipantRole(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("role") PlayerRole role);

    // 게임 구역(Boundary) 및 감옥 구역(Jail Boundary) 업데이트
    void updateRoomBoundary(@Param("roomId") Long roomId, @Param("boundaryWkt") String boundaryWkt, @Param("jailWkt") String jailWkt);

    // 방의 모든 참가자 상태 업데이트
    void updateAllParticipantsStatus(@Param("roomId") Long roomId, @Param("status") ParticipantStatus status);

    // 특정 참가자 상태 업데이트
    void updateParticipantStatus(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("status") ParticipantStatus status);

    // 특정 참가자 상태 조회
    ParticipantStatus findParticipantStatus(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // 내가 참여 중인 다른 방 ID 목록 조회
    List<Long> findOtherParticipatingRooms(@Param("userId") Long userId, @Param("currentRoomId") Long currentRoomId);

    // 특정 역할(도둑)의 모든 참가자 상태 업데이트
    void updateAllThievesStatus(@Param("roomId") Long roomId, @Param("status") ParticipantStatus status);

    // 참가자 정보(ID, 닉네임, 역할) 조회
    List<GameStartResponseDto.ParticipantInfo> findParticipantsWithNickname(@Param("roomId") Long roomId);

    // 시간 초과된 방 조회
    List<Long> findTimeOverRooms(@Param("timeLimit") LocalDateTime timeLimit);

    // 유저 역할 조회
    PlayerRole findUserRole(@Param("roomId") Long roomId, @Param("nickname") String nickname);

    // 서버 타겟팅 전송을 위한 이메일 및 역할 조회
    List<ParticipantEmailInfo> findParticipantsWithEmail(@Param("roomId") Long roomId);

    // 유저 ID로 이메일 조회
    String findEmailByUserId(@Param("userId") Long userId);
    
    // 감옥에 있는 모든 도둑 상태 변경 (탈옥)
    void updateAllArrestedThievesToAlive(@Param("roomId") Long roomId);
    
    // 방의 구역 정보 조회 (WKT)
    Optional<RoomBoundaryInfo> findRoomBoundary(@Param("roomId") Long roomId);

    record ParticipantEmailInfo(String email, PlayerRole role) {}
    
    record RoomBoundaryInfo(String boundaryWkt, String jailWkt) {}
}
