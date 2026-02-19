package com.ssafy.s14p11c204.server.domain.game.api;

import com.ssafy.s14p11c204.server.domain.game.dto.GameResultDto;
import com.ssafy.s14p11c204.server.domain.game.dto.GameStartResponseDto;
import com.ssafy.s14p11c204.server.domain.game.dto.PlayerRole;
import com.ssafy.s14p11c204.server.domain.game.service.GameTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v0/test/game")
@Tag(name = "GameTestController", description = "게임 테스트용 치트 API (개발용)")
public class GameTestController {

    private final GameTestService gameTestService;

    @PostMapping("/{roomId}/force-arrest")
    @Operation(summary = "강제 검거 (경찰 승리)", description = "모든 도둑을 검거 상태로 만들고 경찰 승리로 게임을 종료합니다.")
    public ResponseEntity<GameResultDto> forceArrest(@PathVariable Long roomId) {
        return ResponseEntity.ok(gameTestService.forceArrestAllThieves(roomId));
    }

    @PostMapping("/{roomId}/force-timeout")
    @Operation(summary = "강제 타임아웃 (도둑 승리)", description = "시간 초과로 간주하여 도둑 승리로 게임을 종료합니다.")
    public ResponseEntity<GameResultDto> forceTimeout(@PathVariable Long roomId) {
        return ResponseEntity.ok(gameTestService.forceTimeout(roomId));
    }

    @GetMapping("/start-sample")
    @Operation(summary = "게임 시작 메시지 샘플 조회", description = "WebSocket으로 전송되는 GAME_START 메시지의 구조를 확인하기 위한 샘플 API입니다.")
    public ResponseEntity<GameStartResponseDto> getGameStartSample() {
        GameStartResponseDto sample = GameStartResponseDto.builder()
                .type("GAME_START")
                .roomId(1L)
                .policeCount(1)
                .thiefCount(2)
                .participants(List.of(
                        GameStartResponseDto.ParticipantInfo.builder().userId(10L).nickname("포돌이").role(PlayerRole.POLICE).build(),
                        GameStartResponseDto.ParticipantInfo.builder().userId(11L).nickname("루팡").role(PlayerRole.THIEF).build(),
                        GameStartResponseDto.ParticipantInfo.builder().userId(12L).nickname("괴도키드").role(PlayerRole.THIEF).build()
                ))
                .build();
        return ResponseEntity.ok(sample);
    }
}