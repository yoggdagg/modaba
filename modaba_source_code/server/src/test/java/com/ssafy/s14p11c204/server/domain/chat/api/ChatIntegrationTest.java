package com.ssafy.s14p11c204.server.domain.chat.api;

import com.ssafy.s14p11c204.server.domain.chat.dao.ChatDao;
import com.ssafy.s14p11c204.server.domain.chat.dto.ChatMessageDto;
import com.ssafy.s14p11c204.server.domain.chat.dto.MessageType;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import com.ssafy.s14p11c204.server.global.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.ssafy.s14p11c204.server.domain.user.DefaultUsers.FRANK;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import static org.mockito.BDDMockito.given;

// @Transactional을 사용하지 않습니다.
// 이유: 통합 테스트(RANDOM_PORT) 환경에서 별도의 스레드(WebSocket/STOMP 서버)가 DB에 접근합니다.
// @Transactional을 사용하면 테스트 메서드의 트랜잭션이 커밋되지 않아, 서버 스레드에서 데이터를 볼 수 없습니다 (Isolation 문제).
// 따라서 데이터를 수동으로 생성하고 @AfterEach에서 수동으로 정리(tearDown)해야 합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(com.ssafy.s14p11c204.server.global.config.TestcontainersConfiguration.class)
class ChatIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ChatDao chatDao;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    @Autowired
    private javax.sql.DataSource dataSource;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private WebSocketStompClient stompClient;
    private String URL;
    private Long testRoomId = 1L;

    @BeforeEach
    void setUp() {
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        this.stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        this.URL = String.format("ws://localhost:%d/ws-stomp", port);

        given(userDetailsService.loadUserByUsername(FRANK.getUsername())).willReturn(FRANK);
        
        // Ensure user exists (Frank, ID 6)
        jdbcTemplate.execute("INSERT INTO users (user_id, email, nickname, password, role) " +
                "VALUES (6, 'frank@ssafy.com', 'Frank', 'pass1234', 'USER') " +
                "ON CONFLICT (user_id) DO NOTHING");

        jdbcTemplate.execute("INSERT INTO rooms (room_id, host_id, type, title) " +
                "VALUES (1, 6, 'KYUNGDO'::room_type, 'Test Room') " +
                "ON CONFLICT (room_id) DO NOTHING");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM chat_logs");
        jdbcTemplate.execute("DELETE FROM rooms WHERE room_id = 1");
        jdbcTemplate.execute("DELETE FROM users WHERE user_id = 6");
    }

    @Test
    @DisplayName("일반 채팅(TALK) 메시지는 DB에 저장되어야 한다")
    void talkMessage_IsSavedToDatabase() throws Exception {
        // Given
        ChatMessageDto talkMessage = ChatMessageDto.builder()
                .roomId(testRoomId)
                .type(MessageType.TALK)
                .message("Hello DB!")
                .build();

        String token = jwtTokenProvider.createAccessToken(FRANK);
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        // When
        StompSession stompSession = stompClient.connectAsync(URL, (WebSocketHttpHeaders) null, connectHeaders, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        stompSession.send("/pub/chat/message", talkMessage);

        // Then - DB 확인 (비동기 처리를 위해 대기)
        Thread.sleep(1500); 

        // MyBatis 대신 JdbcTemplate으로 직접 확인해봄
        List<String> rawMessages = jdbcTemplate.queryForList("SELECT message FROM chat_logs WHERE room_id = ?", String.class, testRoomId);
        System.out.println("--- Found messages in DB: " + rawMessages);
        
        assertFalse(rawMessages.isEmpty(), "메시지가 DB에 저장되어야 합니다.");
        assertEquals("Hello DB!", rawMessages.get(0));

        // 기존 MyBatis 호출도 검증 (여기서 에러가 나는지 확인)
        List<ChatMessageDto> messages = chatDao.findMessages(testRoomId, null, 10, null);
        assertNotNull(messages);
    }

    @Test
    @DisplayName("보이스 시그널링(OFFER)은 지정된 수신자(recipientId)에게만 전송되어야 한다")
    void offerMessage_IsRoutedToRecipient() throws Exception {
        // Given
        Long recipientId = FRANK.id(); // 본인에게 전송 테스트
        ChatMessageDto offerMessage = ChatMessageDto.builder()
                .roomId(testRoomId)
                .type(MessageType.OFFER)
                .recipientId(recipientId)
                .message("v=0\r\no=- 472389...")
                .build();

        String token = jwtTokenProvider.createAccessToken(FRANK);
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        // When
        TestStompSessionHandler sessionHandler = new TestStompSessionHandler();
        StompSession stompSession = stompClient.connectAsync(URL, (WebSocketHttpHeaders) null, connectHeaders, sessionHandler).get(5, TimeUnit.SECONDS);

        // 구독: 개인 채널 (/sub/chat/user/{userId})
        stompSession.subscribe("/sub/chat/user/" + recipientId, sessionHandler);
        
        // 메시지 전송
        stompSession.send("/pub/chat/message", offerMessage);

        // Then
        ChatMessageDto received = sessionHandler.getReceivedMessage(3, TimeUnit.SECONDS);
        assertNotNull(received, "개인 채널로 메시지를 받아야 합니다.");
        assertEquals(MessageType.OFFER, received.getType());
        assertEquals("v=0\r\no=- 472389...", received.getMessage());
        
        // DB 확인 - 저장 안 되어야 함
        Thread.sleep(1000);
        List<ChatMessageDto> messages = chatDao.findMessages(testRoomId, null, 50, null);
        boolean hasOffer = messages.stream().anyMatch(m -> m.getType() == MessageType.OFFER);
        assertFalse(hasOffer, "OFFER 메시지는 DB에 저장되지 않아야 합니다.");
    }
}

// Helper class for handling STOMP messages in tests
class TestStompSessionHandler extends StompSessionHandlerAdapter {
    private final java.util.concurrent.BlockingQueue<ChatMessageDto> messages = new java.util.concurrent.LinkedBlockingQueue<>();

    @Override
    public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
        return ChatMessageDto.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        messages.add((ChatMessageDto) payload);
    }

    public ChatMessageDto getReceivedMessage(long timeout, TimeUnit unit) throws InterruptedException {
        return messages.poll(timeout, unit);
    }
}
