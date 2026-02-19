package com.ssafy.s14p11c204.server.domain.chat.service;

import com.ssafy.s14p11c204.server.domain.chat.dto.ChatMessageDto;

import java.util.List;

public interface ChatService {
    void saveMessage(ChatMessageDto messageDto);
    void publishMessage(ChatMessageDto messageDto); // DB 저장 없이 발행만
    List<ChatMessageDto> getMessages(Long roomId, Long lastLogId);
}