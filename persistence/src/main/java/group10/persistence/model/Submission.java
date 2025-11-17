package group10.persistence.model;

import java.time.Instant;
import java.util.UUID;

public final class Submission {
    private final UUID id;
    private final UUID playerId;
    private final UUID matchId;
    private final UUID roundId;
    private final String submissionPayload;
    private final Instant submittedAt;
    private final Integer timeMs; // nullable
    private final boolean isCorrect;
    private final short score;

    public Submission(UUID id, UUID playerId, UUID matchId, UUID roundId, String submissionPayload,
                      Instant submittedAt, Integer timeMs, boolean isCorrect, short score) {
        this.id = id;
        this.playerId = playerId;
        this.matchId = matchId;
        this.roundId = roundId;
        this.submissionPayload = submissionPayload;
        this.submittedAt = submittedAt;
        this.timeMs = timeMs;
        this.isCorrect = isCorrect;
        this.score = score;
    }

    public UUID getId() { return id; }
    public UUID getPlayerId() { return playerId; }
    public UUID getMatchId() { return matchId; }
    public UUID getRoundId() { return roundId; }
    public String getSubmissionPayload() { return submissionPayload; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Integer getTimeMs() { return timeMs; }
    public boolean isCorrect() { return isCorrect; }
    public short getScore() { return score; }
}
