package com.ssafy.s14p11c204.server.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LocationMessageDto {
    public enum MessageType {
        LOCATION,       // 위치 업데이트 (클라 -> 서버)
        ARREST_REQUEST, // 검거 요청 (경찰 -> 도둑)
        ESCAPE_REQUEST, // 탈출 요청 (도둑 -> 본부)
        UNLEASH_REQUEST, // 탈옥 요청 (도둑 -> 감옥)
        
        LOCATION_UPDATE, // 위치 정보 전파 (서버 -> 클라)
        ARREST_RESULT,   // 검거 결과 (서버 -> 클라)
        UNLEASH_RESULT,   // 탈옥 결과 (서버 -> 클라)
        
        WARNING          // 경고 (구역 이탈 등)
    }

    private MessageType type;
    private Long roomId;
    private String senderNickname;
    private Double lat;
    private Double lng;
    private PlayerRole role; // 역할 필드 추가
    
    // 4단계: 확장성을 위한 상태값
    @Builder.Default
    private String status = "NORMAL"; // NORMAL, ITEM_ACTIVE 등
    
    private String targetNickname;
    private String message;
    private Boolean success; // boolean -> Boolean 변경 (null 허용)
    
    // 인원 변동 정보 (추가됨)
    private Integer aliveThiefCount;
    
    // 세션 정보 (추가됨)
    private Long sessionId;
}
