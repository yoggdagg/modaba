package com.ssafy.s14p11c204.server.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLocationDto {
    private Long roomId;
    private Integer userId;
    private Double lat;
    private Double lng;
    private Long timestamp; // 클라이언트 시간 or 서버 시간
}