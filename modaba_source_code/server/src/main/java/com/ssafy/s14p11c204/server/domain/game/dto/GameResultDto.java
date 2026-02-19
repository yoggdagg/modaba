package com.ssafy.s14p11c204.server.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResultDto {
    private Long roomId;
    private String winnerTeam; // "POLICE" or "THIEF"
    private List<PlayerResultDto> playerResults;

    @Getter @Setter // Setter 추가
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerResultDto {
        private Long userId;
        private String nickname;
        private PlayerRole role;
        private Integer oldMmr;
        private Integer newMmr;
        private Integer changeValue;
        private boolean isEscaped;
    }
}