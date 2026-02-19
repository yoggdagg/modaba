package com.ssafy.s14p11c204.server.domain.game.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ssafy.s14p11c204.server.domain.game.ParticipantStatus;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.GameStartResponseDto;
import com.ssafy.s14p11c204.server.domain.game.dto.LocationMessageDto;
import com.ssafy.s14p11c204.server.domain.game.dto.PlayerRole;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomRequestDto;
import com.ssafy.s14p11c204.server.domain.game.service.RoomService;
import com.ssafy.s14p11c204.server.global.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LocationControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private WebSocketStompClient stompClient;
    private String user1Token;
    private String user2Token;
    private Long roomId;
    private String user1Nickname = "유저1";
    private String user2Nickname = "유저2";

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);

        // 데이터 초기화 (순서 중요)
        try {
            jdbcTemplate.execute("DELETE FROM room_participants");
            jdbcTemplate.execute("DELETE FROM rooms");
            jdbcTemplate.execute("DELETE FROM users");
            jdbcTemplate.execute("DELETE FROM regions");
        } catch (Exception e) {
            System.err.println("Data cleanup failed: " + e.getMessage());
        }

        try {
            roomMapper.insertTestRegion(960, "광주광역시", "북구", "용봉동");
        } catch (Exception e) {
            System.err.println("Region insertion failed (might already exist): " + e.getMessage());
        }

        roomService.createTestUser("user1@test.com", user1Nickname);
        roomService.createTestUser("user2@test.com", user2Nickname);

        user1Token = jwtTokenProvider.createAccessToken(userDetailsService.loadUserByUsername("user1@test.com"));
        user2Token = jwtTokenProvider.createAccessToken(userDetailsService.loadUserByUsername("user2@test.com"));

        RoomRequestDto request = RoomRequestDto.builder()
                .title("추격전 테스트")
                .roomType("KYUNGDO")
                .maxUser(2)
                .appointmentTime(LocalDateTime.now().plusHours(1))
                .placeName("운동장")
                .targetLat(37.0)
                .targetLng(127.0)
                .regionId(960)
                .build();
        roomService.createRoom(request, user1Nickname);
        roomId = request.getRoomId();
        roomService.joinRoom(roomId, user2Nickname);
    }

    @Test
    @DisplayName("위치 공유 및 검거 테스트")
    void locationAndArrestTest() throws Exception {
        // 1. 실제 배정된 역할 확인
        List<GameStartResponseDto.ParticipantInfo> participants = roomMapper.findParticipantsWithNickname(roomId);
        String policeToken = null;
        String thiefToken = null;
        String policeNickname = null;
        String thiefNickname = null;

        for (var p : participants) {
            if (p.getRole() == PlayerRole.POLICE) {
                policeNickname = p.getNickname();
                policeToken = policeNickname.equals(user1Nickname) ? user1Token : user2Token;
            } else {
                thiefNickname = p.getNickname();
                thiefToken = thiefNickname.equals(user1Nickname) ? user1Token : user2Token;
            }
        }

        StompSession policeSession = connect(policeToken);
        StompSession thiefSession = connect(thiefToken);
        BlockingQueue<LocationMessageDto> policeQueue = new LinkedBlockingQueue<>();
        
        policeSession.subscribe("/user/sub/game/" + roomId + "/location", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return LocationMessageDto.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) { policeQueue.offer((LocationMessageDto) payload); }
        });

        Thread.sleep(1000);

        // 2. [도둑] 위치 전송
        LocationMessageDto thiefLoc = LocationMessageDto.builder()
                .type(LocationMessageDto.MessageType.LOCATION)
                .role(PlayerRole.THIEF)
                .lat(35.123456)
                .lng(126.123456)
                .build();
        thiefSession.send("/pub/room/" + roomId + "/location", thiefLoc);

        // 3. [경찰] 도둑 위치 수신 확인
        LocationMessageDto receivedLoc = policeQueue.poll(15, TimeUnit.SECONDS);
        assertThat(receivedLoc).isNotNull();
        assertThat(receivedLoc.getSenderNickname()).isEqualTo(thiefNickname);
        
        // 4. [경찰] 도둑 근처로 이동 및 위치 전송
        LocationMessageDto policeLoc = LocationMessageDto.builder()
                .type(LocationMessageDto.MessageType.LOCATION)
                .role(PlayerRole.POLICE)
                .lat(35.123456)
                .lng(126.123456)
                .build();
        policeSession.send("/pub/room/" + roomId + "/location", policeLoc);
        policeQueue.poll(15, TimeUnit.SECONDS); // 본인 위치 업데이트 무시

        // 5. [경찰] 검거 요청 전송
        LocationMessageDto arrestReq = LocationMessageDto.builder()
                .type(LocationMessageDto.MessageType.ARREST_REQUEST)
                .targetNickname(thiefNickname)
                .build();
        policeSession.send("/pub/room/" + roomId + "/location", arrestReq);

        // 6. [경찰] 검거 결과 수신 확인
        LocationMessageDto arrestResult = policeQueue.poll(15, TimeUnit.SECONDS);
        assertThat(arrestResult).isNotNull();
        assertThat(arrestResult.getType()).isEqualTo(LocationMessageDto.MessageType.ARREST_RESULT);
        assertThat(arrestResult.getSuccess()).isTrue();

        // 7. [DB 검증] 도둑의 상태가 ARRESTED로 변경되었는지 확인
        Thread.sleep(1000);
        Long thiefId = roomMapper.findUserIdByNickname(thiefNickname).orElseThrow();
        ParticipantStatus status = roomMapper.findParticipantStatus(roomId, thiefId);
        assertThat(status).isEqualTo(ParticipantStatus.ARRESTED);
    }

    @Test
    @DisplayName("팀별 위치 공유 테스트")
    void teamLocationSharingTest() throws Exception {
        List<GameStartResponseDto.ParticipantInfo> participants = roomMapper.findParticipantsWithNickname(roomId);
        String policeToken = null;
        String thiefToken = null;
        
        for (var p : participants) {
            if (p.getRole() == PlayerRole.POLICE) {
                policeToken = p.getNickname().equals(user1Nickname) ? user1Token : user2Token;
            } else {
                thiefToken = p.getNickname().equals(user1Nickname) ? user1Token : user2Token;
            }
        }

        StompSession policeSession = connect(policeToken);
        StompSession thiefSession = connect(thiefToken);

        BlockingQueue<LocationMessageDto> policeQueue = new LinkedBlockingQueue<>();
        BlockingQueue<LocationMessageDto> thiefQueue = new LinkedBlockingQueue<>();

        policeSession.subscribe("/user/sub/game/" + roomId + "/location", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return LocationMessageDto.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) { policeQueue.offer((LocationMessageDto) payload); }
        });

        thiefSession.subscribe("/user/sub/game/" + roomId + "/location", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return LocationMessageDto.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) { thiefQueue.offer((LocationMessageDto) payload); }
        });

        Thread.sleep(1000);

        // [경찰] 위치 전송 -> 도둑은 못 받아야 함
        LocationMessageDto policeLoc = LocationMessageDto.builder()
                .type(LocationMessageDto.MessageType.LOCATION)
                .role(PlayerRole.POLICE)
                .lat(37.123)
                .lng(127.123)
                .build();
        policeSession.send("/pub/room/" + roomId + "/location", policeLoc);

        assertThat(policeQueue.poll(15, TimeUnit.SECONDS)).isNotNull();
        assertThat(thiefQueue.poll(2, TimeUnit.SECONDS)).isNull(); // 도둑은 경찰 위치 차단 확인

        // [도둑] 위치 전송 -> 경찰과 도둑 모두 받아야 함
        LocationMessageDto thiefLoc = LocationMessageDto.builder()
                .type(LocationMessageDto.MessageType.LOCATION)
                .role(PlayerRole.THIEF)
                .lat(37.456)
                .lng(127.456)
                .build();
        thiefSession.send("/pub/room/" + roomId + "/location", thiefLoc);

        assertThat(policeQueue.poll(15, TimeUnit.SECONDS)).isNotNull();
        assertThat(thiefQueue.poll(15, TimeUnit.SECONDS)).isNotNull();
    }

    private StompSession connect(String token) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.add("Authorization", "Bearer " + token);

        return stompClient.connectAsync(
                "ws://localhost:" + port + "/ws-stomp",
                headers,
                stompHeaders,
                new StompSessionHandlerAdapter() {}
        ).get(15, TimeUnit.SECONDS);
    }
}
