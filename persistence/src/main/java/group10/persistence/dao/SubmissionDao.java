package group10.persistence.dao;

import group10.persistence.model.Submission;

import java.util.List;
import java.util.UUID;

public interface SubmissionDao {

     // Create a submission. Returns generated submission id.
    UUID createSubmission(UUID playerId, UUID matchId, UUID roundId, String submissionPayload, Integer timeMs);

    Submission findById(UUID submissionId);


    // Return single submission for given player/match/round if exists.
    Submission findByPlayerMatchRound(UUID playerId, UUID matchId, UUID roundId);

    List<Submission> listByMatchAndRound(UUID matchId, UUID roundId, int limit, int offset);

    List<Submission> listByPlayer(UUID playerId, int limit, int offset);


    // Mark a submission correct and set its score. Returns true if updated.
    boolean markCorrectAndSetScore(UUID submissionId, boolean isCorrect, short score);

    boolean deleteSubmission(UUID submissionId);
}
