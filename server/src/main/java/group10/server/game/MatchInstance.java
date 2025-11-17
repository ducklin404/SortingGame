package group10.server.game;

import group10.common.dto.Envelope;
import group10.common.net.LengthPrefixedIO;
import group10.common.protocol.ProtocolConstants;
import group10.common.util.JsonUtil;
import group10.server.net.ConnectionRegistry;
import group10.server.dao.RoundsDao;
import group10.server.dao.SubmissionsDao;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Per-match instance that runs rounds. Keep it small and focused.
 */
public class MatchInstance {
    private final UUID matchId;
    private final UUID playerA;
    private final UUID playerB;
    private final RoundGenerator generator;
    private final RoundValidator validator;
    private final ConnectionRegistry connRegistry;
    private final ScheduledExecutorService scheduler;
    private final RoundsDao roundsDao;
    private final SubmissionsDao submissionsDao;

    private int currentRound = 0;
    private final int totalRounds = GameRules.ROUND_COUNT;

    public MatchInstance(UUID matchId, UUID a, UUID b,
                         RoundGenerator generator,
                         RoundValidator validator,
                         ConnectionRegistry connRegistry,
                         ScheduledExecutorService scheduler,
                         RoundsDao roundsDao,
                         SubmissionsDao submissionsDao) {
        this.matchId = matchId;
        this.playerA = a; this.playerB = b;
        this.generator = generator;
        this.validator = validator;
        this.connRegistry = connRegistry;
        this.scheduler = scheduler;
        this.roundsDao = roundsDao;
        this.submissionsDao = submissionsDao;
    }

    public void startNextRound() {
        currentRound++;
        if (currentRound > totalRounds) {
            endMatch();
            return;
        }

        // generate payload
        ObjectNode payload = generator.generateLetterRound(15);

        // persist the round (roundsDao.insertRound returns roundId)
        UUID roundId = roundsDao.insertRound(matchId, currentRound, payload.toString(), payload.get("order").asText(), System.currentTimeMillis() + GameRules.ROUND_TIME_MS);

        // Send START_ROUND to both players
        ObjectNode msg = JsonUtil.MAPPER.createObjectNode();
        msg.put("matchId", matchId.toString());
        msg.put("round", currentRound);
        msg.put("roundId", roundId.toString());
        msg.set("items", payload.get("items"));
        msg.put("order", payload.get("order").asText());
        msg.put("deadlineTs", System.currentTimeMillis() + GameRules.ROUND_TIME_MS);

        Envelope env = new Envelope(ProtocolConstants.START_ROUND, msg, null);
        sendToPlayer(playerA, env);
        sendToPlayer(playerB, env);

        // schedule deadline handler
        scheduler.schedule(() -> onRoundDeadline(roundId), GameRules.ROUND_TIME_MS, TimeUnit.MILLISECONDS);
    }

    private void onRoundDeadline(UUID roundId) {
        // fetch submissions, compute scores, broadcast ROUND_RESULT, then start next round
        // ... use submissionsDao to collect
        // call startNextRound() when done
    }

    private void sendToPlayer(UUID playerId, Envelope env) {
        Socket socket = connRegistry.getSocket(playerId);

        try { LengthPrefixedIO.writeObject(socket, env); } catch (Exception ignored) {}

    }

    private void endMatch() {
        // compute final, persist, broadcast MATCH_RESULT
    }
}
