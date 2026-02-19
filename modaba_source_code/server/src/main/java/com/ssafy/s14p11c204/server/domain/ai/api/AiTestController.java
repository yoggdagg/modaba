package com.ssafy.s14p11c204.server.domain.ai.api;

import com.ssafy.s14p11c204.server.domain.ai.dto.GameAiRequest;
import com.ssafy.s14p11c204.server.domain.ai.dto.GameAiResponse;
import com.ssafy.s14p11c204.server.domain.ai.service.AiTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v0/ai")
@RequiredArgsConstructor
@Tag(name = "AI Test", description = "AI 연결 테스트 API")
public class AiTestController {

    private final AiTestService aiTestService;

    @GetMapping("/test")
    @Operation(summary = "AI 연결 테스트", description = "OpenAI API에 간단한 질문을 보내고 응답을 받습니다.")
    public ResponseEntity<String> testAi() {
        return ResponseEntity.ok(aiTestService.testAiConnection());
    }

    @PostMapping("/report")
    @Operation(summary = "게임 리포트 생성 테스트", description = "게임 데이터를 바탕으로 AI 분석 리포트를 생성합니다.")
    public ResponseEntity<GameAiResponse> generateReport(@RequestBody GameAiRequest request) {
        return ResponseEntity.ok(aiTestService.generateGameReport(request));
    }
}
