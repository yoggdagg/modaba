package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.game.ParticipantStatus;
import com.ssafy.s14p11c204.server.domain.game.RoomStatus;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomListResponseDto;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomRequestDto;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomMapper roomMapper;
    private final GameService gameService;

    @Override
    @Transactional
    public void createRoom(RoomRequestDto dto, String hostNickname) {
        Long hostId = roomMapper.findUserIdByNickname(hostNickname)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다: " + hostNickname));

        // 테스트 편의를 위해 시간 자동 설정
        if (dto.getAppointmentTime() == null) {
            dto.setAppointmentTime(LocalDateTime.now().plusHours(1).plusMinutes(1));
        }

        // 타입에 따른 초기 상태 결정
        RoomStatus status = "APPOINTMENT".equals(dto.getRoomType()) ? RoomStatus.SCHEDULED : RoomStatus.WAITING;
        roomMapper.insertRoom(dto, hostId, status);

        // 방장을 참가자로 등록 (상태: 준비)
        if (dto.getRoomId() != null) {
            roomMapper.insertParticipant(dto.getRoomId(), hostId, ParticipantStatus.READY);
        } else {
            throw new RuntimeException("방 생성 실패: ID를 가져오지 못했습니다.");
        }
    }

    @Override
    @Transactional
    public void leaveRoom(Long roomId, String nickname) {
        Long userId = roomMapper.findUserIdByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다: " + nickname));

        // 1. 방 존재 여부 및 방장 확인
        Long hostId = roomMapper.findHostIdByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        
        boolean isHost = userId.equals(hostId);

        // 2. 참가자 목록에서 삭제
        roomMapper.deleteParticipant(roomId, userId);

        // 3. 남은 인원 확인
        int remainingCount = roomMapper.countParticipants(roomId);

        if (remainingCount == 0) {
            // 4. 인원이 0명이면 방 삭제
            roomMapper.deleteRoom(roomId);
        } else if (isHost) {
            // 5. 방장이 나갔고 인원이 남아있으면 방장 양도
            roomMapper.findOldestParticipant(roomId)
                    .ifPresent(newHostId -> roomMapper.updateRoomHost(roomId, newHostId));
        }
    }

    @Override
    @Transactional
    public void joinRoom(Long roomId, String nickname) {
        Long userId = roomMapper.findUserIdByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다: " + nickname));

        // 1. 방 조회
        RoomRequestDto room = roomMapper.findRoomById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        // 2. 이미 참가 중인지 확인
        if (roomMapper.existsParticipant(roomId, userId)) {
            throw new IllegalStateException("이미 참가 중인 방입니다.");
        }

        // 3. 인원 확인
        int currentCount = roomMapper.countParticipants(roomId);
        if (currentCount >= room.getMaxUser()) {
            throw new IllegalStateException("방 인원이 꽉 찼습니다.");
        }

        // 4. 참가 (상태: 준비)
        roomMapper.insertParticipant(roomId, userId, ParticipantStatus.READY);

        // 5. 인원이 꽉 찼으면 게임 시작
        if (currentCount + 1 == room.getMaxUser()) {
            gameService.startGame(roomId);
        }
    }

    @Override
    @Transactional
    public void createTestUser(String email, String nickname) {
        roomMapper.insertTestUser(email, nickname);
    }

    @Override
    public Long findUserIdByEmail(String email) {
        return roomMapper.findUserIdByEmail(email).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomListResponseDto findAllRooms(String nickname) {
        Long userId = roomMapper.findUserIdByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다: " + nickname));
        
        List<RoomResponseDto> myRooms = roomMapper.findMyRooms(userId);
        List<RoomResponseDto> availableRooms = roomMapper.findAvailableRooms(userId);
        return new RoomListResponseDto(myRooms, availableRooms);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDto> findMyRooms(String nickname) {
        Long userId = roomMapper.findUserIdByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다: " + nickname));
        return roomMapper.findMyRooms(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDto> findAvailableRooms(String nickname) {
        Long userId = roomMapper.findUserIdByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다: " + nickname));
        return roomMapper.findAvailableRooms(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDto> findRoomsByRegion(String city, String district, String neighborhood) {
        return roomMapper.findRoomsByRegion(city, district, neighborhood);
    }

    @Override
    @Transactional
    public void updateLocationSharing(Long roomId, String nickname, boolean enabled) {
        Long userId = roomMapper.findUserIdByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다: " + nickname));

        roomMapper.updateLocationSharingEnabled(roomId, userId, enabled);
    }
}
