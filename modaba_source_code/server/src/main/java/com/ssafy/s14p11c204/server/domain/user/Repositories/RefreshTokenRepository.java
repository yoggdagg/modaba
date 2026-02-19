package com.ssafy.s14p11c204.server.domain.user.Repositories;

import com.ssafy.s14p11c204.server.global.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class RefreshTokenRepository {
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;

    /**
     * 토큰 저장
     * key: 사용자 이메일 (또는 ID)
     * value: Refresh Token 값
     */
    public void save(String email, String refreshToken) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();

        // set(Key, Value, 유효기간) -> 유효기간 지나면 Redis가 알아서 삭제해줌 (개꿀!)
        values.set(email, refreshToken, jwtProperties.getRefreshTokenValidityInSeconds());
    }

    /**
     * 토큰 조회
     */
    public Optional<String> findByEmail(String email) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        String token = values.get(email);
        return Optional.ofNullable(token);
    }

    /**
     * 토큰 삭제 (로그아웃 시)
     */
    public void delete(String email) {
        redisTemplate.delete(email);
    }

    /**
     * 토큰 존재 여부 확인
     */
    public boolean hasKey(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(email));
    }
}