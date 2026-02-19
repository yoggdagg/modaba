package com.ssafy.s14p11c204.server.domain.game.api;

import com.ssafy.s14p11c204.server.domain.game.dto.RoomRequestDto;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomResponseDto;
import com.ssafy.s14p11c204.server.domain.game.service.RoomService;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v0/rooms")
@Tag(name = "RoomController", description = "경찰과 도둑 방과 관련된 API")
public class RoomControllerV0 {
    private final RoomService roomService;

    @PostMapping
    @Operation(summary = "새로운 게임방을 파둡니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "CREATED : 새로운 방을 파는 데 성공했습니다!"),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST : 입력이 잘못됐습니다."),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED : 인증이 필요합니다.")
    })
    public ResponseEntity<Void> open(@Valid @RequestBody RoomRequestDto request, @AuthenticationPrincipal CurrentUser currentUser) {
        // 닉네임 기반으로 서비스 호출
        roomService.createRoom(request, currentUser.nickname());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(request.getRoomId())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/me")
    @Operation(summary = "내 게임방 조회", description = "내가 참여 중인 방 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "OK : 조회 성공")
    public ResponseEntity<List<RoomResponseDto>> findMyRooms(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(roomService.findMyRooms(currentUser.nickname()));
    }

    @GetMapping
    @Operation(summary = "참여 가능 게임방 조회", description = "내가 참여하지 않은(참여 가능한) 방 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "OK : 조회 성공")
    public ResponseEntity<List<RoomResponseDto>> findAvailableRooms(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(roomService.findAvailableRooms(currentUser.nickname()));
    }

    @GetMapping("/search")
    @Operation(summary = "지역별 게임방 검색", description = "선택한 지역에 생성된 방 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "OK : 조회 성공")
    public ResponseEntity<List<RoomResponseDto>> searchRooms(
            @RequestParam String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String neighborhood) {
        return ResponseEntity.ok(roomService.findRoomsByRegion(city, district, neighborhood));
    }

    @PostMapping("/{roomId}")
    @Operation(summary = "선택한 게임 참가", description = "선택한 방에 참여합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK : 방에 성공적으로 들어왔습니다!"),
            @ApiResponse(responseCode = "409", description = "CONFLICT : 인원 초과로 인해 들어가지 못했습니다."),
            @ApiResponse(responseCode = "404", description = "NOT FOUND : 그런 방이 없습니다."),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED : 인증이 필요합니다.")
    })
    public ResponseEntity<Void> join(@PathVariable long roomId, @AuthenticationPrincipal CurrentUser currentUser) {
        try {
            roomService.joinRoom(roomId, currentUser.nickname());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).build();
        }
    }

    @DeleteMapping("/{roomId}")
    @Operation(summary = "현재 게임 퇴장", description = "현재 방에서 나갑니다. 방장이 나갔다면 방장이 양도되어야 하고, 마지막 인원이면 방이 폐쇄되어야 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "NO CONTENT : 퇴장 성공!"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED : 인증이 필요합니다."),
            @ApiResponse(responseCode = "404", description = "NOT FOUND : 그런 방이 없습니다.")
    })
    public ResponseEntity<Void> leave(@PathVariable Long roomId, @AuthenticationPrincipal CurrentUser currentUser) {
        roomService.leaveRoom(roomId, currentUser.nickname());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roomId}/location-sharing")
    @Operation(summary = "위치 공유 수락/거절", description = "스터디 시작 전 위치 공유 여부를 설정합니다.")
    @ApiResponse(responseCode = "200", description = "OK : 설정 완료")
    public ResponseEntity<Void> updateLocationSharing(
            @PathVariable Long roomId,
            @RequestParam boolean enabled,
            @AuthenticationPrincipal CurrentUser currentUser) {
        roomService.updateLocationSharing(roomId, currentUser.nickname(), enabled);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test-user")
    @Operation(summary = "테스트용 유저 생성", description = "테스트용 유저를 생성하고 ID와 닉네임을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "OK : 생성 성공")
    public ResponseEntity<Map<String, Object>> createTestUser() {
        String email = "test2@example.com";
        String nickname = "테스트유저2";
        roomService.createTestUser(email, nickname);
        Long userId = roomService.findUserIdByEmail(email);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("nickname", nickname);

        return ResponseEntity.ok(response);
    }
}
