package com.ssafy.s14p11c204.server.domain.game.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRequestDto {
    private Long roomId; // 생성 후 반환용

    @NotBlank(message = "방 제목은 필수입니다.")
    private String title;

    @NotBlank(message = "방 타입은 필수입니다.")
    private String roomType; // "APPOINTMENT", "KYUNGDO", "FOCUS"

    private Integer maxUser;

    @Future(message = "약속 시간은 현재 시간보다 미래여야 합니다.")
    private LocalDateTime appointmentTime;

    private String placeName;
    private Double targetLat;
    private Double targetLng;

    @NotNull(message = "지역 ID는 필수입니다.")
    private Integer regionId;
}