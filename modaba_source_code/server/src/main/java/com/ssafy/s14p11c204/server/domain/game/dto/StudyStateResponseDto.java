package com.ssafy.s14p11c204.server.domain.game.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudyStateResponseDto {
    private boolean isInStudy;
    private Long roomId;
    private Long sessionId;
    private String title;
    private LocalDateTime startTime;
}
