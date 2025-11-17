package group10.server.game;

import com.fasterxml.jackson.databind.JsonNode;
import group10.common.dto.Envelope;
import group10.common.net.LengthPrefixedIO;
import group10.common.protocol.ProtocolConstants;
import group10.common.util.JsonUtil;
import group10.persistence.dao.MatchDao;
import group10.persistence.model.Submission;
import group10.server.net.ConnectionRegistry;
import group10.persistence.dao.RoundsDao;
import group10.persistence.dao.SubmissionDao;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import group10.server.session.SessionManager;

import java.net.Socket;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Per-match instance that runs rounds. Keep it small and focused.
 */
public class MatchInstance {
    private final UUID matchId;
    private final UUID playerA;
    private final UUID playerB;
    private int playerAScore = 0;
    private int playerBScore = 0;
    private final RoundGenerator generator;
    private final RoundValidator validator;
    private final ConnectionRegistry connRegistry;
    private final SessionManager sessionManager;
    private final ScheduledExecutorService scheduler;
    private final RoundsDao roundsDao;
    private final SubmissionDao submissionsDao;
    private final MatchDao matchDao;
    private short currentRound = 0;
    private final int totalRounds = GameRules.ROUND_COUNT;
    // track scheduled deadline future(s) so we can cancel when both submit
    private final ConcurrentMap<UUID, ScheduledFuture<?>> roundDeadlines = new ConcurrentHashMap<>();

    // in-memory submissions for fast check: roundId -> (sessionId -> submissionJson)
    private final ConcurrentMap<UUID, ConcurrentMap<UUID, ObjectNode>> inMemorySubmissions = new ConcurrentHashMap<>();

    public MatchInstance(UUID a, UUID b,
                         RoundGenerator generator,
                         RoundValidator validator,
                         ConnectionRegistry connRegistry,
                         SessionManager sessionManager,
                         ScheduledExecutorService scheduler,
                         RoundsDao roundsDao,
                         SubmissionDao submissionsDao,
                         MatchDao matchDao) {
        this.playerA = a; this.playerB = b;
        this.generator = generator;
        this.validator = validator;
        this.connRegistry = connRegistry;
        this.sessionManager = sessionManager;
        this.scheduler = scheduler;
        this.roundsDao = roundsDao;
        this.submissionsDao = submissionsDao;
        this.matchDao = matchDao;

        this.matchId = matchDao.createMatch(playerA, playerB);

    }

    public UUID getMatchId(){
        return this.matchId;
    }

    public void startNextRound() {
        if (currentRound == 0){
            matchDao.startMatch(this.matchId);
        }

        currentRound++;
        if (currentRound > totalRounds) {
            endMatch();
            return;
        }
        // generate payload
        ObjectNode payload = generator.generateLetterRound(2);
        // persist the round (roundsDao.insertRound returns roundId)
        UUID roundId = roundsDao.createRound(this.matchId, currentRound, payload.toString(),
                payload.get("order").asText(),
                Instant.ofEpochMilli(System.currentTimeMillis() + GameRules.ROUND_TIME_MS));
        // Send START_ROUND to both players
        ObjectNode msg = JsonUtil.MAPPER.createObjectNode();
        msg.put("matchId", matchId.toString());
        msg.put("round", currentRound);
        msg.put("roundId", roundId.toString());
        msg.set("items", payload.get("items"));
        msg.put("order", payload.get("order").asText());
        msg.put("deadlineTs", System.currentTimeMillis() + GameRules.ROUND_TIME_MS);
        msg.put("playerAPoint", this.playerAScore);
        msg.put("playerBPoint", this.playerBScore);
        Envelope env = new Envelope(ProtocolConstants.START_ROUND, msg, null);
        sendToPlayer(playerA, env);
        sendToPlayer(playerB, env);

        // schedule deadline handler
        ScheduledFuture<?> sf = scheduler.schedule(() -> onRoundDeadline(roundId), GameRules.ROUND_TIME_MS, TimeUnit.MILLISECONDS);
        roundDeadlines.put(roundId, sf);
    }
    public class SubmissionRecord {
        private final String payload;
        private final long timestamp;

