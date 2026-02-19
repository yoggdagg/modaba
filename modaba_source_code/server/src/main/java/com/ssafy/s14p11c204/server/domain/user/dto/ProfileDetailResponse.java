package com.ssafy.s14p11c204.server.domain.user.dto;

import com.ssafy.s14p11c204.server.domain.game.dto.GyeongdoRecord;
import com.ssafy.s14p11c204.server.domain.social.dto.AppointmentRecord;
import com.ssafy.s14p11c204.server.global.format.NicknameFormat;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record ProfileDetailResponse(
        @Positive Long id,
        @NicknameFormat String nickname,
        @Nullable String imageLink, // 이상 3개는 ProfileSimpleResponse에도 있는 객체
        @Nullable GyeongdoRecord gyeongdoRecord,
        @Nullable AppointmentRecord appointmentRecord
) {
}