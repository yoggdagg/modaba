package com.ssafy.s14p11c204.server.domain.user.Repositories;

import com.ssafy.s14p11c204.server.global.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Repository
public class RedisRepository {
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;

    public Object opsForValue() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'opsForValue'");
    }

    public void delete(String email) {
        redisTemplate.delete(email);
    }

    public void save(String email, String code, long time, TimeUnit timeType) {
        redisTemplate.opsForValue().set(
                email,
                code,
                time,
                timeType);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}