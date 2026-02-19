package com.ssafy.s14p11c204.server.domain.ai.dto;

import java.util.List;

public record GameAiResponse(
    String summary_title,
    String commentary,
    List<String> play_style_tag,
    String fitness_report
) {}
