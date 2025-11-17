package group10.server.game;

import group10.persistence.dao.MatchDao;
import group10.server.net.ConnectionRegistry;
import group10.persistence.dao.RoundsDao;
import group10.persistence.dao.SubmissionDao;
import group10.server.session.SessionManager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

/**
 * Tracks pending matches that are waiting for START_MATCH_ACK from both parties.
 * When both acks are present, constructs a MatchInstance and calls startNextRound().
 */
public class MatchManager {
    private final ConnectionRegistry connRegistry;
    private final SessionManager sessionManager;
    private final ScheduledExecutorService scheduler;
    private final RoundGenerator generator;
    private final RoundValidator validator;
    private final RoundsDao roundsDao;
    private final SubmissionDao submissionsDao;
    private final MatchDao matchDao;
    // pending matchId -> MatchRecord
    private final Map<UUID, MatchRecord> pending = new ConcurrentHashMap<>();

    // timeout for waiting both ACKs (ms)
    private final long ackTimeoutMs;

    public MatchManager(ConnectionRegistry connRegistry,
                        SessionManager sessionManager,
                        ScheduledExecutorService scheduler,
                        RoundGenerator generator,
                        RoundValidator validator,
                        RoundsDao roundsDao,
                        SubmissionDao submissionsDao,
                        long ackTimeoutMs, MatchDao matchDao) {
        this.connRegistry = connRegistry;
        this.sessionManager = sessionManager;
        this.scheduler = scheduler;
        this.generator = generator;
        this.validator = validator;
        this.roundsDao = roundsDao;
        this.submissionsDao = submissionsDao;
        this.ackTimeoutMs = ackTimeoutMs;
        this.matchDao = matchDao;
    }

    public static class MatchRecord {
        public final UUID matchId;
        public final UUID playerAUserId;   // user ids (for MatchInstance)
        public final UUID playerBUserId;
        public final UUID sessionA;       // session ids (for sending)
        public final UUID sessionB;
        // threadsafe set of session ids that have acked
        public final Set<UUID> acks = Collections.newSetFromMap(new ConcurrentHashMap<>());
        // created match instance (filled when started)
        public final AtomicReference<MatchInstance> instanceRef = new AtomicReference<>(null);
        // scheduled timeout future to cancel waiting
        public ScheduledFuture<?> timeoutFuture;

        public MatchRecord(UUID matchId, UUID playerAUserId, UUID playerBUserId, UUID sessionA, UUID sessionB) {
            this.matchId = matchId;
            this.playerAUserId = playerAUserId;
            this.playerBUserId = playerBUserId;
            this.sessionA = sessionA;
            this.sessionB = sessionB;
        }
    }

    /**
     * Create a pending match that will wait for START_MATCH_ACK from both sessionA and sessionB.
     * This registers a timeout which will remove the pending match if both acks are not received in time.
     */
    public void createPendingMatch(UUID matchId,
                                   UUID playerAUserId, UUID playerBUserId,
                                   UUID sessionA, UUID sessionB,
                                   BiConsumer<UUID, MatchRecord> onStartCallback) {
        MatchRecord r = new MatchRecord(matchId, playerAUserId, playerBUserId, sessionA, sessionB);
        pending.put(matchId, r);

        // schedule timeout to clean up and notify that match failed to start
        r.timeoutFuture = scheduler.schedule(() -> {
            if (pending.remove(matchId) != null) {
                // notify players that match didn't start
                onStartCallback.accept(matchId, null);
            }
        }, ackTimeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record an ACK from `sessionId` for the match that contains that session.
     * Returns true when both acks are present and the match has been started here;
     * false if still waiting or match not found.
     */
    public boolean recordAck(UUID sessionId) {
        // find corresponding pending match that contains this sessionId
        for (MatchRecord r : pending.values()) {
            if (r.sessionA.equals(sessionId) || r.sessionB.equals(sessionId)) {
                r.acks.add(sessionId);
                // if both acked, start match
                if (r.acks.contains(r.sessionA) && r.acks.contains(r.sessionB)) {
                    // attempt to remove pending, only the first winner will start it
                    MatchRecord removed = pending.remove(r.matchId);
                    if (removed == null) return false;

                    // cancel timeout
                    if (removed.timeoutFuture != null) removed.timeoutFuture.cancel(false);

                    // create and start the MatchInstance
                    MatchInstance inst = new MatchInstance(
                            removed.matchId,
                            removed.playerAUserId,
                            removed.playerBUserId,
                            generator,
                            validator,
                            connRegistry,
                            sessionManager,
                            scheduler,
                            roundsDao,
                            submissionsDao,
                            matchDao
                    );
                    removed.instanceRef.set(inst);

                    // start first round
                    inst.startNextRound();
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public MatchRecord getPending(UUID matchId) {
        return pending.get(matchId);
    }

    public void cancelPending(UUID matchId) {
        MatchRecord r = pending.remove(matchId);
        if (r != null && r.timeoutFuture != null) r.timeoutFuture.cancel(false);
    }
}
