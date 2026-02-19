package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.chat.service.RedisPublisher;
import com.ssafy.s14p11c204.server.domain.game.dao.GameMapper;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.LocationMessageDto;
import com.ssafy.s14p11c204.server.global.util.GeometryUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
// import org.locationtech.jts.geom.Point; // JTS Point 제거
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceImplTest {

    @InjectMocks
    private GameServiceImpl gameService;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private GameMapper gameMapper;

    @Mock
    private RedisPublisher redisPublisher;

    @Mock
    private ActivityService activityService;

    @Mock
    private ChannelTopic channelTopic;

    @Mock
    private GameResultService gameResultService;

    @Mock
    private RedisGeoService redisGeoService;

    @Spy
    private GeometryUtil geometryUtil = new GeometryUtil(); // 실제 로직 사용

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Test
    @DisplayName("탈옥 성공: 구출자가 감옥 근처(5m 이내)에 있을 때")
    void unleashSuccess() {
        // Given
        Long roomId = 1L;
        String rescuer = "hero";
        String jailWkt = "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))"; // 0~10 정사각형 감옥
        
        // 구출자 위치: (5, 5) -> 감옥 내부 (성공)
        // Spring Data Redis Point 사용 (x=lng, y=lat)
        org.springframework.data.geo.Point rescuerLocation = new org.springframework.data.geo.Point(5.0, 5.0);

        when(redisGeoService.getUserLocation(roomId, rescuer)).thenReturn(rescuerLocation);
        when(roomMapper.findRoomBoundary(roomId)).thenReturn(Optional.of(new RoomMapper.RoomBoundaryInfo("...", jailWkt)));

        // When
        gameService.handleUnleash(roomId, rescuer);

        // Then
        // 1. 감옥 도둑 상태 변경 호출 확인
        verify(roomMapper).updateAllArrestedThievesToAlive(roomId);
        
        // 2. 성공 메시지 전송 확인
        ArgumentCaptor<LocationMessageDto> captor = ArgumentCaptor.forClass(LocationMessageDto.class);
        verify(redisPublisher).publish(any(), captor.capture());
        
        LocationMessageDto message = captor.getValue();
        assertThat(message.getType()).isEqualTo(LocationMessageDto.MessageType.UNLEASH_RESULT);
        assertThat(message.getSuccess()).isTrue();
    }

    @Test
    @DisplayName("탈옥 실패: 구출자가 감옥에서 멀 때")
    void unleashFail_TooFar() {
        // Given
        Long roomId = 1L;
        String rescuer = "hero";
        String jailWkt = "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))";
        
        // 구출자 위치: (20, 20) -> 감옥 외부 (실패)
        org.springframework.data.geo.Point rescuerLocation = new org.springframework.data.geo.Point(20.0, 20.0);

        when(redisGeoService.getUserLocation(roomId, rescuer)).thenReturn(rescuerLocation);
        when(roomMapper.findRoomBoundary(roomId)).thenReturn(Optional.of(new RoomMapper.RoomBoundaryInfo("...", jailWkt)));

        // When
        gameService.handleUnleash(roomId, rescuer);

        // Then
        // 1. 상태 변경 호출 안 됨
        verify(roomMapper, never()).updateAllArrestedThievesToAlive(roomId);
        
        // 2. 실패 메시지 전송 확인
        ArgumentCaptor<LocationMessageDto> captor = ArgumentCaptor.forClass(LocationMessageDto.class);
        verify(redisPublisher).publish(any(), captor.capture());
        
        LocationMessageDto message = captor.getValue();
        assertThat(message.getSuccess()).isFalse();
        assertThat(message.getMessage()).contains("너무 멉니다");
    }

    @Test
    @DisplayName("탈옥 실패: 구출자 위치 정보 없음")
    void unleashFail_NoLocation() {
        // Given
        Long roomId = 1L;
        String rescuer = "hero";

        when(redisGeoService.getUserLocation(roomId, rescuer)).thenReturn(null);

        // When
        gameService.handleUnleash(roomId, rescuer);

        // Then
        verify(roomMapper, never()).updateAllArrestedThievesToAlive(roomId);
        
        ArgumentCaptor<LocationMessageDto> captor = ArgumentCaptor.forClass(LocationMessageDto.class);
        verify(redisPublisher).publish(any(), captor.capture());
        
        assertThat(captor.getValue().getSuccess()).isFalse();
        assertThat(captor.getValue().getMessage()).contains("위치 정보를 찾을 수 없습니다");
    }
}
