package com.ssafy.s14p11c204.server.domain.chat.service;

import com.ssafy.s14p11c204.server.domain.chat.dao.ChatDao;
import com.ssafy.s14p11c204.server.domain.chat.dto.ChatMessageDto;
import com.ssafy.s14p11c204.server.domain.chat.dto.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatDao chatDao;
    private final RedisPublisher redisPublisher;
    private final ChannelTopic channelTopic;
    
    // 페이지 크기 상수 정의
    private static final int PAGE_SIZE = 20;

    @Override
    @Transactional
    public void saveMessage(ChatMessageDto messageDto) {
        log.info("ChatService saveMessage: {}", messageDto);

        // 컨트롤러에서 senderId를 보장하므로, 여기서 다시 조회할 필요 없음
        if (messageDto.getSenderId() == null) {
            throw new IllegalArgumentException("발신자 정보(senderId)가 없습니다.");
        }

        if (MessageType.ENTER.equals(messageDto.getType())) {
            messageDto.setMessage(messageDto.getSenderNickname() + "님이 입장하셨습니다.");
        } else if (MessageType.QUIT.equals(messageDto.getType())) {
            messageDto.setMessage(messageDto.getSenderNickname() + "님이 퇴장하셨습니다.");
        }

        if (messageDto.getCreatedAt() == null) {
            messageDto.setCreatedAt(LocalDateTime.now());
        }

        chatDao.save(messageDto);
        redisPublisher.publish(channelTopic, messageDto);
    }

    @Override
    public void publishMessage(ChatMessageDto messageDto) {
        // DB 저장 없이 생성 시간만 설정하고 즉시 발행
        if (messageDto.getCreatedAt() == null) {
            messageDto.setCreatedAt(LocalDateTime.now());
        }
        log.info("ChatService publishing volatile message (type: {}): {}", messageDto.getType(), messageDto);
        redisPublisher.publish(channelTopic, messageDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessages(Long roomId, Long lastLogId) {
        // 검색 기준 시간 설정 (처음 조회 시 최근 24시간)
        LocalDateTime since = null;
        if (lastLogId == null) {
            since = LocalDateTime.now().minusHours(24);
        }

        // 4개의 인자를 모두 전달하여 호출
        return chatDao.findMessages(roomId, lastLogId, PAGE_SIZE, since);
    }
}
