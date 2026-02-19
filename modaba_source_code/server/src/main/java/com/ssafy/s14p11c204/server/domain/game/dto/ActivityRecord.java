package com.ssafy.s14p11c204.server.domain.game.dto;

import java.time.LocalDateTime;

public record ActivityRecord(
    Long id,
    Long sessionId,
    Long userId,
    String trajectory,
    Double totalDistance,
    Double avgSpeed,
    Double maxSpeed,
    Integer activeTime,
    Integer score,
    String aiReport,
    LocalDateTime createdAt
) {}
