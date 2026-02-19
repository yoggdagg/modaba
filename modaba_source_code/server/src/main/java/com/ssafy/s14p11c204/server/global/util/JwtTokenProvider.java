package com.ssafy.s14p11c204.server.global.util;

import com.ssafy.s14p11c204.server.domain.user.User;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import com.ssafy.s14p11c204.server.global.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class JwtTokenProvider {
    private final UserDetailsService userDetailsService;
    private final JwtProperties jwtProperties;

    public String createAccessToken(UserDetails userDetails) {
        CurrentUser currentUser = (CurrentUser) userDetails;
        String userName = currentUser.getUsername();
        long userId = currentUser.id();
        String nickName = currentUser.nickname();
        return createToken(userName, jwtProperties.getAccessTokenValidityInSeconds(),
                Map.of(
                        "type", "access",
                        "userId", userId,
                        "nickname", nickName
                ));
    }

    public String createRefreshToken(UserDetails userDetails) {
        String userName = userDetails.getUsername();
        return createToken(userName, jwtProperties.getRefreshTokenValidityInSeconds(), Map.of("type", "refresh"));
    }

    private String createToken(String userEmail, Duration validityInSeconds, Map<String, Object> additionalClaims) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInSeconds.toMillis());

        return Jwts.builder()
                .subject(userEmail)
                .issuedAt(now)
                .expiration(validity)
                .claims(additionalClaims)
                .signWith(jwtProperties.getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String getTokenType(String token) {
        Claims claims = parseClaims(token);
        Object type = claims.get("type");
        return type != null ? type.toString() : null;
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String email = claims.getSubject();
        
        // 토큰에서 userId와 nickname 추출 (안전하게 처리)
        Long userId = claims.get("userId", Long.class);
        String nickname = claims.get("nickname", String.class);
        
        // CurrentUser 객체 직접 생성
        // userId가 null이면 0L을 할당하여 long 타입 호환성 보장
        CurrentUser currentUser = new CurrentUser(
                userId != null ? userId : 0L, 
                email,
                nickname != null ? nickname : "",
                "", // password
                User.Role.USER // role (기본값)
        );

        return new UsernamePasswordAuthenticationToken(
                currentUser,
                null,
                currentUser.getAuthorities());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtProperties.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
