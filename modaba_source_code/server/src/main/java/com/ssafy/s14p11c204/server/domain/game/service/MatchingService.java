package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper;
import com.ssafy.s14p11c204.server.domain.game.dto.RoomResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final RoomMapper roomMapper;

    @Transactional(readOnly = true)
    public List<RoomResponseDto> findMatch(String nickname) {
        Long userId = roomMapper.findUserIdByNickname(nickname).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다: " + nickname));
        int userMmr = roomMapper.findUserMmr(userId).orElse(1000);
        // 1. 내 MMR 기준 ±100 범위의 방 검색
        int range = 100;
        List<RoomResponseDto> rooms = roomMapper.findRoomsByMmr(userMmr - range, userMmr + range);
        // 2. (선택) 방이 없으면 범위를 넓혀서 재검색 (예: ±200)
        if (rooms.isEmpty()) {
            range = 200;
            rooms = roomMapper.findRoomsByMmr(userMmr - range, userMmr + range);
        }
        return rooms;
    }
}
