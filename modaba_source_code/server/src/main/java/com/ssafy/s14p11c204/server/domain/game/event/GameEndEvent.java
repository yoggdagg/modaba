package com.ssafy.s14p11c204.server.domain.game.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GameEndEvent {
    private final Long roomId;
}
