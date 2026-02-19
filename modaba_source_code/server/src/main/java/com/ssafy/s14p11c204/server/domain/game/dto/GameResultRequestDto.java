package com.ssafy.s14p11c204.server.domain.game.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GameResultRequestDto {
    private String winnerTeam; // "POLICE" or "THIEF"
}