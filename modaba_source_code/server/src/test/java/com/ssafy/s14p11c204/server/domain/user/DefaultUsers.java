package com.ssafy.s14p11c204.server.domain.user;

import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;

public class DefaultUsers {
    // ==========================================
    // Normal Users (A-Z) & Villains
    // ==========================================

    // A: Alice (Standard User)
    public static final CurrentUser ALICE = CurrentUser.builder()
            .id(1L)
            .email("alice@ssafy.com")
            .nickname("Alice")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // B: Bob (Standard User)
    public static final CurrentUser BOB = CurrentUser.builder()
            .id(2L)
            .email("bob@ssafy.com")
            .nickname("Bob")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // C: Charlie (Standard User)
    public static final CurrentUser CHARLIE = CurrentUser.builder()
            .id(3L)
            .email("charlie@ssafy.com")
            .nickname("Charlie")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // D: Dave
    public static final CurrentUser DAVE = CurrentUser.builder()
            .id(4L)
            .email("dave@ssafy.com")
            .nickname("Dave")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // E: Eve (Eavesdropper - 도청자 / 빌런)
    public static final CurrentUser EVE = CurrentUser.builder()
            .id(5L)
            .email("eve@ssafy.com")
            .nickname("Eve")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // F: Frank
    public static final CurrentUser FRANK = CurrentUser.builder()
            .id(6L)
            .email("frank@ssafy.com")
            .nickname("Frank")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // G: Grace
    public static final CurrentUser GRACE = CurrentUser.builder()
            .id(7L)
            .email("grace@ssafy.com")
            .nickname("Grace")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // H: Heidi
    public static final CurrentUser HEIDI = CurrentUser.builder()
            .id(8L)
            .email("heidi@ssafy.com")
            .nickname("Heidi")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // I: Ivan
    public static final CurrentUser IVAN = CurrentUser.builder()
            .id(9L)
            .email("ivan@ssafy.com")
            .nickname("Ivan")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // J: Judy
    public static final CurrentUser JUDY = CurrentUser.builder()
            .id(10L)
            .email("judy@ssafy.com")
            .nickname("Judy")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // K: Kevin
    public static final CurrentUser KEVIN = CurrentUser.builder()
            .id(11L)
            .email("kevin@ssafy.com")
            .nickname("Kevin")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // L: Larry
    public static final CurrentUser LARRY = CurrentUser.builder()
            .id(12L)
            .email("larry@ssafy.com")
            .nickname("Larry")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // M: Mallory (Malicious - 악의적 공격자 / 빌런)
    public static final CurrentUser MALLORY = CurrentUser.builder()
            .id(13L)
            .email("mallory@ssafy.com")
            .nickname("Mallory")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // N: Nancy
    public static final CurrentUser NANCY = CurrentUser.builder()
            .id(14L)
            .email("nancy@ssafy.com")
            .nickname("Nancy")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // O: Oscar (Opponent - 적대자 / 빌런)
    public static final CurrentUser OSCAR = CurrentUser.builder()
            .id(15L)
            .email("oscar@ssafy.com")
            .nickname("Oscar")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // P: Peggy (Prover)
    public static final CurrentUser PEGGY = CurrentUser.builder()
            .id(16L)
            .email("peggy@ssafy.com")
            .nickname("Peggy")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // Q: Quentin
    public static final CurrentUser QUENTIN = CurrentUser.builder()
            .id(17L)
            .email("quentin@ssafy.com")
            .nickname("Quentin")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // R: Rupert
    public static final CurrentUser RUPERT = CurrentUser.builder()
            .id(18L)
            .email("rupert@ssafy.com")
            .nickname("Rupert")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // S: Sybil (Sybil Attack - 다중 계정 공격자 / 빌런)
    public static final CurrentUser SYBIL = CurrentUser.builder()
            .id(19L)
            .email("sybil@ssafy.com")
            .nickname("Sybil")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // T: Trudy (Intruder - 침입자 / 빌런)
    public static final CurrentUser TRUDY = CurrentUser.builder()
            .id(20L)
            .email("trudy@ssafy.com")
            .nickname("Trudy")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // U: Ursula
    public static final CurrentUser URSULA = CurrentUser.builder()
            .id(21L)
            .email("ursula@ssafy.com")
            .nickname("Ursula")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // V: Victor (Verifier)
    public static final CurrentUser VICTOR = CurrentUser.builder()
            .id(22L)
            .email("victor@ssafy.com")
            .nickname("Victor")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // W: Walter (Warden - 감시자 / 관리자 권한 부여)
    public static final CurrentUser WALTER = CurrentUser.builder()
            .id(23L)
            .email("walter@ssafy.com")
            .nickname("Walter")
            .password("pass1234")
            .role(User.Role.ADMIN) // 관리자 테스트용
            .build();

    // X: Xavier
    public static final CurrentUser XAVIER = CurrentUser.builder()
            .id(24L)
            .email("xavier@ssafy.com")
            .nickname("Xavier")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // Y: Yves
    public static final CurrentUser YVES = CurrentUser.builder()
            .id(25L)
            .email("yves@ssafy.com")
            .nickname("Yves")
            .password("pass1234")
            .role(User.Role.USER)
            .build();

    // Z: Zelda
    public static final CurrentUser ZELDA = CurrentUser.builder()
            .id(26L)
            .email("zelda@ssafy.com")
            .nickname("Zelda")
            .password("pass1234")
            .role(User.Role.USER)
            .build();
}