package com.ssafy.s14p11c204.server.domain.chat.api;

import com.ssafy.s14p11c204.server.domain.chat.dto.ChatMessageDto;
import com.ssafy.s14p11c204.server.domain.chat.dto.MessageType;
import com.ssafy.s14p11c204.server.domain.chat.service.ChatService;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import com.ssafy.s14p11c204.server.global.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.ssafy.s14p11c204.server.domain.user.DefaultUsers.FRANK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(com.ssafy.s14p11c204.server.global.config.TestcontainersConfiguration.class)
class ChatControllerTest {

    private static class TestStompSessionHandler extends StompSessionHandlerAdapter {
        private final BlockingQueue<Throwable> failures = new LinkedBlockingQueue<>();
        private StompSession session;

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("--- STOMP Session Connected ---");
            this.session = session;
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            System.err.println("--- STOMP Handle Exception ---");
            exception.printStackTrace();
            failures.add(exception);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("--- STOMP Transport Error ---");
            exception.printStackTrace();
            failures.add(exception);
        }

        public StompSession getSession() {
            return session;
        }

        public Throwable getFailure() throws InterruptedException {
            return failures.poll(5, TimeUnit.SECONDS);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private WebSocketStompClient stompClient;
    private String URL;

    @BeforeEach
    void setUp() {
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        this.stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        this.URL = String.format("ws://localhost:%d/ws-stomp", port);

        given(userDetailsService.loadUserByUsername(FRANK.getUsername())).willReturn(FRANK);
    }

    @Test
    @DisplayName("인증된 사용자가 보낸 채팅 메시지는 Principal 기반으로 SenderId가 설정되어야 한다")
    void webSocketChatMessage_SetsSenderIdFromPrincipal() throws Exception {
        // Given
        Long roomId = 1L;
        // Malicious DTO with a different senderId
        ChatMessageDto maliciousMessage = ChatMessageDto.builder()
                .roomId(roomId)
                .senderId(999L) // Spoofed sender ID
                .senderNickname("Mallory")
                .message("Hello from Frank!")
                .type(MessageType.TALK)
                .build();

        // Create a real token for Frank
        CurrentUser frankUser = FRANK;
        String token = jwtTokenProvider.createAccessToken(frankUser);

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        // When
        TestStompSessionHandler sessionHandler = new TestStompSessionHandler();
        StompSession stompSession = stompClient.connectAsync(URL, (WebSocketHttpHeaders) null, connectHeaders, sessionHandler).get(5, TimeUnit.SECONDS);

        if (sessionHandler.getFailure() != null) {
            throw new Exception("STOMP connection failed: " + sessionHandler.getFailure().getMessage(), sessionHandler.getFailure());
        }

        stompSession.send("/pub/chat/message", maliciousMessage);

        // Then
        // 처리는 0.5초 이내에 완료되어야 함 (게임 수준의 실시간 성능 검증)
        ArgumentCaptor<ChatMessageDto> messageCaptor = ArgumentCaptor.forClass(ChatMessageDto.class);
        verify(chatService, timeout(500)).saveMessage(messageCaptor.capture());

        ChatMessageDto capturedMessage = messageCaptor.getValue();

        // Assert that the senderId was overwritten by the principal's ID, not the spoofed one
        assertEquals(FRANK.id(), capturedMessage.getSenderId());
        assertEquals(FRANK.nickname(), capturedMessage.getSenderNickname());
        assertEquals("Hello from Frank!", capturedMessage.getMessage());
    }
}
