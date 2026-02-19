package com.ssafy.s14p11c204.server.domain.user.service;

import com.ssafy.s14p11c204.server.domain.user.User;
import com.ssafy.s14p11c204.server.domain.user.Repositories.RedisRepository;
import com.ssafy.s14p11c204.server.domain.user.Repositories.RefreshTokenRepository;
import com.ssafy.s14p11c204.server.domain.user.Repositories.UserMapper;
import com.ssafy.s14p11c204.server.domain.user.dto.*;
import com.ssafy.s14p11c204.server.global.util.JwtTokenProvider;

// JWT
import io.jsonwebtoken.JwtException;

// Redis
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;

// Mail
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
// Util
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

// Spring
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Lombok
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepo;
    private final RedisRepository redisRepository;
    private final JavaMailSender mailSender;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
            RefreshTokenRepository refreshTokenRepo, RedisRepository redisRepository, JavaMailSender mailSender) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepo = refreshTokenRepo;
        this.redisRepository = redisRepository;
        this.mailSender = mailSender;
    }

    @Override
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
                .provider(User.Provider.LOCAL) // Provider 설정 추가 (LOCAL)
                .isActive(true) // 활성화 상태 기본값 true
                .mmr(1000) // 기본 MMR 설정 (선택 사항)
                .build();

        userMapper.signup(user);
    }

    @Override
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
        String email = jwtTokenProvider.getUsername(refreshToken);

        if (refreshTokenRepo.hasKey(email)) {
            refreshTokenRepo.delete(email);
        } else {
            log.warn("이미 로그아웃되었거나 만료된 리프레시 토큰에 대한 로그아웃 요청: {}", email);
        }
    }

    @Override
    public void sendCode(String email) {
        Optional<User> userOpt = userMapper.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        String code = createVerificationCode();
        redisRepository.save("password-reset:" + email, code, 5, TimeUnit.MINUTES);

        try {
            sendPasswordResetEmail(email, code);
            log.info("패스워드 재설정 인증코드 전송: {}", email);
        } catch (MessagingException e) {
            log.error("패스워드 재설정 인증코드 전송 실패: {}", email, e);
            redisRepository.delete("password-reset:" + email);
            return;
        }
    }

    @Override
    public void verifyCode(String email, String code) {
        String savedCode = redisRepository.get("password-reset:" + email);

        if (savedCode == null) {
            throw new RuntimeException("인증 코드가 만료되었거나 존재하지 않습니다");
        }

        if (!savedCode.equals(code)) {
            throw new RuntimeException("인증 코드가 일치하지 않습니다");
        }

        log.info("비밀번호 재설정 인증 코드 검증 성공: {}", email);
    }

    @Override
    @Transactional
    public void resetPw(String email, String code, String newPw) {
        String savedCode = redisRepository.get("password-reset:" + email);

        if (savedCode == null) {
            throw new RuntimeException("인증 코드가 만료되었거나 존재하지 않습니다");
        }

        if (!savedCode.equals(code)) {
            throw new RuntimeException("인증 코드가 일치하지 않습니다");
        }

        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        String encodedPassword = passwordEncoder.encode(newPw);
        user.setPassword(encodedPassword);
        userMapper.update(user);

        redisRepository.delete("password-reset:" + email);
        refreshTokenRepo.delete(email);

        log.info("비밀번호 재설정 완료: {}", email);
    }

    private String createVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private void sendPasswordResetEmail(String to, String code) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("비밀번호 재설정 인증 코드");

        String htmlContent = buildEmailContent(code);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private String buildEmailContent(String code) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .code-box {
                            background-color: #f4f4f4;
                            padding: 20px;
                            text-align: center;
                            font-size: 32px;
                            font-weight: bold;
                            letter-spacing: 5px;
                            margin: 20px 0;
                        }
                        .warning { color: #d32f2f; font-size: 14px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h2>비밀번호 재설정 인증 코드</h2>
                        <p>비밀번호 재설정을 위한 인증 코드입니다.</p>
                        <div class="code-box">%s</div>
                        <p class="warning">⚠️ 이 코드는 5분 후 만료됩니다.</p>
                        <p class="warning">본인이 요청하지 않았다면 이 이메일을 무시하세요.</p>
                    </div>
                </body>
                </html>
                """.formatted(code);
    }

    @Override
    public String reissue(String refreshToken) {
        String email = jwtTokenProvider.getUsername(refreshToken);
        String savedToken = refreshTokenRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("잘못된 리프레시 토큰입니다."));
        if (!refreshToken.equals(savedToken)) {
            throw new RuntimeException("잘못된 리프레시 토큰");
        }

        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 사용자가 존재하지 않습니다."));

        UserDetails userDetails = new CurrentUser(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getPassword(),
                user.getRole());
        return jwtTokenProvider.createAccessToken(userDetails);

    }

}
