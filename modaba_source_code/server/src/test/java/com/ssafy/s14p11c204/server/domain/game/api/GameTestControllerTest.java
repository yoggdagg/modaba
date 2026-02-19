package com.ssafy.s14p11c204.server.domain.game.api;

import com.ssafy.s14p11c204.server.domain.game.ParticipantStatus;
import com.ssafy.s14p11c204.server.domain.game.RoomStatus;
import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dao.GameMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.GameResultDto;
import com.ssafy.s14p11c204.server.domain.game.dto.PlayerRole;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomRequestDto;
import com.ssafy.s14p11c204.server.domain.game.service.GameTestService;
import com.ssafy.s14p11c204.server.domain.game.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class GameTestControllerTest {

    @Autowired
    private GameTestService gameTestService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private GameMapper gameMapper;

    private Long roomId;
    private final int MAX_USER = 4;

    @BeforeEach
    void setUp() {
        // 0. 테스트용 지역 생성
        roomMapper.insertTestRegion(960, "광주광역시", "북구", "용봉동");

        // 1. 유저 4명 생성
        for (int i = 1; i <= MAX_USER; i++) {
            roomService.createTestUser("user" + i + "@test.com", "유저" + i);
        }

        // 2. 방 생성 및 게임 시작
        createAndStartGame();
    }

    private void createAndStartGame() {
        RoomRequestDto request = RoomRequestDto.builder()
                .title("치트 테스트 방")
                .roomType("KYUNGDO")
                .maxUser(MAX_USER)
                .appointmentTime(LocalDateTime.now().plusHours(1))
                .placeName("운동장")
                .targetLat(37.0)
                .targetLng(127.0)
                .regionId(960)
                .build();
        roomService.createRoom(request, "유저1");
        roomId = request.getRoomId();

        roomService.joinRoom(roomId, "유저2");
        roomService.joinRoom(roomId, "유저3");
        roomService.joinRoom(roomId, "유저4"); // 자동 시작
    }

    @Test
    @Disabled("MMR 히스토리 스키마 문제로 임시 비활성화")
    @DisplayName("강제 검거 (경찰 승리) 테스트")
    void forceArrestTest() {
        // Given: 게임 진행 중 (PLAYING)
        assertThat(roomService.findMyRooms("유저1").get(0).getStatus()).isEqualTo(RoomStatus.PLAYING);

        // When: 강제 검거 API 호출 (Service 직접 호출로 대체)
        GameResultDto result = gameTestService.forceArrestAllThieves(roomId);

        // Then:
        // 1. 방 상태가 FINISHED로 변경되었는지
        assertThat(roomService.findMyRooms("유저1").get(0).getStatus()).isEqualTo(RoomStatus.FINISHED);

        // 2. 승리 팀이 POLICE인지
        assertThat(result.getWinnerTeam()).isEqualTo("POLICE");

        // 3. 도둑들의 상태가 ARRESTED로 변경되었는지 (MMR 변동 확인)
        // 수정: gameMapper.findGameParticipants 대신 result 객체 사용
        List<GameResultDto.PlayerResultDto> participants = result.getPlayerResults();
        
        for (GameResultDto.PlayerResultDto p : participants) {
            assertThat(p.getNewMmr()).isNotNull();
            assertThat(p.getOldMmr()).isNotNull();
            
            if (p.getRole() == PlayerRole.THIEF) {
                assertThat(p.getNewMmr()).isLessThanOrEqualTo(p.getOldMmr()); // 도둑은 패배했으므로 MMR 하락/유지
            } else {
                assertThat(p.getNewMmr()).isGreaterThan(p.getOldMmr()); // 경찰은 승리했으므로 MMR 상승
            }
        }
    }

    @Test
    @Disabled("MMR 히스토리 스키마 문제로 임시 비활성화")
    @DisplayName("강제 타임아웃 (도둑 승리) 테스트")
    void forceTimeoutTest() {
        // Given: 게임 진행 중
        assertThat(roomService.findMyRooms("유저1").get(0).getStatus()).isEqualTo(RoomStatus.PLAYING);

        // When: 강제 타임아웃 API 호출
        GameResultDto result = gameTestService.forceTimeout(roomId);

        // Then:
        // 1. 방 상태가 FINISHED로 변경되었는지
        assertThat(roomService.findMyRooms("유저1").get(0).getStatus()).isEqualTo(RoomStatus.FINISHED);

        // 2. 승리 팀이 THIEF인지
        assertThat(result.getWinnerTeam()).isEqualTo("THIEF");

        // 3. MMR 변동 확인
        // 수정: gameMapper.findGameParticipants 대신 result 객체 사용
        List<GameResultDto.PlayerResultDto> participants = result.getPlayerResults();
        
        for (GameResultDto.PlayerResultDto p : participants) {
            assertThat(p.getNewMmr()).isNotNull();
            assertThat(p.getOldMmr()).isNotNull();

            if (p.getRole() == PlayerRole.THIEF) {
                assertThat(p.getNewMmr()).isGreaterThan(p.getOldMmr()); // 도둑 승리 -> MMR 상승
            } else {
                assertThat(p.getNewMmr()).isLessThanOrEqualTo(p.getOldMmr()); // 경찰 패배 -> MMR 하락/유지
            }
        }
    }
}
