-- Enable UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";


-- PLAYERS
CREATE TABLE players (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(20) UNIQUE NOT NULL,
    display_name VARCHAR(30),
    hashed_password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- SESSIONS
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_heartbeat TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_sessions_user ON sessions(user_id);


-- INVITES
CREATE TABLE invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE TABLE invites_players (
    invite_id UUID NOT NULL REFERENCES invites(id) ON DELETE CASCADE,
    from_player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    to_player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    PRIMARY KEY (invite_id, from_player_id, to_player_id)
);


-- MATCHES
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    player_a_points INTEGER DEFAULT 0,
    player_b_points INTEGER DEFAULT 0,
    result VARCHAR(12) CHECK (result IN ('A_WIN', 'B_WIN', 'DRAW'))
);

CREATE TABLE match_players (
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    player_a UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    player_b UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    PRIMARY KEY (match_id, player_a, player_b)
);


-- ROUNDS

CREATE TABLE rounds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    round_number SMALLINT NOT NULL CHECK (round_number BETWEEN 1 AND 10),
    payload VARCHAR(255) NOT NULL,
    order_type VARCHAR(10) NOT NULL CHECK (order_type IN ('ASC', 'DESC', 'LEX')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deadline TIMESTAMP
);

CREATE INDEX idx_rounds_match ON rounds(match_id);


-- SUBMISSIONS
CREATE TABLE submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    round_id UUID NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    submission_payload VARCHAR(255),
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    time_ms INTEGER CHECK (time_ms >= 0),
    is_correct BOOLEAN DEFAULT FALSE,
    score SMALLINT DEFAULT 0,
    UNIQUE (player_id, match_id, round_id)
);

CREATE INDEX idx_submissions_match_round ON submissions(match_id, round_id);

-- PLAYER STATS
CREATE TABLE player_stats (
    player_id UUID PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
    total_points NUMERIC(10,2) DEFAULT 0,
    wins INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    draws INTEGER DEFAULT 0,
    matches_played INTEGER DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

