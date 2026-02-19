package com.ssafy.s14p11c204.server.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ssafy.s14p11c204.server.domain.game.dto.PlayerRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessageDto {
    private Long logId;           // 로그 ID (DB 저장 후 생성)
    private Long roomId;          // 방 번호
    private Long senderId;     // 보낸 사람 ID
    private String senderNickname; // 보낸 사람 닉네임 (편의상 추가)
    private Long recipientId;    // 수신자 ID (1:1 시그널링 또는 귓속말용, null이면 전체 전송)
    private String message;       // 메시지 내용
    private MessageType type;     // 메시지 타입
    private LocalDateTime createdAt; // 생성 시간

    // 팀 채팅을 위한 필드 추가
    private PlayerRole targetRole; // POLICE, THIEF, null(전체)
    
    // 4단계: 확장성을 위한 상태값 (추후 사용)
    @Builder.Default
    private String status = "NORMAL";

    // 세션 정보 (추가됨)
    private Long sessionId;
}
