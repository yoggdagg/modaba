package com.ssafy.s14p11c204.server.domain.chat.api;

import com.ssafy.s14p11c204.server.domain.chat.dto.ChatMessageDto;
import com.ssafy.s14p11c204.server.domain.chat.service.ChatService;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "ChatController", description = "채팅 관련 API")
public class ChatController {

    private final ChatService chatService;

    /**
     * websocket "/pub/chat/message"로 들어오는 메시징을 처리한다.
     */
    @MessageMapping("/chat/message")
    public void message(ChatMessageDto message, Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof CurrentUser currentUser) {
            // 보안 강화: Principal에서 ID와 닉네임을 모두 가져와 강제 설정
            // 클라이언트가 보낸 senderId나 nickname은 무시하고 서버 인증 정보로 덮어씀
            message.setSenderId(currentUser.id());
            message.setSenderNickname(currentUser.nickname());
            
            log.debug("Authenticated message from: ID={}, Nickname={}", currentUser.id(), currentUser.nickname());
        } else {
            log.error("Unauthenticated message received. Access denied.");
            return; // 인증되지 않은 메시지는 처리하지 않음
        }

        // Optional을 사용하여 타입이 null일 경우 기본값 TALK 설정
        java.util.Optional.ofNullable(message.getType())
                .ifPresentOrElse(
                        type -> {}, // 값이 있으면 그대로 둠
                        () -> {
                            log.warn("Message type is null, defaulting to TALK for message: {}", message);
                            message.setType(com.ssafy.s14p11c204.server.domain.chat.dto.MessageType.TALK);
                        }
                );

        switch (message.getType()) {
            case TALK, ENTER, QUIT, CAPTURE, UNLEASH -> chatService.saveMessage(message);
            case OFFER, ANSWER, CANDIDATE -> chatService.publishMessage(message);
            default -> {
                log.warn("Unknown message type: {}", message.getType());
                chatService.publishMessage(message);
            }
        }
    }

    @GetMapping("/api/v0/chat/rooms/{roomId}/messages")
    @Operation(summary = "채팅 내역 조회", description = "특정 방의 채팅 내역을 조회합니다.")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastLogId) {
        return ResponseEntity.ok(chatService.getMessages(roomId, lastLogId));
    }
}
