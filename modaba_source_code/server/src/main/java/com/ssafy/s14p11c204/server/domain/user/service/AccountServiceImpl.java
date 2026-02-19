package com.ssafy.s14p11c204.server.domain.user.service;

import com.ssafy.s14p11c204.server.domain.user.User;
import com.ssafy.s14p11c204.server.domain.user.Repositories.RefreshTokenRepository;
import com.ssafy.s14p11c204.server.domain.user.Repositories.UserMapper;
import com.ssafy.s14p11c204.server.domain.user.dao.UserMapperTemp;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import com.ssafy.s14p11c204.server.domain.user.dto.MmrHistoryDto;
import com.ssafy.s14p11c204.server.domain.user.dto.ProfileDetailResponse;
import com.ssafy.s14p11c204.server.domain.user.dto.ProfileUpdateRequest;
import com.ssafy.s14p11c204.server.domain.user.dto.PwUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class AccountServiceImpl implements AccountService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepo;

    @Override
    public User getDetailedProfile(String email) {
        return userMapper.findByEmail(email).orElseThrow(() -> new NoSuchElementException("그런 사용자 없음."));
    }

    @Override
    public void updateProfile(String email, ProfileUpdateRequest request) {
        User user = new User().builder()
                .email(email)
                .password(request.password())
                .imageLink(request.imageLink())
                .deviceToken(request.deviceToken())
                .build();
        int affectedRows = userMapper.update(user);
        if (affectedRows != 1) {
            throw new OptimisticLockingFailureException("알 수 없는 이유로 변경이 일어나지 않음");
        }
    }

    @Override
    @Transactional // DB 수정을 수반하므로 트랜잭션 처리가 필요합니다.
    public void updatePassword(String email, PwUpdateRequest request) {
        // 1. 사용자 조회
        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 사용자가 존재하지 않습니다."));

        // 2. 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.oldPass(), user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 2.5 새 비밀번호가 기존 비밀번호와 같은지 확인 (선택 사항이지만 권장)
        if (passwordEncoder.matches(request.newPass(), user.getPassword())) {
            throw new RuntimeException("새 비밀번호는 기존 비밀번호와 다르게 설정해야 합니다.");
        }

        // 3. 새 비밀번호 암호화
        String newEncodedPw = passwordEncoder.encode(request.newPass());

        // 4. 비밀번호 업데이트
        int affectedRows = userMapper.updatePassword(email, newEncodedPw);

        // 5. 결과 검증
        if (affectedRows == 0) {
            throw new RuntimeException("비밀번호 변경에 실패했습니다. (유효하지 않은 이메일)");
        }
    }

    @Override
    @Transactional
    public void unregister(String email) {
        int affectedRows = userMapper.unregister(email);
        if (affectedRows != 1) {
            throw new RuntimeException("회원 탈퇴 실패 오류");
        }

        refreshTokenRepo.delete(email);
        log.warn("사용자 {} 회원 탈퇴 완료.", email);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MmrHistoryDto> getMmrHistory(String email) {
        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        return userMapper.findMmrHistoryByUserId(user.getId());
    }
}
