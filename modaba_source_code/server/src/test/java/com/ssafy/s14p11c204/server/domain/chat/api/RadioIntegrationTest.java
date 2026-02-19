package com.ssafy.s14p11c204.server.domain.chat.api;

import com.ssafy.s14p11c204.server.domain.chat.dto.VoiceMessageDto;
import com.ssafy.s14p11c204.server.global.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.ssafy.s14p11c204.server.domain.user.DefaultUsers.FRANK;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(com.ssafy.s14p11c204.server.global.config.TestcontainersConfiguration.class)
class RadioIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private WebSocketStompClient stompClient;
    private String URL;
    private final Long roomId = 1L;

    @BeforeEach
    void setUp() {
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        this.stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        this.URL = String.format("ws://localhost:%d/ws-stomp", port);

        given(userDetailsService.loadUserByUsername(FRANK.getUsername())).willReturn(FRANK);

        try {
            // 데이터 정리 (순서 중요: 자식 -> 부모)
            // users를 참조하는 모든 테이블 정리
            jdbcTemplate.execute("DELETE FROM chat_logs");
            jdbcTemplate.execute("DELETE FROM mmr_history");
            jdbcTemplate.execute("DELETE FROM friendships");
            jdbcTemplate.execute("DELETE FROM game_sessions");
            jdbcTemplate.execute("DELETE FROM room_participants");
            jdbcTemplate.execute("DELETE FROM rooms");
            
            // users는 다른 테스트와 공유될 수 있으므로 주의, 여기서는 Frank(ID 6)만 정리
            jdbcTemplate.execute("DELETE FROM users WHERE user_id = 6");

            // 데이터 삽입
            jdbcTemplate.execute("INSERT INTO users (user_id, email, nickname, password, role) " +
                    "VALUES (6, 'frank@ssafy.com', 'Frank', 'pass1234', 'USER')");

            jdbcTemplate.execute("INSERT INTO rooms (room_id, host_id, type, title, status) " +
                    "VALUES (1, 6, 'KYUNGDO'::room_type, 'Radio Test', 'PLAYING'::room_status)");
            
            jdbcTemplate.execute("INSERT INTO room_participants (room_id, user_id, role, status) " +
                    "VALUES (1, 6, 'POLICE'::player_role, 'IN_GAME'::participant_status)");
        } catch (Exception e) {
            System.err.println("DB Setup Failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @AfterEach
    void tearDown() {
        try {
            // tearDown에서도 참조 테이블 먼저 정리
            jdbcTemplate.execute("DELETE FROM chat_logs WHERE room_id = 1");
            jdbcTemplate.execute("DELETE FROM room_participants WHERE room_id = 1");
            jdbcTemplate.execute("DELETE FROM rooms WHERE room_id = 1");
            // users는 다른 테스트에 영향을 줄 수 있으므로 조심스럽게 처리하거나 생략 가능
            // 하지만 setUp에서 다시 생성하므로 삭제해도 무방
            jdbcTemplate.execute("DELETE FROM users WHERE user_id = 6");
        } catch (Exception e) {
            System.err.println("DB Teardown Failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("P2S 무전기: 음성 데이터를 보내면 팀 채널로 중계되어야 한다")
    void voiceMessage_IsRelayedToTeamChannel() throws Exception {
        // Given
        byte[] mockAudioData = new byte[]{1, 2, 3, 4, 5};
        VoiceMessageDto sendDto = VoiceMessageDto.builder()
                .roomId(roomId)
                .audioData(mockAudioData)
                .build();

        String token = jwtTokenProvider.createAccessToken(FRANK);
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        // When
        TestVoiceHandler handler = new TestVoiceHandler();
        StompSession session = stompClient.connectAsync(URL, (WebSocketHttpHeaders) null, connectHeaders, new StompSessionHandlerAdapter() {
            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                System.err.println("STOMP Exception: " + exception.getMessage());
            }
        }).get(5, TimeUnit.SECONDS);
        
        session.subscribe("/sub/voice/team/1/POLICE", handler);
        session.send("/pub/radio/voice", sendDto);

        // Then
        VoiceMessageDto received = handler.pollMessage(10, TimeUnit.SECONDS);
        
        assertNotNull(received, "무전 데이터를 수신해야 합니다.");
        assertArrayEquals(mockAudioData, received.getAudioData());
        assertEquals("POLICE", received.getTeam());
        assertEquals(FRANK.id(), received.getSenderId());
    }

    @Test
    @DisplayName("P2S 무전기: 루프백 플래그를 설정하면 전용 루프백 채널로 수신되어야 한다")
    void voiceMessage_LoopbackWorks() throws Exception {
        // Given
        byte[] mockAudioData = new byte[]{9, 8, 7, 6};
        VoiceMessageDto sendDto = VoiceMessageDto.builder()
                .roomId(roomId)
                .audioData(mockAudioData)
                .loopback(true)
                .build();

        String token = jwtTokenProvider.createAccessToken(FRANK);
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        // When
        TestVoiceHandler handler = new TestVoiceHandler();
        StompSession session = stompClient.connectAsync(URL, (WebSocketHttpHeaders) null, connectHeaders, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);

        String loopbackDest = "/sub/voice/loopback/" + FRANK.id();
        session.subscribe(loopbackDest, handler);
        session.send("/pub/radio/voice", sendDto);

        // Then
        VoiceMessageDto received = handler.pollMessage(5, TimeUnit.SECONDS);

        assertNotNull(received, "루프백 데이터를 수신해야 합니다.");
        assertArrayEquals(mockAudioData, received.getAudioData());
        assertEquals("LOOPBACK", received.getTeam());
        assertTrue(received.isLoopback());
    }

    private static class TestVoiceHandler extends StompSessionHandlerAdapter {
        private final BlockingQueue<VoiceMessageDto> queue = new LinkedBlockingQueue<>();

        @Override
        public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
            return VoiceMessageDto.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            queue.add((VoiceMessageDto) payload);
        }

        public VoiceMessageDto pollMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return queue.poll(timeout, unit);
        }
    }
}
