package com.ssafy.s14p11c204.server.domain.social.api;

import com.ssafy.s14p11c204.server.domain.social.Friendship;
import com.ssafy.s14p11c204.server.domain.social.service.FriendService;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import com.ssafy.s14p11c204.server.domain.user.dto.ProfileSimpleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "친구(Social)", description = "친구 신청, 목록 조회, 차단 등 관계 관리 API")
@Validated // @Positive 등 파라미터 검증을 위해 필수
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v0/friends")
class FriendControllerV0 {

    private final FriendService friendService;

    @PostMapping("/{targetUserId}")
    @Operation(summary = "친구 신청 보내기", description = "특정 사용자에게 친구 신청을 보냅니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "ACCEPTED : 신청 완료 (또는 이미 친구라 자동 수락됨)"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND : 해당 사용자가 없습니다."),
            @ApiResponse(responseCode = "409", description = "CONFLICT : 이미 요청을 보냈거나, 자기 자신입니다."),
    })
    public ResponseEntity<Void> sendRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable @Positive long targetUserId
    ) {
        friendService.sendRequest(currentUser.id(), targetUserId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    @Operation(summary = "내 친구 목록 조회", description = "서로 친구(ACCEPTED)인 사용자 목록을 조회합니다.")
    public ResponseEntity<List<ProfileSimpleResponse>> showFriends(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        List<ProfileSimpleResponse> result = friendService.showRelations(currentUser.id(), Friendship.Status.ACCEPTED);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pending")
    @Operation(summary = "받은 친구 신청 조회", description = "나에게 온 친구 신청(PENDING) 목록을 조회합니다.")
    public ResponseEntity<List<ProfileSimpleResponse>> showPending(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        List<ProfileSimpleResponse> result = friendService.showRelations(currentUser.id(), Friendship.Status.PENDING);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/blocked")
    @Operation(summary = "차단한 사용자 조회", description = "내가 차단(REJECTED)한 사용자 목록을 조회합니다.")
    public ResponseEntity<List<ProfileSimpleResponse>> showBlocked(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        List<ProfileSimpleResponse> result = friendService.showRelations(currentUser.id(), Friendship.Status.REJECTED);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{targetUserId}")
    @Operation(summary = "관계 상태 변경 (수락/거절)", description = "친구 신청을 수락(ACCEPTED)하거나 거절(PENDING -> 삭제)합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "NO CONTENT : 처리 성공"),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST : 잘못된 상태 값입니다."),
    })
    public ResponseEntity<Void> respondRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable @Positive long targetUserId,
            @Parameter(description = "변경할 상태 (ACCEPTED: 수락, REJECTED: 차단, PENDING: 삭제/거절)")
            @RequestParam @NotNull Friendship.Status status
    ) {
        friendService.updateRelation(currentUser.id(), targetUserId, status);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{targetUserId}")
    @Operation(summary = "친구 끊기 / 요청 취소", description = "친구 관계를 끊거나, 내가 보낸 신청을 취소합니다.")
    public ResponseEntity<Void> unfriend(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable @Positive long targetUserId
    ) {
        // Service 로직상 PENDING으로 업데이트하면 delete(삭제)가 수행됨
        friendService.updateRelation(currentUser.id(), targetUserId, Friendship.Status.PENDING);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/block/{targetUserId}")
    @Operation(summary = "사용자 차단", description = "특정 사용자를 차단(REJECTED)합니다. 기존 친구 관계도 삭제됩니다.")
    public ResponseEntity<Void> block(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable @Positive long targetUserId
    ) {
        friendService.updateRelation(currentUser.id(), targetUserId, Friendship.Status.REJECTED);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/block/{targetUserId}")
    @Operation(summary = "차단 해제", description = "차단을 해제합니다. (관계가 완전히 삭제된 상태로 돌아갑니다)")
    public ResponseEntity<Void> unblock(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable @Positive long targetUserId
    ) {
        // 차단 해제도 결국 관계를 지우는 것(PENDING 처리 로직)과 동일
        friendService.updateRelation(currentUser.id(), targetUserId, Friendship.Status.PENDING);
        return ResponseEntity.noContent().build();
    }
}