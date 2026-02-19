package com.ssafy.s14p11c204.server.domain.game.dto;

import com.ssafy.s14p11c204.server.domain.game.ParticipantStatus;
import com.ssafy.s14p11c204.server.domain.game.dto.PlayerRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameParticipantDto {
    private Long userId;
    private String nickname;
    private String profileImage;
    private PlayerRole role; // POLICE, THIEF
    private ParticipantStatus status; // READY, MOVING, ARRIVED
}
