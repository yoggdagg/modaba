package com.ssafy.s14p11c204.server.domain.game.dto;

import lombok.Builder;

@Builder
public record TagRequest(
        long roomId,
        String rawPacketData
) {
}
