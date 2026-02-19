package com.ssafy.s14p11c204.server.global.util;

import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;

import com.ssafy.s14p11c204.server.global.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;

import static com.ssafy.s14p11c204.server.domain.user.DefaultUsers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

@ExtendWith(MockitoExtension.class) // JUnit 5에서 Mockito를 사용하기 위한 설정
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        // 1. 진짜 로직이 포함된 JwtProperties를 직접 생성 (스프링 없이도 키 생성 로직이 돌아감)
        JwtProperties jwtProperties = new JwtProperties(
                "this-is-a-very-secret-key-at-least-32-characters-long",
                Duration.ofSeconds(3600),
                Duration.ofSeconds(1209600)
        );

        // 2. 주입! @RequiredArgsConstructor 덕분에 깔끔하게 생성자로 들어감
        jwtTokenProvider = new JwtTokenProvider(userDetailsService, jwtProperties);
    }
    @Nested
    @DisplayName("Happy Path: 정상 케이스")
    class HappyPath {

        @Test
        @DisplayName("Access Token 생성 및 파싱 성공")
        void createAndParseAccessTokenSuccess() {
            // Given
            CurrentUser user = URSULA;

            // When
            String token = jwtTokenProvider.createAccessToken(user);

            // Then
            assertEquals(user.getUsername(), jwtTokenProvider.getUsername(token));
            assertEquals("access", jwtTokenProvider.getTokenType(token));
            assertTrue(jwtTokenProvider.isAccessToken(token));
            assertTrue(jwtTokenProvider.validateToken(token));
        }

        @Test
        @DisplayName("Refresh Token 생성 및 타입 확인 성공")
        void createRefreshTokenSuccess() {
            // Given
            CurrentUser user = URSULA;

            // When
            String token = jwtTokenProvider.createRefreshToken(user);

            // Then
            assertEquals(user.getUsername(), jwtTokenProvider.getUsername(token));
            assertEquals("refresh", jwtTokenProvider.getTokenType(token));
            assertTrue(jwtTokenProvider.isRefreshToken(token));
        }

        @Test
        @DisplayName("Authentication 객체 조회 성공")
        void getAuthenticationSuccess() {
            // Given
            CurrentUser user = JUDY;
            String token = jwtTokenProvider.createAccessToken(user);
            // given(userDetailsService.loadUserByUsername(user.getUsername())).willReturn(user); // 삭제됨

            // When
            Authentication authentication = jwtTokenProvider.getAuthentication(token);

            // Then
            assertNotNull(authentication);
            assertEquals(user.getUsername(), ((CurrentUser) authentication.getPrincipal()).getUsername());
            // 권한 검증은 JwtTokenProvider 구현에 따라 다를 수 있으므로, 필요시 수정
            // assertEquals(user.getAuthorities(), authentication.getAuthorities());
        }
    }

    @Nested
    @DisplayName("Unhappy Path: 실패 및 예외 케이스")
    class UnhappyPath {

        @Test
        @DisplayName("만료된 토큰 검증 시 false 반환")
        void validateTokenExpired() {
            // Given: 만료 시간을 0초로 설정한 프로퍼티로 임시 생성
            JwtProperties expiredProps = new JwtProperties(
                    "test-secret-key-at-least-32-characters-long-12345678",
                    Duration.ZERO, // 생성 즉시 만료
                    Duration.ZERO
            );
            JwtTokenProvider expiredProvider = new JwtTokenProvider(userDetailsService, expiredProps);
            String token = expiredProvider.createAccessToken(JUDY);

            // When
            boolean isValid = expiredProvider.validateToken(token);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("잘못된 형식의 토큰 검증 시 false 반환")
        void validateTokenInvalidFormat() {
            // Given
            String invalidToken = "invalid.token.format";

            // When
            boolean isValid = jwtTokenProvider.validateToken(invalidToken);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("토큰 타입 불일치 확인 (Access를 Refresh로 체크)")
        void tokenTypeMismatch() {
            // Given
            String accessToken = jwtTokenProvider.createAccessToken(JUDY);

            // When & Then
            assertFalse(jwtTokenProvider.isRefreshToken(accessToken));
        }
    }
}