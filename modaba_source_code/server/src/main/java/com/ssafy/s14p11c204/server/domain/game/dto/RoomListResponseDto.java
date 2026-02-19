package com.ssafy.s14p11c204.server.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class RoomListResponseDto {
    private List<RoomResponseDto> myRooms;
    private List<RoomResponseDto> availableRooms;
}