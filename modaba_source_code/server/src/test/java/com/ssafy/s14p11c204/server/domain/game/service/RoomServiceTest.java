package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomRequestDto;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomMapper roomMapper;

    private String hostNickname = "테스트방장";
    private String userNickname = "테스트참가자";

    @BeforeEach
    void setUp() {
        // 테스트용 지역 및 유저 생성
        roomMapper.insertTestRegion(960, "광주광역시", "북구", "용봉동");
        roomService.createTestUser("host@test.com", hostNickname);
        roomService.createTestUser("user@test.com", userNickname);
    }

    @Test
    @DisplayName("방 생성 성공 테스트")
    void createRoom() {
        // given
        RoomRequestDto request = RoomRequestDto.builder()
                .title("테스트 방")
                .roomType("KYUNGDO")
                .maxUser(4)
                .appointmentTime(LocalDateTime.now().plusHours(2))
                .placeName("서울역")
                .targetLat(37.5547)
                .targetLng(126.9707)
                .regionId(960)
                .build();

        // when
        roomService.createRoom(request, hostNickname);

        // then
        List<RoomResponseDto> myRooms = roomService.findMyRooms(hostNickname);
        assertEquals(1, myRooms.size());
        assertEquals("테스트 방", myRooms.get(0).getTitle());
        assertEquals(hostNickname, myRooms.get(0).getHostNickname());
    }

    @Test
    @DisplayName("방 참가 성공 테스트")
    void joinRoom() {
        // given
        RoomRequestDto request = RoomRequestDto.builder()
                .title("참가 테스트 방")
                .roomType("KYUNGDO")
                .maxUser(4)
                .regionId(960)
                .build();
        roomService.createRoom(request, hostNickname);
        Long roomId = request.getRoomId();

        // when
        roomService.joinRoom(roomId, userNickname);

        // then
        List<RoomResponseDto> myRooms = roomService.findMyRooms(userNickname);
        assertEquals(1, myRooms.size());
        assertEquals(roomId, myRooms.get(0).getRoomId());
        
        int count = roomMapper.countParticipants(roomId);
        assertEquals(2, count); // 방장 + 참가자
    }

    @Test
    @DisplayName("중복 참가 예외 테스트")
    void joinRoom_Duplicate() {
        // given
        RoomRequestDto request = RoomRequestDto.builder()
                .title("중복 참가 방")
                .roomType("KYUNGDO")
                .maxUser(4)
                .regionId(960)
                .build();
        roomService.createRoom(request, hostNickname);
        Long roomId = request.getRoomId();

        // when & then
        // 방장은 이미 참가 상태이므로 다시 참가하면 예외 발생
        assertThrows(IllegalStateException.class, () -> roomService.joinRoom(roomId, hostNickname));
    }

    @Test
    @DisplayName("방 퇴장 테스트")
    void leaveRoom() {
        // given
        RoomRequestDto request = RoomRequestDto.builder()
                .title("퇴장 테스트 방")
                .roomType("KYUNGDO")
                .maxUser(4)
                .regionId(960)
                .build();
        roomService.createRoom(request, hostNickname);
        Long roomId = request.getRoomId();
        roomService.joinRoom(roomId, userNickname);

        // when
        roomService.leaveRoom(roomId, userNickname);

        // then
        List<RoomResponseDto> myRooms = roomService.findMyRooms(userNickname);
        assertTrue(myRooms.isEmpty());

        int count = roomMapper.countParticipants(roomId);
        assertEquals(1, count); // 방장만 남음
    }

    @Test
    @DisplayName("위치 공유 수락 테스트")
    void updateLocationSharing() {
        // given
        RoomRequestDto request = RoomRequestDto.builder()
                .title("위치 공유 방")
                .roomType("FOCUS")
                .maxUser(4)
                .regionId(960)
                .build();
        roomService.createRoom(request, hostNickname);
        Long roomId = request.getRoomId();

        // when
        roomService.updateLocationSharing(roomId, hostNickname, true);

        // then
        // DB를 직접 조회해서 확인하는 로직이 필요하지만, 
        // 현재는 예외 없이 실행되는지 확인하는 것으로 대체
        // (실제 검증을 위해서는 RoomMapper에 조회 메서드가 필요함)
        assertDoesNotThrow(() -> roomService.updateLocationSharing(roomId, hostNickname, false));
    }
}

