package com.ssafy.s14p11c204.server.domain.user.Repositories;

import java.util.List;
import java.util.Optional;

import com.ssafy.s14p11c204.server.domain.user.dto.MmrHistoryDto;
import org.apache.ibatis.annotations.Mapper;

import com.ssafy.s14p11c204.server.domain.user.User;

@Mapper
public interface UserMapper {
    boolean existsByEmail(String email);

    void signup(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findKakaoByEmail(String email);

    int update(User user);

    int updatePassword(String email, String password);

    int unregister(String email);

    Optional<User> findById(Long userId);
    
    // MMR 히스토리 조회 (추가됨)
    List<MmrHistoryDto> findMmrHistoryByUserId(Long userId);
}
