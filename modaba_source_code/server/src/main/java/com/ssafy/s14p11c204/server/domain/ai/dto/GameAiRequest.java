package com.ssafy.s14p11c204.server.domain.ai.dto;

import java.util.List;

public record GameAiRequest(
    String role,            // POLICE, THIEF
    String result,          // WIN, LOSE
    int totalDistanceM,
    double maxSpeedKmh,
    int playTimeMin,
    List<String> locations  // ["신창동", "수완동"]
) {}
