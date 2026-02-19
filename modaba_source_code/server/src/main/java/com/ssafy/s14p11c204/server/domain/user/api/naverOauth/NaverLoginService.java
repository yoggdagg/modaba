package com.ssafy.s14p11c204.server.domain.user.api.naverOauth;

import com.ssafy.s14p11c204.server.domain.user.User;
import com.ssafy.s14p11c204.server.domain.user.Repositories.RefreshTokenRepository;
import com.ssafy.s14p11c204.server.domain.user.Repositories.UserMapper;
import com.ssafy.s14p11c204.server.domain.user.api.naverOauth.vo.NaverLoginProfile;
import com.ssafy.s14p11c204.server.domain.user.api.naverOauth.vo.NaverLoginProfileResponse;
import com.ssafy.s14p11c204.server.domain.user.api.naverOauth.vo.NaverLoginVo;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import com.ssafy.s14p11c204.server.domain.user.dto.LoginRequest;
import com.ssafy.s14p11c204.server.domain.user.dto.LoginResponse;
import com.ssafy.s14p11c204.server.domain.user.dto.SignupRequest;
import com.ssafy.s14p11c204.server.global.util.JwtTokenProvider;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class NaverLoginService {

        private final WebClient webClient;
        private final UserMapper userMapper;
        private final PasswordEncoder passwordEncoder;
        private final JwtTokenProvider jwtTokenProvider;
        private final RefreshTokenRepository refreshTokenRepo;

        @Value("${api.naver.client_id}")
        private String client_id;

        @Value("${api.naver.client_secret}")
        private String client_secret;

        public NaverLoginService(WebClient webClient, UserMapper userMapper, PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider, RefreshTokenRepository refreshTokenRepo) {
                this.webClient = webClient;
                this.userMapper = userMapper;
                this.passwordEncoder = passwordEncoder;
                this.jwtTokenProvider = jwtTokenProvider;
                this.refreshTokenRepo = refreshTokenRepo;
        }

        /**
         * @description Naver 로그인을 위하여 Access_tokin을 발급받음
         * @param resValue
         *                   1) code: 토큰 발급용 1회용 코드
         *                   2) state: CORS 를 방지하기 위한 임의의 토큰
         * @param grant_type
         *                   1) 발급:'authorization_code'
         *                   2) 갱신:'refresh_token'
         *                   3) 삭제: 'delete'
         * @return
         */
        public NaverLoginVo requestNaverLoginAccessToken(Map<String, String> resValue, String grant_type) {
                final String uri = UriComponentsBuilder
                                .fromUriString("https://nid.naver.com")
                                .path("/oauth2.0/token")
                                .queryParam("grant_type", grant_type)
                                .queryParam("client_id", this.client_id)
                                .queryParam("client_secret", this.client_secret)
                                .queryParam("code", resValue.get("code"))
                                .queryParam("state", resValue.get("state"))
                                .queryParam("refresh_token", resValue.get("refresh_token")) // Access_token 갱신시 사용
                                .build()
                                .encode()
                                .toUriString();

                return webClient
                                .get()
                                .uri(uri)
                                .retrieve()
                                .bodyToMono(NaverLoginVo.class)
                                .block();
        }

        // ----- 프로필 API 호출 (Unique한 id 값을 가져오기 위함) -----
        public NaverLoginProfile requestNaverLoginProfile(NaverLoginVo naverLoginVo) {
                final String profileUri = UriComponentsBuilder
                                .fromUriString("https://openapi.naver.com")
                                .path("/v1/nid/me")
                                .build()
                                .encode()
                                .toUriString();

                return webClient
                                .get()
                                .uri(profileUri)
                                .header("Authorization", "Bearer " + naverLoginVo.getAccess_token())
                                .retrieve()
                                .bodyToMono(NaverLoginProfileResponse.class)
                                .block()
                                .getResponse(); // NaverLoginProfile 은 건네준다.
        }

        public void signup(SignupRequest request) {
                // 중복 체크 로직
                if (userMapper.existsByEmail(request.email())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
                }

                User user = User.builder()
                                .email(request.email())
                                .nickname(request.nickname())
                                .password(passwordEncoder.encode(request.password()))
                                .role(User.Role.USER)
                                .provider(User.Provider.NAVER)
                                .build();

                userMapper.signup(user);
        }

        public LoginResponse login(LoginRequest request) {
                // 1. 유저 조회
                User user = userMapper.findByEmail(request.email())
                                .orElseThrow(() -> new BadCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다."));

                // 2. 비밀번호 비교
                if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                        throw new BadCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다.");
                }

                // 3. CurrentUser 생성
                UserDetails userDetails = new CurrentUser(
                                user.getId(),
                                user.getEmail(),
                                user.getNickname(),
                                user.getPassword(),
                                user.getRole());

                String accessToken;
                String refreshToken;

                // 4. 토큰 발급 단계 로그 및 예외 처리
                try {
                        accessToken = jwtTokenProvider.createAccessToken(userDetails);
                        refreshToken = jwtTokenProvider.createRefreshToken(userDetails);
                } catch (Exception e) {
                        log.error("JWT 토큰 생성 중 오류 발생 - User: {}, Error: {}", user.getEmail(), e.getMessage(), e);
                        throw new JwtException("토큰 생성에 실패했습니다.", e);
                }

                // 5. Redis 저장 단계 로그 및 예외 처리
                try {
                        refreshTokenRepo.save(userDetails.getUsername(), refreshToken);
                } catch (Exception e) {
                        log.error("Redis 리프레시 토큰 저장 실패 - User: {}, Error: {}", user.getEmail(), e.getMessage(), e);
                        throw new RedisSystemException("세션 저장소 연결 오류가 발생했습니다.", e);
                }

                return new LoginResponse(refreshToken, accessToken, user.getNickname());
        }

        @Transactional
        public void logout(String refreshToken) {
                // 1. 토큰 유효성 검증 및 이메일 추출
                // 여기서 발생하는 예외(예: 토큰 만료)는 호출한 컨트롤러의 ExceptionHandler가 처리하게 됩니다.
                String email = jwtTokenProvider.getUsername(refreshToken);

                // 2. Redis에 해당 이메일로 저장된 리프레시 토큰이 있는지 확인
                if (refreshTokenRepo.hasKey(email)) {
                        // 3. 있으면 삭제
                        refreshTokenRepo.delete(email);
                } else {
                        // 4. 없으면 이미 로그아웃된 상태로 간주하고 경고만 로깅
                        log.warn("이미 로그아웃되었거나 만료된 리프레시 토큰에 대한 로그아웃 요청: {}", email);
                }
        }

        @Transactional
        public LoginResponse signupOrLogin(NaverLoginProfile naverLoginProfile) {
                String email = naverLoginProfile.getEmail();
                String nickname = naverLoginProfile.getName();

                // 1. 사용자 조회 또는 생성
                User user = userMapper.findByEmail(email)
                                .orElseGet(() -> {
                                        // 신규 사용자면 회원가입 (비밀번호 없음!)
                                        String pw = "NAVER" + UUID.randomUUID().toString();
                                        User newUser = User.builder()
                                                        .email(email)
                                                        .nickname(nickname)
                                                        .password(passwordEncoder.encode(pw))
                                                        .role(User.Role.USER)
                                                        .provider(User.Provider.NAVER)
                                                        .build();
                                        userMapper.signup(newUser);
                                        log.info("신규 네이버 소셜 로그인 사용자 가입: {}", email);
                                        return newUser;
                                });
                if (user.getProvider() != User.Provider.NAVER) {
                        log.error("다른 Provider로 가입된 이메일로 네이버 로그인 시도 - email: {}, provider: {}",
                                        email, user.getProvider());
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        String.format("해당 이메일은 이미 %s 계정으로 가입되어 있습니다.", user.getProvider()));
                }

                // 2. 네이버 인증이 완료되었으므로 바로 JWT 토큰 발급
                UserDetails userDetails = new CurrentUser(
                                user.getId(),
                                user.getEmail(),
                                user.getNickname(),
                                user.getPassword(),
                                user.getRole());

                String accessToken = jwtTokenProvider.createAccessToken(userDetails);
                String refreshToken = jwtTokenProvider.createRefreshToken(userDetails);

                // 3. Redis에 리프레시 토큰 저장
                refreshTokenRepo.save(userDetails.getUsername(), refreshToken);

                log.info("네이버 소셜 로그인 완료: {}", email);
                return new LoginResponse(refreshToken, accessToken, user.getNickname());
        }

}
