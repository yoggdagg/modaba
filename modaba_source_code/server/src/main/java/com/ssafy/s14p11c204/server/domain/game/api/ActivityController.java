package com.ssafy.s14p11c204.server.domain.game.api;

import com.ssafy.s14p11c204.server.domain.game.dto.ActivityRecord;
import com.ssafy.s14p11c204.server.domain.game.service.ActivityService;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v0/activities")
@RequiredArgsConstructor
@Tag(name = "ActivityController", description = "유저 활동량 및 AI 리포트 조회 API")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/me")
    @Operation(summary = "내 활동 기록 목록 조회", description = "로그인한 유저의 과거 모든 활동 기록 요약을 조회합니다.")
    public ResponseEntity<List<ActivityRecord>> getMyActivities(@AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(activityService.getUserActivities(user.id()));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "활동 상세 및 AI 리포트 조회", description = "특정 게임 세션의 상세 경로와 AI 분석 리포트를 조회합니다.")
    public ResponseEntity<ActivityRecord> getActivityDetail(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(activityService.getActivityDetail(sessionId, user.id()));
    }
}
