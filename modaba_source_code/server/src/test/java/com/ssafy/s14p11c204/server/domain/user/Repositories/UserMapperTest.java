//package com.ssafy.s14p11c204.server.domain.user.Repositories;
//
//import com.ssafy.s14p11c204.server.domain.user.User;
//import com.ssafy.s14p11c204.server.global.util.IntegrationTestUtil;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//import java.util.UUID;
//
//import static com.ssafy.s14p11c204.server.domain.user.DefaultUsers.LARRY;
//import static org.junit.jupiter.api.Assertions.*;
//
//@Transactional
//class UserMapperTest implements IntegrationTestUtil {
//
//    @Autowired
//    private UserMapper userMapper;
//
//    @Test
//    @DisplayName("회원가입 및 이메일로 사용자 조회")
//    void signupAndFindByEmailTest() {
//        // Given
//        String uniqueEmail = "testuser_" + java.util.UUID.randomUUID().toString() + "@example.com";
//        String uniqueNickname = "TestUser_" + java.util.UUID.randomUUID().toString().substring(0, 8);
//        User user = User.builder()
//                .email(uniqueEmail)
//                .password("password") // Raw password for test
//                .nickname(uniqueNickname)
//                .role(User.Role.USER)
//                .build();
//
//        // When
//        userMapper.signup(user);
//        Optional<User> foundUserOptional = userMapper.findByEmail(uniqueEmail);
//
//        // Then
//        assertTrue(foundUserOptional.isPresent(), "저장된 사용자를 이메일로 찾을 수 있어야 합니다.");
//        User foundUser = foundUserOptional.get();
//        assertAll("사용자 정보 검증",
//                () -> assertEquals(uniqueEmail, foundUser.getEmail()),
//                () -> assertEquals(uniqueNickname, foundUser.getNickname()),
//                () -> assertNotNull(foundUser.getId(), "사용자 ID는 null이 아니어야 합니다."),
//                () -> assertEquals(User.Role.USER, foundUser.getRole(), "기본 역할은 USER여야 합니다.")
//        );
//    }
//
//    @Test
//    @DisplayName("존재하지 않는 이메일로 사용자 조회 시 빈 Optional 반환")
//    void findByEmail_NonExistent() {
//        // When
//        Optional<User> foundUserOptional = userMapper.findByEmail("nonexistent@example.com");
//
//        // Then
//        assertTrue(foundUserOptional.isEmpty(), "존재하지 않는 사용자를 조회하면 결과는 비어있어야 합니다.");
//    }
//
//    @Test
//    @DisplayName("이메일 존재 여부 확인")
//    void existsByEmailTest() {
//        // Given
//        String uniqueEmail = "exists_" + java.util.UUID.randomUUID().toString() + "@example.com";
//        String uniqueNickname = "ExistsUser_" + java.util.UUID.randomUUID().toString().substring(0, 8);
//        User user = User.builder()
//                .email(uniqueEmail)
//                .password("password")
//                .nickname(uniqueNickname)
//                .role(User.Role.USER)
//                .build();
//        userMapper.signup(user);
//
//        // When
//        boolean exists = userMapper.existsByEmail(uniqueEmail);
//        boolean notExists = userMapper.existsByEmail("nonexistent_" + java.util.UUID.randomUUID().toString() + "@example.com");
//
//        // Then
//        assertTrue(exists, "저장된 이메일은 존재한다고 나와야 합니다.");
//        assertFalse(notExists, "저장되지 않은 이메일은 존재하지 않는다고 나와야 합니다.");
//    }
//}