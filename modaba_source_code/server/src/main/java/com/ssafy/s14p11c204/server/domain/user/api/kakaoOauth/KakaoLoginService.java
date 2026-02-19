package com.ssafy.s14p11c204.server.domain.user.api.kakaoOauth;

import com.ssafy.s14p11c204.server.domain.user.User;
import com.ssafy.s14p11c204.server.domain.user.Repositories.RefreshTokenRepository;
import com.ssafy.s14p11c204.server.domain.user.Repositories.UserMapper;
import com.ssafy.s14p11c204.server.domain.user.api.kakaoOauth.vo.KakaoLoginProfile;
import com.ssafy.s14p11c204.server.domain.user.api.kakaoOauth.vo.KakaoLoginVo;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import com.ssafy.s14p11c204.server.domain.user.dto.LoginResponse;
import com.ssafy.s14p11c204.server.global.util.JwtTokenProvider;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class KakaoLoginService {

        private final WebClient webClient;
        private final UserMapper userMapper;
        private final PasswordEncoder passwordEncoder;
        private final JwtTokenProvider jwtTokenProvider;
        private final RefreshTokenRepository refreshTokenRepo;

        @Value("${api.kakao.rest_api_key}")
        private String restApiKey;

        @Value("${api.kakao.redirect_uri}")
        private String redirectUri;

        @Value("${api.kakao.client_secret}")
        private String clientSecret;

        public KakaoLoginService(
                        WebClient webClient,
                        UserMapper userMapper,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider,
                        RefreshTokenRepository refreshTokenRepo) {
                this.webClient = webClient;
                this.userMapper = userMapper;
                this.passwordEncoder = passwordEncoder;
                this.jwtTokenProvider = jwtTokenProvider;
                this.refreshTokenRepo = refreshTokenRepo;
        }

        /**
         * @description 카카오 로그인을 위하여 Access Token을 발급받음
         * @param resValue
         *                  1) code: 토큰 발급용 1회용 코드
         * @param grantType
         *                  1) 발급: 'authorization_code'
         *                  2) 갱신: 'refresh_token'
         * @return KakaoLoginVo (액세스 토큰 정보)
         */
        public KakaoLoginVo requestKakaoLoginAccessToken(
                        Map<String, String> resValue,
                        String grantType) {

                System.out.println("카카오 rest API Key:" + restApiKey);
                System.out.println("카카오 client Secret Key:" + clientSecret);
                System.out.println("카카오 리다이렉트 URL:" + redirectUri);
                // ✅ 카카오는 POST + application/x-www-form-urlencoded 방식
                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                formData.add("grant_type", grantType);
                formData.add("client_id", restApiKey);
                formData.add("redirect_uri", redirectUri);
                formData.add("code", resValue.get("code"));
                formData.add("client_secret", clientSecret); // ⭐ 추가 필요

                return webClient
                                .post()
                                .uri("https://kauth.kakao.com/oauth/token") // ✅ 카카오 엔드포인트
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .body(BodyInserters.fromFormData(formData))
                                .retrieve()
                                .bodyToMono(KakaoLoginVo.class)
                                .block();
        }

        /**
         * @description 카카오 프로필 API 호출 (사용자 정보 가져오기)
         * @param kakaoLoginVo 액세스 토큰 정보
         * @return KakaoLoginProfile (사용자 프로필 정보)
         */
        public KakaoLoginProfile requestKakaoLoginProfile(KakaoLoginVo kakaoLoginVo) {
                return webClient
                                .get()
                                .uri("https://kapi.kakao.com/v2/user/me") // ✅ 카카오 프로필 API
                                .header("Authorization", "Bearer " + kakaoLoginVo.getAccessToken())
                                .retrieve()
                                .bodyToMono(KakaoLoginProfile.class)
                                .block();
        }

        /**
         * @description 카카오 소셜 로그인 - 회원가입 또는 로그인 처리
         * @param kakaoLoginProfile 카카오 사용자 프로필
         * @return LoginResponse (JWT 토큰 및 사용자 정보)
         */
        @Transactional
        public LoginResponse signupOrLogin(KakaoLoginProfile kakaoLoginProfile) {
                String email = kakaoLoginProfile.getEmail();
                String nickname = kakaoLoginProfile.getNickname();

                // 이메일이 없으면 에러 (카카오는 이메일 제공 동의가 필수)
                if (email == null || email.isEmpty()) {
                        log.error("카카오 프로필에 이메일이 없습니다. 이메일 제공 동의가 필요합니다.");
                        throw new IllegalArgumentException("이메일 정보가 필요합니다. 카카오 로그인 시 이메일 제공에 동의해주세요.");
                }

                // 1. 사용자 조회 또는 생성
                User user = userMapper.findByEmail(email)
                                .orElseGet(() -> {
                                        // ✅ UUID로 랜덤 비밀번호 생성 (소셜 로그인 사용자는 이 비밀번호 사용 불가)
                                        String randomPassword = UUID.randomUUID().toString();

                                        User newUser = User.builder()
                                                        .email(email)
                                                        .nickname(nickname != null ? nickname : "카카오사용자")
                                                        .password(passwordEncoder.encode(randomPassword)) // ✅ 인코딩된 랜덤
                                                                                                          // 비밀번호
                                                        .role(User.Role.USER)
                                                        .provider(User.Provider.KAKAO) // ✅ KAKAO
                                                        .build();

                                        userMapper.signup(newUser);
                                        log.info("신규 카카오 소셜 로그인 사용자 가입: {}", email);
                                        return newUser;
                                });

                // 2. Provider 검증 (선택사항 - 다른 Provider로 가입된 이메일인지 확인)
                if (user.getProvider() != User.Provider.KAKAO) {
                        log.error("다른 Provider로 가입된 이메일로 카카오 로그인 시도 - email: {}, provider: {}",
                                        email, user.getProvider());
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        String.format("해당 이메일은 이미 %s 계정으로 가입되어 있습니다.", user.getProvider()));
                }

                // 3. JWT 토큰 발급
                UserDetails userDetails = new CurrentUser(
                                user.getId(),
                                user.getEmail(),
                                user.getNickname(),
                                user.getPassword(),
                                user.getRole());

                String accessToken = jwtTokenProvider.createAccessToken(userDetails);
                String refreshToken = jwtTokenProvider.createRefreshToken(userDetails);

                // 4. Redis에 리프레시 토큰 저장
                refreshTokenRepo.save(userDetails.getUsername(), refreshToken);

                log.info("카카오 소셜 로그인 완료 - email: {}, nickname: {}", email, user.getNickname());
                return new LoginResponse(refreshToken, accessToken, user.getNickname());
        }
}
