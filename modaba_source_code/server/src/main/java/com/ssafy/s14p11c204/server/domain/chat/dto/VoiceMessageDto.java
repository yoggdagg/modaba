package com.ssafy.s14p11c204.server.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceMessageDto {
    private Long roomId;
    private Long senderId;
    private String senderNickname;
    private byte[] audioData; // AAC Encoded Binary Data
    private Double timestamp; // [수정] double -> Double (null 허용)
    private String team;      // "POLICE" or "THIEF" (서버에서 검증 후 세팅)
    private boolean loopback; // [추가] 루프백 여부 (자신에게만 되돌려줌)
}
