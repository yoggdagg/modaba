package com.ssafy.s14p11c204.server.global.properties;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.convert.DurationUnit;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Getter
@ConfigurationProperties(prefix = "spring.jwt")
public class JwtProperties {
    private final SecretKey secretKey;
    private final Duration accessTokenValidityInSeconds;
    private final Duration refreshTokenValidityInSeconds;

    @ConstructorBinding
    public JwtProperties(
            String secret,
            @DurationUnit(ChronoUnit.SECONDS) Duration accessTokenValidityInSeconds,
            @DurationUnit(ChronoUnit.SECONDS) Duration refreshTokenValidityInSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
    }
}
