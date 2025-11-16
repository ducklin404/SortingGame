BEGIN;

WITH from_p AS (
    SELECT id AS from_id
    FROM players
    WHERE username = 'u1'
    LIMIT 1
),
to_p AS (
    SELECT id AS to_id
    FROM players
    WHERE username = 'u2'
    LIMIT 1
),

existing_inv AS (
    SELECT ip.invite_id
    FROM invites_players ip
    JOIN from_p ON ip.from_player_id = from_p.from_id
    JOIN to_p   ON ip.to_player_id   = to_p.to_id
    LIMIT 1
),

upd AS (
    UPDATE invites
    SET status = 'PENDING',
        created_at = NOW(),
        expires_at = '2030-01-01 00:00:00'
    WHERE id IN (SELECT invite_id FROM existing_inv)
    RETURNING id
),

ins_inv AS (
    INSERT INTO invites (status, created_at, expires_at)
    SELECT 'PENDING', NOW(), '2030-01-01 00:00:00'
    WHERE NOT EXISTS (SELECT 1 FROM existing_inv)
    RETURNING id
),

inv_id AS (
    SELECT id AS invite_id FROM upd
    UNION ALL
    SELECT id FROM ins_inv
)

INSERT INTO invites_players (invite_id, from_player_id, to_player_id)
SELECT invite_id, from_id, to_id
FROM inv_id, from_p, to_p
ON CONFLICT DO NOTHING;

COMMIT;

SELECT i.id, i.status, i.created_at, i.expires_at
FROM invites i
JOIN invites_players ip ON ip.invite_id = i.id
JOIN players p_from ON p_from.username = 'u1' AND ip.from_player_id = p_from.id
JOIN players p_to   ON p_to.username   = 'u2' AND ip.to_player_id   = p_to.id
LIMIT 1;
