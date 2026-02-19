package com.ssafy.s14p11c204.server.domain.game.dto;

/**
 * 게임 중 유저의 위치 궤적(Trajectory)을 기록하기 위한 시계열 데이터 포인트
 */
public record TrajectoryPoint(
    long t,      // Timestamp (Unix epoch)
    double lat,  // 위도
    double lng,  // 경도
    double spd   // 속도 (km/h)
) {}
