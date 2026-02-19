package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.chat.service.RedisPublisher;
import com.ssafy.s14p11c204.server.domain.game.ParticipantStatus;
import com.ssafy.s14p11c204.server.domain.game.dao.FocusMapper;
import com.ssafy.s14p11c204.server.domain.game.dao.GameMapper;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.LocationMessageDto;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomRequestDto;
import com.ssafy.s14p11c204.server.domain.game.dto.StudyStateResponseDto;
import com.ssafy.s14p11c204.server.global.util.GeometryUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.ChannelTopic;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private GameMapper gameMapper;

    @Mock
    private FocusMapper focusMapper;

    @Mock
    private GeometryUtil geometryUtil;

    @Mock
    private RedisPublisher redisPublisher;

    @Mock
    private ChannelTopic channelTopic;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private AiReportService aiReportService;

    @InjectMocks
    private StudyServiceImpl studyService;

    @Test
    @DisplayName("스터디 정상 종료 및 리포트 생성 요청 테스트")
    void endStudy_Success() {
        // given
        Long roomId = 100L;
        String nickname = "tester";
        Long userId = 1L;
        Long sessionId = 500L;

        when(roomMapper.findUserIdByNickname(nickname)).thenReturn(Optional.of(userId));
        when(gameMapper.findCurrentSessionId(roomId)).thenReturn(Optional.of(sessionId));
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(aiReportService.createReport(sessionId, userId)).thenReturn(10L);

        // when
        Long reportId = studyService.endStudy(roomId, nickname);

        // then
        // 1. 세션 종료 처리 확인
        verify(gameMapper).updateGameSession(roomId, 0, "STUDY_END");
        
        // 2. 평균 점수 정산 확인
        verify(focusMapper).updateAverageScore(sessionId, userId);
        
        // 3. 방 상태 변경 확인
        verify(roomMapper).updateRoomStatus(roomId, "FINISHED");
        
        // 4. Redis 하트비트 삭제 확인
        verify(redisTemplate).delete(contains("study:heartbeat:500:tester"));
        
        // 5. AI 리포트 서비스 호출 확인
        verify(aiReportService).createReport(sessionId, userId);
        
        // 6. 리포트 ID 반환 확인
        assertEquals(10L, reportId);
    }

    @Test
    @DisplayName("스터디 종료 실패 - 진행 중인 세션 없음")
    void endStudy_NoSession() {
        // given
        Long roomId = 100L;
        String nickname = "tester";
        when(roomMapper.findUserIdByNickname(nickname)).thenReturn(Optional.of(1L));
        when(gameMapper.findCurrentSessionId(roomId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () -> studyService.endStudy(roomId, nickname));
        
        // 검증: 이후 로직은 실행되면 안 됨
        verify(gameMapper, never()).updateGameSession(anyLong(), anyInt(), anyString());
        verify(roomMapper, never()).updateRoomStatus(anyLong(), anyString());
    }

    @Test
    @DisplayName("현재 공부 상태 조회 테스트 - 진행 중인 세션 있음")
    void getCurrentState_InStudy() {
        // given
        String nickname = "tester";
        Long userId = 1L;
        Long roomId = 100L;
        Long sessionId = 500L;
        LocalDateTime now = LocalDateTime.now();

        when(roomMapper.findUserIdByNickname(nickname)).thenReturn(Optional.of(userId));
        when(roomMapper.findActiveRoomIdByUserId(userId)).thenReturn(Optional.of(roomId));
        
        RoomRequestDto room = RoomRequestDto.builder().title("열공방").build();
        when(roomMapper.findRoomById(roomId)).thenReturn(Optional.of(room));
        when(gameMapper.findCurrentSessionId(roomId)).thenReturn(Optional.of(sessionId));
        when(gameMapper.findCurrentSessionStartTime(roomId)).thenReturn(Optional.of(now));

        // when
        StudyStateResponseDto response = studyService.getCurrentState(nickname);

        // then
        assertTrue(response.isInStudy());
        assertEquals(roomId, response.getRoomId());
        assertEquals(sessionId, response.getSessionId());
        assertEquals("열공방", response.getTitle());
        assertEquals(now, response.getStartTime());
    }

    @Test
    @DisplayName("현재 공부 상태 조회 테스트 - 진행 중인 세션 없음")
    void getCurrentState_NotInStudy() {
        // given
        String nickname = "tester";
        Long userId = 1L;

        when(roomMapper.findUserIdByNickname(nickname)).thenReturn(Optional.of(userId));
        when(roomMapper.findActiveRoomIdByUserId(userId)).thenReturn(Optional.empty());

        // when
        StudyStateResponseDto response = studyService.getCurrentState(nickname);

        // then
        assertFalse(response.isInStudy());
        assertNull(response.getRoomId());
        assertNull(response.getSessionId());
    }

    @Test
    @DisplayName("집중도 로그 기록 및 브로드캐스팅 테스트")
    void addFocusLog_Success() {
        // given
        Long sessionId = 500L;
        String nickname = "tester";
        Double score = 85.5;
        when(roomMapper.findUserIdByNickname(nickname)).thenReturn(Optional.of(1L));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        studyService.addFocusLog(sessionId, nickname, score);

        // then
        verify(focusMapper).upsertFocusLog(eq(sessionId), eq(1L), anyString());
        verify(redisPublisher).publish(eq(channelTopic), any(LocationMessageDto.class));
        verify(valueOperations).set(anyString(), eq("alive"), eq(2L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("하트비트 갱신 테스트")
    void heartbeat_Success() {
        // given
        Long sessionId = 500L;
        String nickname = "tester";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        studyService.heartbeat(sessionId, nickname);

        // then
        String expectedKey = "study:heartbeat:500:tester";
        verify(valueOperations).set(eq(expectedKey), eq("alive"), eq(2L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("도착 인증 성공 테스트 (10m 이내)")
    void verifyArrival_Success() {
        // given
        Long roomId = 100L;
        String nickname = "tester";
        Double targetLat = 37.5;
        Double targetLng = 127.0;
        
        RoomRequestDto room = RoomRequestDto.builder()
                .roomId(roomId)
                .targetLat(targetLat)
                .targetLng(targetLng)
                .build();

        when(roomMapper.findRoomById(roomId)).thenReturn(Optional.of(room));
        when(geometryUtil.createPoint(targetLat, targetLng)).thenReturn(mock(Point.class));
        when(geometryUtil.isNear(any(), anyDouble(), anyDouble(), anyDouble())).thenReturn(true);
        when(roomMapper.findUserIdByNickname(nickname)).thenReturn(Optional.of(1L));

        // when
        boolean result = studyService.verifyArrival(roomId, nickname, 37.50001, 127.00001);

        // then
        assertTrue(result);
        verify(roomMapper).updateParticipantStatus(roomId, 1L, ParticipantStatus.ARRIVED);
    }

    @Test
    @DisplayName("도착 인증 실패 테스트 (거리 멂)")
    void verifyArrival_Fail() {
        // given
        Long roomId = 100L;
        String nickname = "tester";
        Double targetLat = 37.5;
        Double targetLng = 127.0;
        
        RoomRequestDto room = RoomRequestDto.builder()
                .roomId(roomId)
                .targetLat(targetLat)
                .targetLng(targetLng)
                .build();

        when(roomMapper.findRoomById(roomId)).thenReturn(Optional.of(room));
        when(geometryUtil.createPoint(targetLat, targetLng)).thenReturn(mock(Point.class));
        when(geometryUtil.isNear(any(), anyDouble(), anyDouble(), anyDouble())).thenReturn(false);

        // when
        boolean result = studyService.verifyArrival(roomId, nickname, 37.6, 127.1);

        // then
        assertFalse(result);
        verify(roomMapper, never()).updateParticipantStatus(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("태그 시 세션이 없으면 새로 생성")
    void tagUser_CreateSession() {
        // given
        Long roomId = 100L;
        String actor = "actor";
        String target = "target";
        
        when(roomMapper.findUserIdByNickname(actor)).thenReturn(Optional.of(1L));
        when(roomMapper.findUserIdByNickname(target)).thenReturn(Optional.of(2L));
        
        when(gameMapper.findCurrentSessionId(roomId))
                .thenReturn(Optional.empty()) 
                .thenReturn(Optional.of(500L));

        // when
        Long sessionId = studyService.tagUser(roomId, actor, target);

        // then
        assertEquals(500L, sessionId);
        verify(gameMapper).createGameSession(roomId);
        verify(roomMapper).updateRoomStatus(roomId, "PLAYING");
        verify(gameMapper).insertActionLog(eq(500L), eq(1L), eq(2L), eq("TAG"));
        verify(roomMapper, times(2)).updateParticipantStatus(eq(roomId), anyLong(), eq(ParticipantStatus.IN_GAME));
    }

    @Test
    @DisplayName("태그 시 이미 세션이 있으면 기존 세션 ID 반환")
    void tagUser_ExistingSession() {
        // given
        Long roomId = 100L;
        String actor = "actor";
        String target = "target";
        
        when(roomMapper.findUserIdByNickname(actor)).thenReturn(Optional.of(1L));
        when(roomMapper.findUserIdByNickname(target)).thenReturn(Optional.of(2L));
        
        when(gameMapper.findCurrentSessionId(roomId)).thenReturn(Optional.of(500L));

        // when
        Long sessionId = studyService.tagUser(roomId, actor, target);

        // then
        assertEquals(500L, sessionId);
        verify(gameMapper, never()).createGameSession(roomId); 
        verify(gameMapper).insertActionLog(eq(500L), eq(1L), eq(2L), eq("TAG"));
    }
}