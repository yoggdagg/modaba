package com.ssafy.s14p11c204.server.domain.game.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface GameDao {
    /**
     * @param roomId 게임 시작을 원하는 방 id
     * @return 게임 시작 여부
     */
    int start(long roomId);

    /**
     * @param roomId 게임 종료를 원하는 방 id
     * @return 게임 종료 여부
     */
    int terminate(long roomId);

    /**
     *
     * @param roomId 현재 게임이 진행중인 방 id
     * @param taggedPlayerInfo 휴대폰으로 터치한 사람 정보
     * @return 해당 정보와 매치되는 플레이어의 id
     */
    Optional<Long> tag (long roomId, long taggedPlayerInfo);

    /**
     *
     * @param roomId 현재 게임이 진행중인 방 id
     * @param userId 이 정보를 던지는 유저 id
     * @param latitude 위도
     * @param longitude 경도
     * @return 반영여부? 일단 함수 변경이 일어나는 것보단 나으니까~
     */
    int updatePosition(long roomId, long userId, double latitude, double longitude);

    /**
     *
     * @param roomId 현재 게임이 진행중인 방 id
     * @param userId 투옥당할 유저 id
     * @return 반영 여부? 일단 함수 변경이 일어나는 것보단 나으니까~
     */
    int imprison(long roomId, long userId);

    /**
     *
     * @param roomId 현재 게임이 진행중인 방 id
     * @param userId 탈옥할 유저 id
     * @return 반영 여부? 일단 함수 변경이 일어나는 것보단 나으니까~
     */
    int unimprison(long roomId, long userId);
}
