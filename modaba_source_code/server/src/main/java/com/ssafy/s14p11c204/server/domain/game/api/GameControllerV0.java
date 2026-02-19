package com.ssafy.s14p11c204.server.domain.game.api;

import com.ssafy.s14p11c204.server.domain.game.dto.GameBoundaryDto;
import com.ssafy.s14p11c204.server.domain.game.dto.GameResultDto;
import com.ssafy.s14p11c204.server.domain.game.dto.GameResultRequestDto;
import com.ssafy.s14p11c204.server.domain.game.dto.MyPosition;
import com.ssafy.s14p11c204.server.domain.game.dto.TagRequest;
import com.ssafy.s14p11c204.server.domain.game.service.GameResultService;
import com.ssafy.s14p11c204.server.domain.game.service.GameService;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v0/games/{roomId}") // games로 통일 (테스트 코드와 일치)
@Tag(name = "GameController", description = "게임 진행 및 결과 처리 API")
public class GameControllerV0 {
    private final GameService gameService;
    private final GameResultService gameResultService;

    @GetMapping
    @Operation(summary = "게임 방 상세 조회", description = "참여자 목록, 역할(경찰/도둑), 준비 상태 등을 포함한 상세 정보를 조회합니다.")
    public ResponseEntity<com.ssafy.s14p11c204.server.domain.game.dto.GameRoomDetailDto> getRoomDetail(@PathVariable Long roomId) {
        return ResponseEntity.ok(gameService.getRoomDetail(roomId));
    }

    @PostMapping
    @Operation(
        summary = "게임 시작 (구역 설정 포함)", 
        description = "방장이 게임을 시작합니다. 도넛 모양의 게임 구역과 다각형 감옥 구역을 설정할 수 있습니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = GameBoundaryDto.class),
                examples = @ExampleObject(
                    name = "도넛 모양 구역 예시",
                    value = "{\"roomId\": 1, \"coordinates\": [[[127.0, 37.0], [127.1, 37.0], [127.1, 37.1], [127.0, 37.1]], [[127.04, 37.04], [127.06, 37.04], [127.06, 37.06], [127.04, 37.06]]], \"jailCoordinates\": [[127.02, 37.02], [127.03, 37.02], [127.03, 37.03], [127.02, 37.03]]}"
                )
            )
        )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK : 게임이 시작되었습니다!"),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST : 이미 시작된 게임입니다."),
            @ApiResponse(responseCode = "403", description = "FORBIDDEN : 방장이 아닙니다."),
            @ApiResponse(responseCode = "409", description = "CONFLICT : 전원의 준비가 안 됐습니다."),
    })
    public ResponseEntity<Void> gameStart(
            @PathVariable Long roomId, 
            @AuthenticationPrincipal CurrentUser user,
            @RequestBody(required = false) GameBoundaryDto boundaryDto) {
        
        if (boundaryDto != null) {
            gameService.gameStart(roomId, user.id(), boundaryDto);
        } else {
            gameService.gameStart(roomId, user.id());
        }
        return ResponseEntity.ok().build();
    }

    @PatchMapping
    @Operation(summary = "게임 준비 완료/취소", description = "본인이 게임할 준비가 (안) 되었음을 알립니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "NO CONTENT : 반영되었습니다!"),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST : 이미 시작된 게임입니다."),
            @ApiResponse(responseCode = "403", description = "FORBIDDEN : 방의 멤버가 아니십니다."),
    })
    public ResponseEntity<Void> ready(@PathVariable Long roomId, @AuthenticationPrincipal CurrentUser user, @RequestParam boolean isReady) {
        gameService.ready(roomId, user.id(), isReady);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/tagging")
    @Operation(summary = "특정인을 태그함", description = "게임 종료는 서버가 계산")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "NO CONTENT : 태그 이벤트가 잘 발송되었습니다!"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND : 해당 태그에 대응되는 사용자는 플레이어가 아닙니다.")
    })
    public ResponseEntity<Void> tag(@PathVariable Long roomId, @AuthenticationPrincipal CurrentUser user, @RequestBody TagRequest tagRequest) {
        gameService.tagProcess(roomId, user.id(), tagRequest);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/position")
    @Operation(summary = "본인의 위치를 보고함", description = "구역 탈출 판단")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "NO CONTENT : 위치 보고 이벤트가 발송되었습니다!")
    })
    public ResponseEntity<Void> position(@PathVariable Long roomId, @AuthenticationPrincipal CurrentUser user, @RequestBody MyPosition myPosition) {
        gameService.refreshPosition(roomId, user.id(), myPosition);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/result")
    @Operation(summary = "게임 종료 및 결과 처리", description = "게임이 종료되면 승리 팀을 입력받아 MMR을 정산합니다.")
    public ResponseEntity<GameResultDto> finishGame(
            @PathVariable Long roomId,
            @RequestBody GameResultRequestDto request) {
        
        GameResultDto result = gameResultService.processGameResult(roomId, request.getWinnerTeam());
        return ResponseEntity.ok(result);
    }
}
