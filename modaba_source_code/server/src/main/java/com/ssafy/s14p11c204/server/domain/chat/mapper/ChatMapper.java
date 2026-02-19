package com.ssafy.s14p11c204.server.domain.chat.mapper;

import com.ssafy.s14p11c204.server.domain.chat.dto.ChatMessageDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatMapper {
    // 메시지 저장
    void insertMessage(ChatMessageDto messageDto);

    // 메시지 조회 (페이징: lastLogId보다 작은 ID를 limit만큼 조회)
    // lastLogId가 null이면 가장 최신 메시지부터 조회
    List<ChatMessageDto> findMessagesByRoomId(
            @Param("roomId") Long roomId, 
            @Param("lastLogId") Long lastLogId, 
            @Param("limit") int limit,
            @Param("since") LocalDateTime since // 24시간 제한
    );
}