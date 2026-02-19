package com.ssafy.s14p11c204.server.domain.game.dto;

import com.ssafy.s14p11c204.server.domain.game.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomResponseDto {
    private Long roomId;
    private String title;
    private String roomType;
    private Integer maxUser;
    private Integer currentUserCount;
    private RoomStatus status;
    private LocalDateTime appointmentTime;
    private String placeName;
    private Integer regionId;
    private String regionName;
    private Integer hostId;       // 방장 ID 추가
    private String hostNickname;  // 방장 닉네임 추가
}