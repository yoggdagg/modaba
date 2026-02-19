package com.ssafy.s14p11c204.server.domain.chat.service;

import com.ssafy.s14p11c204.server.domain.chat.dto.ChatMessageDto;
import com.ssafy.s14p11c204.server.domain.game.dto.UserLocationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {
    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(ChannelTopic topic, ChatMessageDto message) {
        log.info("RedisPublisher publishing to topic {}: {}", topic.getTopic(), message);
        redisTemplate.convertAndSend(topic.getTopic(), message);
    }

    public void publish(ChannelTopic topic, UserLocationDto location) {
        // 위치 정보는 너무 자주 찍히므로 로그 레벨을 debug로 하거나 생략
        // log.debug("RedisPublisher publishing location: {}", location);
        redisTemplate.convertAndSend(topic.getTopic(), location);
    }

    // 범용 publish 메서드 추가 (GameStartResponseDto 등 처리용)
    public void publish(ChannelTopic topic, Object message) {
        log.info("RedisPublisher publishing object to topic {}: {}", topic.getTopic(), message);
        redisTemplate.convertAndSend(topic.getTopic(), message);
    }
}