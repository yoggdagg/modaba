package com.ssafy.s14p11c204.server.domain.game.api;

import com.ssafy.s14p11c204.server.domain.game.dto.StudyStateResponseDto;
import com.ssafy.s14p11c204.server.domain.game.service.StudyService;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v0/study")
@RequiredArgsConstructor
@Tag(name = "StudyController", description = "스터디(몰입) 관련 API")
public class StudyController {

    private final StudyService studyService;

    @GetMapping("/current-state")
    @Operation(summary = "현재 공부 상태 조회 (복구용)", description = "진행 중인 스터디 세션이 있는지 확인하고 정보를 반환합니다.")
    public ResponseEntity<StudyStateResponseDto> getCurrentState(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(studyService.getCurrentState(currentUser.nickname()));
    }

    @PostMapping("/{roomId}/arrival")
    @Operation(summary = "스터디 장소 도착 인증", description = "현재 위치가 스터디 목적지 10m 이내인지 확인하고 도착 처리를 합니다.")
    public ResponseEntity<Map<String, Object>> verifyArrival(
            @PathVariable Long roomId,
            @RequestBody Map<String, Double> location,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        Double lat = location.get("lat");
        Double lng = location.get("lng");
        
        boolean success = studyService.verifyArrival(roomId, currentUser.nickname(), lat, lng);
        
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "도착 인증에 성공했습니다!" : "아직 목적지 근처가 아닙니다."
        ));
    }

    @PostMapping("/{roomId}/tag")
    @Operation(summary = "사용자 태그", description = "도착한 사용자끼리 서로 태그하여 공부 시작을 알립니다.")
    public ResponseEntity<Map<String, Object>> tagUser(
            @PathVariable Long roomId,
            @RequestParam String targetNickname,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        Long sessionId = studyService.tagUser(roomId, currentUser.nickname(), targetNickname);
        
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "message", "태그에 성공했습니다. 공부 세션이 시작되었습니다!"
        ));
    }

    @PostMapping("/focus-log")
    @Operation(summary = "집중도 점수 기록", description = "안드로이드에서 분석한 집중도 점수를 1분마다 서버에 전송합니다.")
    public ResponseEntity<Void> addFocusLog(
            @RequestParam Long sessionId,
            @RequestParam Double score,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        studyService.addFocusLog(sessionId, currentUser.nickname(), score);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "세션 생존 신고", description = "앱이 켜져 있음을 서버에 알려 세션이 자동 종료되지 않게 합니다.")
    public ResponseEntity<Void> heartbeat(
            @RequestParam Long sessionId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        studyService.heartbeat(sessionId, currentUser.nickname());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/end")
    @Operation(summary = "스터디 종료 및 리포트 생성", description = "스터디를 종료하고 AI 리포트를 생성합니다.")
    public ResponseEntity<Map<String, Object>> endStudy(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        
        Long reportId = studyService.endStudy(roomId, currentUser.nickname());
        
        return ResponseEntity.ok(Map.of(
                "message", "스터디가 종료되었습니다. 수고하셨습니다!",
                "reportId", reportId != null ? reportId : -1
        ));
    }
}
