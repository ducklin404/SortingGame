-- === Migration: reset schema (chạy trên DB dev!) ===

-- DROP (theo thứ tự để tránh FK error)
DROP TABLE IF EXISTS submissions;
DROP TABLE IF EXISTS rounds;
DROP TABLE IF EXISTS match_players;
DROP TABLE IF EXISTS matches;
DROP TABLE IF EXISTS player_stats;
DROP TABLE IF EXISTS invites_players;
DROP TABLE IF EXISTS invites;
DROP TABLE IF EXISTS sessions;
DROP TABLE IF EXISTS players;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- PLAYERS
CREATE TABLE players (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         username VARCHAR(50) UNIQUE NOT NULL,
                         display_name VARCHAR(100),
                         hashed_password VARCHAR(255) NOT NULL,
                         created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- SESSIONS
CREATE TABLE sessions (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          user_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
                          started_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          last_heartbeat TIMESTAMP WITHOUT TIME ZONE,
                          is_active BOOLEAN DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id);

-- INVITES
-- Thiết kế: mỗi invite là 1 hàng, lưu from_player và to_player trực tiếp (đơn giản và dễ truy vấn)
CREATE TABLE invites (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         from_player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
                         to_player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
                         status VARCHAR(10) NOT NULL CHECK (status IN ('PENDING','ACCEPTED','REJECTED','EXPIRED')),
                         created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         expires_at TIMESTAMP WITHOUT TIME ZONE,
                         updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_invites_from ON invites(from_player_id);
CREATE INDEX IF NOT EXISTS idx_invites_to ON invites(to_player_id);
CREATE INDEX IF NOT EXISTS idx_invites_status ON invites(status);

-- MATCHES
-- Lưu thông tin trận đấu; liên kết tới invite (nếu có) và 2 player trực tiếp
CREATE TABLE matches (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         invite_id UUID REFERENCES invites(id) ON DELETE SET NULL,
                         player1_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE, -- thường là inviter
                         player2_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE, -- invitee
                         winner_id UUID REFERENCES players(id),
                         created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         started_at TIMESTAMP WITHOUT TIME ZONE,
                         ended_at TIMESTAMP WITHOUT TIME ZONE,
                         player1_points INTEGER DEFAULT 0 CHECK (player1_points >= 0),
                         player2_points INTEGER DEFAULT 0 CHECK (player2_points >= 0),
                         status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ONGOING','FINISHED','CANCELLED')),
                         result VARCHAR(12) CHECK (result IN ('PLAYER1_WIN','PLAYER2_WIN','DRAW'))
);
CREATE INDEX IF NOT EXISTS idx_matches_player1 ON matches(player1_id);
CREATE INDEX IF NOT EXISTS idx_matches_player2 ON matches(player2_id);
CREATE INDEX IF NOT EXISTS idx_matches_invite ON matches(invite_id);

-- (tùy chọn) nếu muốn model nhiều-to-many players<->matches, dùng match_players
-- tuy nhiên hiện matches đã lưu 2 player nên thường không cần bảng này.
CREATE TABLE match_players (
                               match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
                               player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
                               role VARCHAR(10) NOT NULL CHECK (role IN ('A','B')), -- role A/B để map với player1/player2
                               PRIMARY KEY (match_id, player_id)
);

-- ROUNDS
CREATE TABLE rounds (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
                        round_number SMALLINT NOT NULL CHECK (round_number >= 1),
                        payload VARCHAR(1000) NOT NULL,
                        order_type VARCHAR(10) NOT NULL CHECK (order_type IN ('ASC','DESC','LEX')),
                        created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        deadline TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_rounds_match ON rounds(match_id);

-- SUBMISSIONS
CREATE TABLE submissions (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
                             match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
                             round_id UUID NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
                             submission_payload VARCHAR(2000),
                             submitted_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             time_ms INTEGER CHECK (time_ms >= 0),
                             is_correct BOOLEAN DEFAULT FALSE,
                             score INTEGER DEFAULT 0,
                             UNIQUE (player_id, match_id, round_id)
);
CREATE INDEX IF NOT EXISTS idx_submissions_match_round ON submissions(match_id, round_id);

-- PLAYER STATS
CREATE TABLE player_stats (
                              player_id UUID PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
                              total_points NUMERIC(12,2) DEFAULT 0,
                              wins INTEGER DEFAULT 0,
                              losses INTEGER DEFAULT 0,
                              draws INTEGER DEFAULT 0,
                              matches_played INTEGER DEFAULT 0,
                              last_updated TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Sample helper function: auto-update invites.updated_at on change (optional)
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_invites_updated_at ON invites;
CREATE TRIGGER trg_invites_updated_at
    BEFORE UPDATE ON invites
    FOR EACH ROW
    EXECUTE PROCEDURE update_timestamp();

-- End of migration
