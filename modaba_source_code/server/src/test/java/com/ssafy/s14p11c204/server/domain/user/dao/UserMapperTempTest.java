//package com.ssafy.s14p11c204.server.domain.user.dao;
//
//import com.ssafy.s14p11c204.server.domain.user.Repositories.UserMapper;
//import com.ssafy.s14p11c204.server.domain.user.User;
//import com.ssafy.s14p11c204.server.domain.user.dto.ProfileDetailResponse;
//import com.ssafy.s14p11c204.server.global.util.IntegrationTestUtil;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//import java.util.UUID;
//
//// ★ Assertions
//import static org.junit.jupiter.api.Assertions.*;
//
//@Transactional
//class UserMapperTempTest implements IntegrationTestUtil {
//
//    @Autowired
//    private UserMapperTemp userMapperTemp;
//
//    @Autowired
//    private UserMapper userMapper;
//
//    @Test
//    @DisplayName("사용자 인증 정보 조회 - MyBatis 매핑(CurrentUser) 검증")
//    void selectCurrentUserByIdTest() {
//        // Given
//        String uniqueEmail = "auth_" + UUID.randomUUID() + "@example.com";
//        String uniqueNickname = "AuthUser_" + UUID.randomUUID().toString().substring(0, 8);
//        User user = User.builder()
//                .email(uniqueEmail)
//                .password("password")
//                .nickname(uniqueNickname)
//                .role(User.Role.USER)
//                .build();
//
//        userMapper.signup(user);
//
//        User savedUser = userMapper.findByEmail(uniqueEmail)
//                .orElseThrow(() -> new IllegalStateException("테스트용 사용자 생성 실패"));
//        Long userId = savedUser.getId();
//
//        // When
//        Optional<com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser> currentUserOptional = userMapperTemp.selectCurrentUserById(userId);
//
//        // Then
//        assertTrue(currentUserOptional.isPresent(), "인증 정보 조회 결과가 존재해야 합니다.");
//        com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser currentUser = currentUserOptional.get();
//
//        assertAll("CurrentUser 정보 검증",
//                () -> assertEquals(userId, currentUser.id(), "ID가 일치해야 합니다."),
//                () -> assertEquals(uniqueEmail, currentUser.email(), "이메일이 일치해야 합니다."),
//                () -> assertEquals(uniqueNickname, currentUser.nickname(), "닉네임이 일치해야 합니다."),
//                () -> assertEquals(User.Role.USER, currentUser.role(), "Role Enum 매핑이 정확해야 합니다.")
//        );
//    }
//}