        public SubmissionRecord(String payload, long timestamp) {
            this.payload = payload;
            this.timestamp = timestamp;
        }

        public String getPayload() { return payload; }
        public long getTimestamp() { return timestamp; }
    }

    private void onRoundDeadline(UUID roundId) {
        evaluateRoundAndProceed(roundId);
    }

    public void handleSubmission(UUID sessionId, ObjectNode payload) {
        // minimal validation
        if (payload == null) return;
        String roundIdStr = payload.path("roundId").asText();
        if (roundIdStr == null) return;

        UUID roundId = UUID.fromString(roundIdStr);
        UUID playerId = sessionManager.getUserId(sessionId);
        // persist submission to DB (best-effort)
        System.out.println("Test 1");
        System.out.println(payload);
        long ts = System.currentTimeMillis();
        try {
            System.out.println("payload");
            System.out.println(payload);
            System.out.println(payload.path("submission").toString());
            submissionsDao.createSubmission(playerId, matchId, roundId, payload.path("submission").toString(), ts);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Test 2");
        // store in-memory
        inMemorySubmissions.computeIfAbsent(roundId, k -> new ConcurrentHashMap<>())
                .put(playerId, payload);

        // if both players submitted for this round, evaluate immediately
        ConcurrentMap<UUID, ObjectNode> map = inMemorySubmissions.get(roundId);
        if (map != null) {
            if (map.containsKey(playerA) && map.containsKey(playerB)) {
                // cancel deadline
                ScheduledFuture<?> sf = roundDeadlines.remove(roundId);
                if (sf != null) sf.cancel(false);

                // evaluate and broadcast result
                evaluateRoundAndProceed(roundId);
            }
        }
    }

    private void evaluateRoundAndProceed(UUID roundId) {
        try {
            // load round payload (string) and parse to ObjectNode
            String roundPayloadStr = roundsDao.findById(roundId).getPayload();
            ObjectNode roundPayload = (ObjectNode) JsonUtil.MAPPER.readTree(roundPayloadStr);

            // fetch submissions for both players using player ids
            Submission subA = submissionsDao.findByPlayerMatchRound(playerA, matchId, roundId);
            Submission subB = submissionsDao.findByPlayerMatchRound(playerB, matchId, roundId);

            ArrayNode submissionAArray = JsonUtil.MAPPER.createArrayNode();
            ArrayNode submissionBArray = JsonUtil.MAPPER.createArrayNode();

            System.out.println("TESTING");
            if (subA != null) {
                String p = subA.getSubmissionPayload();
                // if saved payload is a JSON array, parse; otherwise try splitting a string
                try {
                    JsonNode n = JsonUtil.MAPPER.readTree(p);
                    if (n.isArray()) submissionAArray = (ArrayNode)n;
                    else submissionAArray.add(p);
                } catch (Exception ex) {
                    // fallback: treat as comma separated
                    for (String s : p.split(",")) submissionAArray.add(s);
                }
            }


            if (subB != null) {
                String p = subB.getSubmissionPayload();
                try {
                    JsonNode n = JsonUtil.MAPPER.readTree(p);
                    if (n.isArray()) submissionBArray = (ArrayNode)n;
                    else submissionBArray.add(p);
                } catch (Exception ex) {
                    for (String s : p.split(",")) submissionBArray.add(s);
                }
            }

            System.out.println("A array");
            System.out.println(submissionAArray);
            System.out.println("B array");
            System.out.println(submissionBArray);

            // validate
            RoundValidator.ValidationResult resA = validator.validate(roundPayload, submissionAArray);
            RoundValidator.ValidationResult resB = validator.validate(roundPayload, submissionBArray);

            // convert correctness to points.
            short ptsA = (short)(resA.correct() ? 1 : 0);
            short ptsB = (short)(resB.correct() ? 1 : 0);

            // persist submission correctness & score (if DAO supports it)
            if (subA != null) submissionsDao.markCorrectAndSetScore(subA.getId(), resA.correct(), ptsA);
            if (subB != null) submissionsDao.markCorrectAndSetScore(subB.getId(), resB.correct(), ptsB);

            // update in-memory scores
            addPointToPlayerA(ptsA);
            addPointToPlayerB(ptsB);
            System.out.println("Score");
            System.out.println(this.playerAScore);
            System.out.println(this.playerBScore);
            // build result payload
            ObjectNode result = JsonUtil.MAPPER.createObjectNode();
            result.put("matchId", matchId.toString());
            result.put("roundId", roundId.toString());
            result.put("round", currentRound);
            result.put("playerASubmission", submissionAArray);
            result.put("playerBSubmission", submissionBArray);
            result.put("playerAPoint", playerAScore);
            result.put("playerBPoint", playerBScore); // consistent casing
            result.put("message", "Round finished");
            System.out.println("result");
            System.out.println(result);
            Envelope env = new Envelope(ProtocolConstants.ROUND_RESULT, result, null);
            sendToPlayer(playerA, env);
            sendToPlayer(playerB, env);

        } catch (Exception e) {
            e.printStackTrace();
            ObjectNode err = JsonUtil.MAPPER.createObjectNode()
                    .put("matchId", matchId.toString())
                    .put("message", "Server error evaluating round");
            Envelope env = new Envelope(ProtocolConstants.ROUND_RESULT, err, null);
            sendToPlayer(playerA, env);
            sendToPlayer(playerB, env);
        } finally {
            inMemorySubmissions.remove(roundId);
            roundDeadlines.remove(roundId);
            startNextRound();
        }
    }



    private void sendToPlayer(UUID playerId, Envelope env) {
        UUID sessionId = sessionManager.getLatestActiveSession(playerId).getId();
        Socket socket = connRegistry.getSocket(sessionId);

        try { LengthPrefixedIO.writeObject(socket, env); } catch (Exception e) {e.printStackTrace();}

    }

    private void addPointToPlayerA(int points) {
        playerAScore += points;
    }

    private void addPointToPlayerB(int points) {
        playerBScore += points;
    }


    private void endMatch() {
        String result;
        long aTime = submissionsDao.getTotalElapsedTimeForPlayerInMatch(this.playerA, this.matchId, false, true);
        long bTime = submissionsDao.getTotalElapsedTimeForPlayerInMatch(this.playerB, this.matchId, false, true);
        if (this.playerAScore > this.playerBScore){
            result = "A_WIN";
        }else if (this.playerBScore > this.playerAScore){
            result = "B_WIN";
        }else{
            if (aTime > bTime){
                result = "A_WIN";
            }else if (bTime > aTime){
                result = "B_WIN";}
            else{
                result = "DRAW";
            }
        }
        matchDao.finishMatch(this.matchId, result);
        matchDao.updatePoints(this.matchId, this.playerAScore, this.playerBScore);
        // build payload to send to both players
        ObjectNode payload = JsonUtil.MAPPER.createObjectNode();
        payload.put("matchId", matchId.toString());
        payload.put("result", result);
        payload.put("playerAPoint", this.playerAScore);
        payload.put("playerBPoint", this.playerBScore);
        payload.put("playerATimeMs", aTime);
        payload.put("playerBTimeMs", bTime);
        // convenience human-readable seconds
        payload.put("playerATimeSec", aTime / 1000.0);
        payload.put("playerBTimeSec", bTime / 1000.0);
        payload.put("message", "Match finished");

        // envelope type: use an appropriate constant. If MATCH_FINISHED doesn't exist,
        // replace with a string or an existing protocol constant.
        String envelopeType = ProtocolConstants.MATCH_RESULT; // or "MATCH_FINISHED" if constant missing
        Envelope env = new Envelope(envelopeType, payload, null);

        // send to both players (sendToPlayer handles session/socket lookup)
        sendToPlayer(playerA, env);
        sendToPlayer(playerB, env);
    }
}
