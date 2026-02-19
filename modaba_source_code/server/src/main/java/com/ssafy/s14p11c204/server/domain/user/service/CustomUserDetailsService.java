package com.ssafy.s14p11c204.server.domain.user.service;

import com.ssafy.s14p11c204.server.domain.user.User;
import com.ssafy.s14p11c204.server.domain.user.Repositories.UserMapper;
import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. DB 조회
        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        // 2. CurrentUser(UserDetails) 변환
        return new CurrentUser(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getPassword(),
                User.Role.USER);
    }
}
