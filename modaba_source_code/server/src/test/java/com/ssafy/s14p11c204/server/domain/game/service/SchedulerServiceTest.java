package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.chat.dto.ChatMessageDto;
import com.ssafy.s14p11c204.server.domain.chat.dto.MessageType;
import com.ssafy.s14p11c204.server.domain.chat.service.RedisPublisher;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.listener.ChannelTopic;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private RedisPublisher redisPublisher;

    @Mock
    private ChannelTopic channelTopic;
    
    // gameResultService는 checkTimeOverGames에서 사용되지만 여기서는 테스트하지 않으므로 Mock 처리
    @Mock
    private GameResultService gameResultService;

    @InjectMocks
    private SchedulerService schedulerService;

    @Test
    @DisplayName("스터디(FOCUS) 알림 및 위치 공유 요청 전송 테스트")
    void checkUpcomingRooms_FocusType() {
        // given
        RoomResponseDto focusRoom = RoomResponseDto.builder()
                .roomId(1L)
                .roomType("FOCUS")
                .title("스터디 방")
                .build();

        when(roomMapper.findRoomsStartingSoon(any(), any()))
                .thenReturn(List.of(focusRoom));

        // when
        schedulerService.checkUpcomingRooms();

        // then
        ArgumentCaptor<ChatMessageDto> messageCaptor = ArgumentCaptor.forClass(ChatMessageDto.class);
        verify(redisPublisher).publish(any(ChannelTopic.class), messageCaptor.capture());

        ChatMessageDto sentMessage = messageCaptor.getValue();
        assertEquals(MessageType.LOCATION_REQUEST, sentMessage.getType());
        assertEquals("곧 스터디 시작 시간입니다. 위치 공유를 시작하시겠습니까?", sentMessage.getMessage());

        verify(roomMapper).updateNotiSent(1L, true);
    }

    @Test
    @DisplayName("일반(KYUNGDO) 알림 전송 테스트")
    void checkUpcomingRooms_NormalType() {
        // given
        RoomResponseDto normalRoom = RoomResponseDto.builder()
                .roomId(2L)
                .roomType("KYUNGDO")
                .title("경도 방")
                .build();

        when(roomMapper.findRoomsStartingSoon(any(), any()))
                .thenReturn(List.of(normalRoom));

        // when
        schedulerService.checkUpcomingRooms();

        // then
        ArgumentCaptor<ChatMessageDto> messageCaptor = ArgumentCaptor.forClass(ChatMessageDto.class);
        verify(redisPublisher).publish(any(ChannelTopic.class), messageCaptor.capture());

        ChatMessageDto sentMessage = messageCaptor.getValue();
        assertEquals(MessageType.EVENT, sentMessage.getType());
        assertEquals("약속 시간 1시간 전입니다! 잊지 않으셨죠?", sentMessage.getMessage());

        verify(roomMapper).updateNotiSent(2L, true);
    }

    @Test
    @DisplayName("알림 대상이 없을 때")
    void checkUpcomingRooms_Empty() {
        // given
        when(roomMapper.findRoomsStartingSoon(any(), any()))
                .thenReturn(Collections.emptyList());

        // when
        schedulerService.checkUpcomingRooms();

        // then
        verify(redisPublisher, never()).publish(any(ChannelTopic.class), any(ChatMessageDto.class));
        verify(roomMapper, never()).updateNotiSent(any(), anyBoolean());
    }
}
