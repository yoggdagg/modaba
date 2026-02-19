-- Drop tables
DROP TABLE IF EXISTS mmr_history CASCADE;
DROP TABLE IF EXISTS friendships CASCADE;
DROP TABLE IF EXISTS chat_logs CASCADE;
DROP TABLE IF EXISTS user_game_activities CASCADE;
DROP TABLE IF EXISTS game_sessions CASCADE;
DROP TABLE IF EXISTS room_participants CASCADE;
DROP TABLE IF EXISTS rooms CASCADE;
DROP TABLE IF EXISTS regions CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Drop types
DROP TYPE IF EXISTS participant_status CASCADE;
DROP TYPE IF EXISTS player_role CASCADE;
DROP TYPE IF EXISTS room_status CASCADE;
DROP TYPE IF EXISTS room_type CASCADE;
DROP TYPE IF EXISTS provider_type CASCADE;
DROP TYPE IF EXISTS user_role CASCADE;

-- Enums
CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');
CREATE TYPE provider_type AS ENUM ('LOCAL', 'KAKAO', 'GOOGLE', 'NAVER');
CREATE TYPE room_type AS ENUM ('KYUNGDO', 'RUNNING');
CREATE TYPE room_status AS ENUM ('WAITING', 'SCHEDULED', 'PLAYING', 'FINISHED');
CREATE TYPE player_role AS ENUM ('POLICE', 'THIEF', 'OBSERVER');
CREATE TYPE participant_status AS ENUM ('READY', 'MOVING', 'ARRIVED', 'IN_GAME', 'ARRESTED');

-- Tables
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    nickname VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    role user_role DEFAULT 'USER',
    provider provider_type DEFAULT 'LOCAL',
    is_active BOOLEAN DEFAULT TRUE,
    profile_image_url VARCHAR(255),
    mmr BIGINT DEFAULT 1000,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    device_token VARCHAR(255)
);

CREATE TABLE regions (
    region_id BIGINT PRIMARY KEY,
    city VARCHAR(255),
    district VARCHAR(255),
    neighborhood VARCHAR(255)
);

CREATE TABLE rooms (
    room_id BIGSERIAL PRIMARY KEY,
    host_id BIGINT,
    type room_type,
    title VARCHAR(255),
    status room_status DEFAULT 'WAITING',
    appointment_time TIMESTAMP,
    place_name VARCHAR(255),
    target_lat DOUBLE PRECISION,
    target_lng DOUBLE PRECISION,
    region_id BIGINT,
    max_user INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (host_id) REFERENCES users(user_id),
    FOREIGN KEY (region_id) REFERENCES regions(region_id)
);

CREATE TABLE room_participants (
    room_id BIGINT,
    user_id BIGINT,
    role player_role,
    status participant_status DEFAULT 'READY',
    ready BOOLEAN DEFAULT FALSE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id, user_id),
    FOREIGN KEY (room_id) REFERENCES rooms(room_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Needed for ActivityServiceIntegrationTest
CREATE TABLE game_sessions (
    session_id BIGSERIAL PRIMARY KEY,
    room_id BIGINT,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    winner_team VARCHAR(50), -- 'POLICE' or 'THIEF'
    avg_mmr INT, -- 추가됨
    FOREIGN KEY (room_id) REFERENCES rooms(room_id)
);

CREATE TABLE user_game_activities (
    activity_id BIGSERIAL PRIMARY KEY,
    session_id BIGINT,
    user_id BIGINT,
    distance DOUBLE PRECISION,
    steps INT,
    calories DOUBLE PRECISION,
    trajectory JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES game_sessions(session_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE chat_logs (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT,
    sender_id BIGINT,
    message TEXT,
    type VARCHAR(50),
    target_role player_role,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE friendships (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT,
    receiver_id BIGINT,
    status VARCHAR(50), -- 'PENDING', 'ACCEPTED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (requester_id) REFERENCES users(user_id),
    FOREIGN KEY (receiver_id) REFERENCES users(user_id)
);

CREATE TABLE mmr_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    game_id BIGINT, -- 추가됨
    change_value INT,
    result_mmr INT, -- 추가됨
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
