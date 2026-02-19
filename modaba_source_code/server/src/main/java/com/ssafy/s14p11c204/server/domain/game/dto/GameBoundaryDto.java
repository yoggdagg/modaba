package com.ssafy.s14p11c204.server.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameBoundaryDto {
    private Long roomId;
    
    /**
     * 게임 전체 구역 좌표 (도넛 모양 지원)
     * 구조: [ [외곽선 좌표들], [구멍1 좌표들], [구멍2 좌표들], ... ]
     * 각 좌표: [lng, lat]
     */
    private List<List<List<Double>>> coordinates;

    /**
     * 감옥 구역 좌표 (단일 다각형)
     * 구조: [ [lng, lat], [lng, lat], ... ]
     */
    private List<List<Double>> jailCoordinates;
}
