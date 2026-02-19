package com.ssafy.s14p11c204.server.domain.chat.api;

import com.ssafy.s14p11c204.server.domain.chat.dto.VoiceMessageDto;
import com.ssafy.s14p11c204.server.domain.chat.service.RadioService;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RadioController {

    private final RadioService radioService;

    // 클라이언트 발신 경로: /pub/radio/voice
    @MessageMapping("/radio/voice")
    public void sendVoice(VoiceMessageDto voiceDto, Principal principal) {
        log.info("[RadioController] 음성 데이터 수신: Room={}, User={}", voiceDto.getRoomId(), 
                (principal != null ? principal.getName() : "Anonymous"));
        
        if (principal instanceof Authentication authentication && authentication.getPrincipal() instanceof CurrentUser currentUser) {
            voiceDto.setSenderId(currentUser.id());
            voiceDto.setSenderNickname(currentUser.nickname());
        }
        
        // 무전 처리 로직 위임
        radioService.processVoice(voiceDto);
    }
}
