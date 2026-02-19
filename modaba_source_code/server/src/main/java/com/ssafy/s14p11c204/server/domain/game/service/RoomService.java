package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.game.dto.RoomListResponseDto;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomRequestDto;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomResponseDto;

import java.util.List;

public interface RoomService {
    void createRoom(RoomRequestDto dto, String hostNickname);
    void leaveRoom(Long roomId, String nickname);
    void joinRoom(Long roomId, String nickname);
    void createTestUser(String email, String nickname);
    Long findUserIdByEmail(String email);
    
    RoomListResponseDto findAllRooms(String nickname);
    List<RoomResponseDto> findMyRooms(String nickname);
    List<RoomResponseDto> findAvailableRooms(String nickname);
    List<RoomResponseDto> findRoomsByRegion(String city, String district, String neighborhood);
    void updateLocationSharing(Long roomId, String nickname, boolean enabled);
}