package com.ssafy.s14p11c204.server.domain.chat.dao;

import com.ssafy.s14p11c204.server.domain.chat.dto.ChatMessageDto;
import com.ssafy.s14p11c204.server.domain.chat.mapper.ChatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatDao {
    private final ChatMapper chatMapper;

    public void save(ChatMessageDto messageDto) {
        chatMapper.insertMessage(messageDto);
    }

    public List<ChatMessageDto> findMessages(Long roomId, Long lastLogId, int limit, LocalDateTime since) {
        return chatMapper.findMessagesByRoomId(roomId, lastLogId, limit, since);
    }
}