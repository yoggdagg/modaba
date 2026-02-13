-- --------------------------------------------------------
-- PostgreSQL Database Schema - Safe Initialization Script
-- Generated: January 27, 2026
-- This script preserves existing data - only creates missing objects
-- --------------------------------------------------------

-- ========================================================
-- SECTION 1: ENUM Type Definitions (Safe - Skip if exists)
-- ========================================================

-- User role enumeration
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Authentication provider types
DO $$ BEGIN
    CREATE TYPE provider_type AS ENUM ('LOCAL', 'KAKAO', 'NAVER', 'GOOGLE');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Friendship status
DO $$ BEGIN
    CREATE TYPE friendship_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Room type classification
DO $$ BEGIN
    CREATE TYPE room_type AS ENUM ('KYUNGDO', 'APPOINTMENT', 'FOCUS');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Room status
DO $$ BEGIN
    CREATE TYPE room_status AS ENUM ('WAITING', 'PLAYING', 'SCHEDULED', 'FINISHED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Participant status
DO $$ BEGIN
    CREATE TYPE participant_status AS ENUM ('READY', 'MOVING', 'ARRIVED', 'IN_GAME', 'ARRESTED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Player role in game
DO $$ BEGIN
    CREATE TYPE player_role AS ENUM ('POLICE', 'THIEF');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Action types in game
DO $$ BEGIN
    CREATE TYPE action_type AS ENUM ('ARREST', 'ESCAPE' , 'TAG');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Inventory item types
DO $$ BEGIN
    CREATE TYPE item_type_enum AS ENUM ('CHARACTER', 'BADGE');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- ========================================================
-- SECTION 2: Table Definitions (Safe - Skip if exists)
-- ========================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- --------------------------------------------------------
-- 2.1 Regions Master Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS regions (
    region_id SERIAL PRIMARY KEY,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    neighborhood VARCHAR(50) NOT NULL,
    adm_code VARCHAR(20) UNIQUE,
    center_lat NUMERIC(10,8),
    center_lng NUMERIC(11,8)
);

-- --------------------------------------------------------
-- 2.2 Users Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    profile_image_url VARCHAR(255) DEFAULT NULL,
    provider provider_type DEFAULT 'LOCAL',
    mmr INT DEFAULT 1000,
    role user_role DEFAULT 'USER',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    device_token VARCHAR(255)
);

CREATE TABLE gyeongdo_records (
                                  user_id INT NOT NULL PRIMARY KEY,
                                  police_win INT NOT NULL DEFAULT 0,
                                  police_lose INT NOT NULL DEFAULT 0,
                                  imprisoning_cnt INT NOT NULL DEFAULT 0,
                                  thief_win INT NOT NULL DEFAULT 0,
                                  thief_lose INT NOT NULL DEFAULT 0,
                                  rescuing_cnt INT NOT NULL DEFAULT 0,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Column comments for users table (safe to re-run)
COMMENT ON COLUMN users.user_id IS '고유 식별자';
COMMENT ON COLUMN users.nickname IS '사용자 닉네임';
COMMENT ON COLUMN users.email IS '계정 정보';
COMMENT ON COLUMN users.password IS '암호화된 비밀번호';
COMMENT ON COLUMN users.profile_image_url IS '프로필 이미지 경로';
COMMENT ON COLUMN users.provider IS '로그인 제공자';
COMMENT ON COLUMN users.mmr IS '게임 레이팅 점수';
COMMENT ON COLUMN users.role IS '사용자 권한 (USER, ADMIN)';
COMMENT ON COLUMN users.is_active IS '활성화 상태 (Soft Delete 활용)';

-- --------------------------------------------------------
-- 2.3 Friendships Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS friendships (
    requester_id INT NOT NULL,
    receiver_id INT NOT NULL,
    status friendship_status DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (requester_id, receiver_id),
    FOREIGN KEY (requester_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- --------------------------------------------------------
-- 2.4 Rooms Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS rooms (
    room_id SERIAL PRIMARY KEY,
    host_id INT NOT NULL,
    type room_type NOT NULL,
    status room_status DEFAULT 'WAITING',
    title VARCHAR(100) NOT NULL,
    max_user INT DEFAULT 8,
    appointment_time TIMESTAMP,
    place_name VARCHAR(255),
    target_lat NUMERIC(10,8),
    target_lng NUMERIC(11,8),
    region_id INT,
    is_noti_sent BOOLEAN DEFAULT FALSE,
    jail_boundary GEOMETRY(Polygon, 4326),
    boundary GEOMETRY(Polygon, 4326),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (host_id) REFERENCES users(user_id),
    FOREIGN KEY (region_id) REFERENCES regions(region_id)
);

-- --------------------------------------------------------
-- 2.5 Room Participants Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS room_participants (
    room_id INT NOT NULL,
    user_id INT NOT NULL,
    status participant_status DEFAULT 'READY',
    rank_pos INT,
    role player_role,
    is_escaped BOOLEAN DEFAULT FALSE,
    location_sharing_enabled BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (room_id, user_id),
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- --------------------------------------------------------
-- 2.6 Game Sessions Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS game_sessions (
    session_id SERIAL PRIMARY KEY,
    room_id INT,
    voice_channel_id VARCHAR(100),
    winner_team VARCHAR(50),
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    avg_mmr INT,
    end_time TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id)
);

-- --------------------------------------------------------
-- 2.7 Action Logs Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS action_logs (
    log_id SERIAL PRIMARY KEY,
    session_id INT NOT NULL,
    actor_id INT NOT NULL,
    target_id INT NOT NULL,
    type action_type NOT NULL,
    lat NUMERIC(10,8),
    lng NUMERIC(11,8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES game_sessions(session_id),
    FOREIGN KEY (actor_id) REFERENCES users(user_id),
    FOREIGN KEY (target_id) REFERENCES users(user_id)
);

-- --------------------------------------------------------
-- 2.8 Focus Records Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS focus_records (
    record_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    focus_scores JSONB NOT NULL,
    avg_score NUMERIC(5,2),
    session_id INT REFERENCES game_sessions(session_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- --------------------------------------------------------
-- 2.9 User Inventory Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_inventory (
    inventory_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    item_type item_type_enum NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    acquired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- --------------------------------------------------------
-- 2.10 Chat Logs Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_logs (
    log_id SERIAL PRIMARY KEY,
    room_id INT NOT NULL,
    sender_id INT NOT NULL,
    message TEXT,
    type VARCHAR(20),
    target_role player_role,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(user_id)
);

-- --------------------------------------------------------
-- 2.11 MMR History Table
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS mmr_history (
    history_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    game_id INT, -- 이 컬럼이 누락되어 있었습니다.
    change_value INT NOT NULL,
    result_mmr INT NOT NULL, -- final_mmr 대신 result_mmr로 변경 (코드와 일치)
    reason VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
    -- 필요하다면 game_id에 대한 외래키도 추가 가능
);

-- --------------------------------------------------------
-- 2.12 User Game Activities Table (Comprehensive Activity & Trajectory)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_game_activities (
    activity_id SERIAL PRIMARY KEY,
    session_id INT NOT NULL,
    user_id INT NOT NULL,
    -- Time-series trajectory data: [{"t": timestamp, "lat": lat, "lng": lng, "spd": speed}, ...]
    trajectory JSONB NOT NULL,
    total_distance NUMERIC(12,2) DEFAULT 0.0, -- Total distance in meters (up to 9.9B meters)
    avg_speed NUMERIC(10,2) DEFAULT 0.0,      -- Average speed in km/h
    max_speed NUMERIC(10,2) DEFAULT 0.0,      -- Max speed recorded in km/h
    active_time_sec INT DEFAULT 0,            -- Total time spent moving in seconds
    activity_score INT DEFAULT 0,             -- Computed activity score for rankings
    ai_report JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES game_sessions(session_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE study_reports (
        report_id SERIAL PRIMARY KEY,
        session_id INT NOT NULL,
        user_id INT NOT NULL,
        report_content TEXT, -- AI가 생성한 리포트 본문
       summary VARCHAR(255), -- 한 줄 요약
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (session_id) REFERENCES game_sessions(session_id) ON DELETE CASCADE,
       FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
    );
    
-- ========================================================
-- SECTION 3: Indexes for Performance Optimization (Safe)
-- ========================================================

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_nickname ON users(nickname);

-- Friendships table indexes
CREATE INDEX IF NOT EXISTS idx_friendships_receiver ON friendships(receiver_id);

-- Rooms table indexes
CREATE INDEX IF NOT EXISTS idx_rooms_status ON rooms(status);
CREATE INDEX IF NOT EXISTS idx_rooms_type ON rooms(type);

-- Room participants table indexes
CREATE INDEX IF NOT EXISTS idx_room_participants_user ON room_participants(user_id);

-- Regions table indexes
CREATE INDEX IF NOT EXISTS idx_regions_city_district ON regions(city, district);

CREATE INDEX idx_rooms_boundary ON rooms USING GIST (boundary);
-- ========================================================
-- END OF SCHEMA
-- ========================================================
