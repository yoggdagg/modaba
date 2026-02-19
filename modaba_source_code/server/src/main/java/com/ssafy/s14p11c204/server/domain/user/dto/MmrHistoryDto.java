package com.ssafy.s14p11c204.server.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MmrHistoryDto {
    private Long historyId;
    private Long userId;
    private Long gameId; // gameId 필드 추가
    private Integer changeValue;
    private Integer finalMmr;
    private String reason;
    private LocalDateTime createdAt;
}
