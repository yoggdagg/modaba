package com.ssafy.s14p11c204.server.domain.chat.service;

import com.ssafy.s14p11c204.server.domain.chat.dto.VoiceMessageDto;
import com.ssafy.s14p11c204.server.domain.game.dao.GameMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.PlayerRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RadioService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final GameMapper gameMapper;

    // 무전 음성 데이터 처리
    public void processVoice(VoiceMessageDto voiceDto) {
        // 루프백 요청인 경우 별도 처리
        if (voiceDto.isLoopback()) {
            voiceDto.setTeam("LOOPBACK");
            String topicName = String.format("radio:room:%d:user:%d:loopback", voiceDto.getRoomId(), voiceDto.getSenderId());
            log.info("[RadioService] 루프백 발행: Topic={}, Sender={}", topicName, voiceDto.getSenderId());
            redisTemplate.convertAndSend(topicName, voiceDto);
            return;
        }


        // 1. 발신자의 역할(팀) 조회 (DB 조회)
        PlayerRole role = gameMapper.findParticipantRole(voiceDto.getRoomId(), voiceDto.getSenderId())
                .orElseGet(() -> {
                    // [테스트용] DB에 정보가 없으면 기본적으로 POLICE 팀으로 설정하여 루프백 허용
                    log.warn("[RadioService] 역할을 찾을 수 없어 테스트용으로 'POLICE' 팀 할당 (User: {})", voiceDto.getSenderId());
                    return PlayerRole.POLICE;
                });

        voiceDto.setTeam(role.name());

        // 2. Redis 채널 결정 (radio:room:{roomId}:team:{team})
        String topicName = String.format("radio:room:%d:team:%s", voiceDto.getRoomId(), voiceDto.getTeam());
        
        log.info("[RadioService] Redis 발행 시작: Topic={}, Sender={}", topicName, voiceDto.getSenderId());
        
        // 3. Redis 발행
        redisTemplate.convertAndSend(topicName, voiceDto);
    }
}