package com.ssafy.s14p11c204.server.domain.game.dto;

import com.ssafy.s14p11c204.server.domain.game.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameRoomDetailDto {
    private Long roomId;
    private String title;
    private RoomStatus status;
    private Long hostId;
    private List<GameParticipantDto> participants;
    
    // 게임 구역 정보 (도넛 모양 지원)
    private List<List<List<Double>>> coordinates;
    
    // 감옥 구역 정보 (단일 다각형)
    private List<List<Double>> jailCoordinates;

    // 게임 종료 예정 시간 (추가됨)
    private LocalDateTime endTime;
}
