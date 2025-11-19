-- FILE 5: initialize random player stats

-- This script assigns random stats to every player that exists in the `players` table.
-- It only sets stats for players who don't already have a row in player_stats.

INSERT INTO player_stats (
    player_id,
    total_points,
    wins,
    losses,
    draws,
    matches_played,
    last_updated
)
SELECT
    p.id,
    -- total_points between 0 and 500 with 2 decimals
    ROUND((RANDOM() * 500)::numeric, 2),
    -- wins between 0 and 50
    (RANDOM() * 50)::int,
    -- losses between 0 and 50
    (RANDOM() * 50)::int,
    -- draws between 0 and 20
    (RANDOM() * 20)::int,
    -- matches_played = wins + losses + draws
    ((RANDOM() * 50)::int + (RANDOM() * 50)::int + (RANDOM() * 20)::int),
    NOW()
FROM players p
LEFT JOIN player_stats ps ON ps.player_id = p.id
WHERE ps.player_id IS NULL;


-- Optional: Normalize matches_played to equal wins + losses + draws exactly
UPDATE player_stats
SET matches_played = wins + losses + draws,
    last_updated = NOW();
