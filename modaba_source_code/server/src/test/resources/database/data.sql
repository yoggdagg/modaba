-- 기존 데이터 삭제 (중복 방지)
DELETE FROM room_participants;
DELETE FROM rooms;
DELETE FROM users;

-- 테스트 유저 1 (ID: 6, FRANK 역할 대용)
-- 비밀번호: password123! (BCrypt: $2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2)
INSERT INTO users (user_id, email, nickname, password, role, created_at)
VALUES (6, 'test@example.com', '테스트유저1', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'USER', NOW());

-- 테스트 유저 2 (ID: 7)
INSERT INTO users (user_id, email, nickname, password, role, created_at)
VALUES (7, 'test2@example.com', '테스트유저2', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'USER', NOW());

-- 테스트용 방 생성 (RadioIntegrationTest 등에서 참조하는 구조)
INSERT INTO rooms (room_id, host_id, type, title, status)
VALUES (1, 6, 'KYUNGDO', 'Radio Test Room', 'PLAYING');

-- 방 참가자 설정 (테스트유저1을 POLICE 팀으로)
INSERT INTO room_participants (room_id, user_id, role)
VALUES (1, 6, 'POLICE');
