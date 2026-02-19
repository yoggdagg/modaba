package com.ssafy.s14p11c204.server.domain.user.api;

import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import com.ssafy.s14p11c204.server.domain.user.dto.MmrHistoryDto;
import com.ssafy.s14p11c204.server.domain.user.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v0/users")
@Tag(name = "UserController", description = "사용자 정보 조회 API")
@RestController
public class UserControllerV0 {

    private final AccountService accountService;

    @GetMapping("/me/mmr-history")
    @Operation(summary = "내 MMR 변동 이력 조회", description = "로그인한 사용자의 MMR 변동 내역을 조회합니다.")
    public ResponseEntity<List<MmrHistoryDto>> getMyMmrHistory(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(accountService.getMmrHistory(currentUser.getUsername()));
    }
}
