package com.ssafy.s14p11c204.server.domain.user.api;

import com.ssafy.s14p11c204.server.domain.user.User;
import com.ssafy.s14p11c204.server.domain.user.dto.*;
import com.ssafy.s14p11c204.server.domain.user.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/v0/accounts")
@Tag(name = "AccountController", description = "본인 계정을 관리하기 위한 API")
@RestController
class AccountControllerV0 {
        private final AccountService accountService;

        @GetMapping
        @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 계정 정보를 가져옵니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "OK : 조회에 성공했습니다!"),
        })
        ResponseEntity<User> getMyProfile(@AuthenticationPrincipal CurrentUser currentUser) {
                return ResponseEntity.ok(
                                accountService.getDetailedProfile(currentUser.getUsername()));
        }

        @PatchMapping
        @Operation(summary = "프로필 수정", description = "닉네임 등 본인의 계정 정보를 일부 수정합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "OK : 수정에 성공했습니다!"),
                        @ApiResponse(responseCode = "400", description = "BAD REQUEST : 입력이 유효하지 않습니다."),
                        @ApiResponse(responseCode = "409", description = "CONFLICT : 닉네임 중복이 발생했습니다."),
        })
        ResponseEntity<Void> updateProfile(
                        @AuthenticationPrincipal CurrentUser currentUser,
                        @Valid @RequestBody ProfileUpdateRequest request) {
                accountService.updateProfile(currentUser.getUsername(), request);
                return ResponseEntity.ok().build();
        }

        @PatchMapping("/password")
        @Operation(summary = "비밀번호 변경", description = "로그인 상태에서 새 비밀번호로 교체합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "NO CONTENT : 비밀번호 변경에 성공했습니다!"),
                        @ApiResponse(responseCode = "400", description = "BAD REQUEST : 비밀번호가 유효하지 않습니다."),
        })
        ResponseEntity<Void> updatePassword(
                        @AuthenticationPrincipal CurrentUser currentUser,
                        @RequestBody PwUpdateRequest request) {
                accountService.updatePassword(currentUser.getUsername(), request);
                return ResponseEntity.noContent().build(); // 비밀번호 변경 후 로그아웃 API 콜은 프론트 담당
        }

        @DeleteMapping
        @Operation(summary = "회원 탈퇴", description = "계정을 삭제하고 연동된 정보를 파기합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "탈퇴 처리에 성공했습니다!")
        })
        public ResponseEntity<Map<String, String>> unregister(@AuthenticationPrincipal CurrentUser currentUser) {
                accountService.unregister(currentUser.getUsername());
                return ResponseEntity.ok().body(Map.of("message", "회원 탈퇴 완료"));
        }
}
