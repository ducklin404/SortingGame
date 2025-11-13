package group10.server;

import group10.common.*;
import group10.persistence.InviteDAO;
import group10.persistence.MatchDAO;
import group10.persistence.PlayerDAO;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * MatchmakingManager - Thành viên 2: Nguyễn Tiến An
 *
 * Quản lý invite và match creation với persistence layer
 */
@Slf4j
public class MatchmakingManager {
    private final SessionManager sessionManager;
    private final InviteDAO inviteDAO;
    private final PlayerDAO playerDAO;
    private final MatchDAO matchDAO;

    // Active matches in memory
    private final Map<UUID, GameMatch> activeMatches;  // matchId -> GameMatch
    private final Map<UUID, UUID> playerToMatch;    // playerId -> matchId

    // Invite expiration scheduler
    private final InviteExpirationScheduler expirationScheduler;

    public MatchmakingManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.inviteDAO = new InviteDAO();
        this.playerDAO = new PlayerDAO();
        this.matchDAO = new MatchDAO();
        this.activeMatches = new ConcurrentHashMap<>();
        this.playerToMatch = new ConcurrentHashMap<>();
        this.expirationScheduler = new InviteExpirationScheduler(this);

        // Start expiration checker
        expirationScheduler.start();

        log.info("MatchmakingManager initialized");
    }

    /**
     * Send invite
     * Client sends: {"type": "INVITE", "payload": {"to_player_id": "uuid"}}
     */
    public synchronized boolean sendInvite(UUID fromPlayerId, UUID toPlayerId) {
        log.debug("Attempting to send invite from {} to {}", fromPlayerId, toPlayerId);

        PlayerSession fromSession = sessionManager.getSessionByPlayerId(fromPlayerId);
        PlayerSession toSession = sessionManager.getSessionByPlayerId(toPlayerId);

        if (fromSession == null || toSession == null) {
            log.warn("Cannot send invite: player session not found");
            return false;
        }

        // Check status
        if (!"IDLE".equals(fromSession.getStatus())) {
            sendError(fromSession, "You are busy!");
            return false;
        }

        if (!"IDLE".equals(toSession.getStatus())) {
            sendError(fromSession, "Player is busy!");
            return false;
        }

        // Check if already has pending invite
        if (inviteDAO.hasPendingInvite(toPlayerId)) {
            sendError(fromSession, "Player already has a pending invitation!");
            return false;
        }

        // Create invite in database (30 seconds expiration)
        UUID inviteId = inviteDAO.createInvite(fromPlayerId, toPlayerId, 30000);

        if (inviteId == null) {
            sendError(fromSession, "Failed to create invitation");
            return false;
        }

        // Get player info
        Player fromPlayer = playerDAO.getPlayerById(fromPlayerId);

        // Send invite to target player
        // Format: {"type": "INVITE", "payload": {"invite_id": "uuid", "from_player_id": "uuid", ...}}
        Message inviteMsg = new Message(MessageType.INVITE);
        inviteMsg.put("invite_id", inviteId.toString());
        inviteMsg.put("from_player_id", fromPlayerId.toString());
        inviteMsg.put("from_username", fromPlayer.getUsername());
        inviteMsg.put("from_display_name", fromPlayer.getDisplayName());
        inviteMsg.put("expires_in", 30); // seconds
        toSession.getHandler().sendMessage(inviteMsg);

        log.info("Invite sent: id={}, from={}, to={}", inviteId, fromPlayerId, toPlayerId);

        return true;
    }

    /**
     * Handle invite response
     * Client sends: {"type": "INVITE_RESPONSE", "payload": {"status": "ACCEPTED", "invite_id": "uuid"}}
     */
    public synchronized void handleInviteResponse(UUID respondingPlayerId, UUID inviteId, String status) {
        log.debug("Invite response: player={}, invite={}, status={}",
                respondingPlayerId, inviteId, status);

        // Get invite from database
        Invite invite = inviteDAO.getInviteById(inviteId);

        if (invite == null) {
            log.warn("Invite not found: {}", inviteId);
            return;
        }

        // Verify the responding player is the correct recipient
        if (!invite.getToPlayerId().equals(respondingPlayerId)) {
            log.warn("Invalid invite response: wrong player");
            return;
        }

        // Check if invite is still pending
        if (!invite.isPending()) {
            log.warn("Invite is not pending: {}", inviteId);
            return;
        }

        // Check if expired
        if (invite.isExpired()) {
            inviteDAO.updateInviteStatus(inviteId, Protocol.STATUS_EXPIRED);
            sendInviteExpired(invite);
            return;
        }

        // Get sessions
        PlayerSession fromSession = sessionManager.getSessionByPlayerId(invite.getFromPlayerId());
        PlayerSession toSession = sessionManager.getSessionByPlayerId(invite.getToPlayerId());

        if (fromSession == null || toSession == null) {
            log.warn("Player session not found when handling invite response");
            return;
        }

        if (Protocol.STATUS_ACCEPTED.equals(status)) {
            // Update invite status
            inviteDAO.updateInviteStatus(inviteId, Protocol.STATUS_ACCEPTED);

            // Create match
            createAndStartMatch(invite, fromSession, toSession);

        } else if (Protocol.STATUS_REJECTED.equals(status)) {
            // Update invite status
            inviteDAO.updateInviteStatus(inviteId, Protocol.STATUS_REJECTED);

            // Notify sender
            Player toPlayer = playerDAO.getPlayerById(invite.getToPlayerId());

            Message rejectMsg = new Message(MessageType.INVITE_RESPONSE);
            rejectMsg.put("status", Protocol.STATUS_REJECTED);
            rejectMsg.put("player_name", toPlayer.getDisplayName());
            fromSession.getHandler().sendMessage(rejectMsg);

            log.info("Invite rejected: id={}", inviteId);
        }
    }

    /**
     * Create and start match from accepted invite
     */
    private void createAndStartMatch(Invite invite, PlayerSession p1Session, PlayerSession p2Session) {
        UUID player1Id = invite.getFromPlayerId();
        UUID player2Id = invite.getToPlayerId();

        // Create match in database
        UUID matchId = matchDAO.createMatch(invite.getId(), player1Id, player2Id);

        if (matchId == null) {
            sendError(p1Session, "Failed to create match");
            sendError(p2Session, "Failed to create match");
            log.error("Failed to create match in database");
            return;
        }

        // Create match object
        GameMatch match = GameMatch.builder()
                .id(matchId)
                .inviteId(invite.getId())
                .player1Id(player1Id)
                .player2Id(player2Id)
                .status("WAITING")
                .startedAt(new java.sql.Timestamp(System.currentTimeMillis()))
                .build();

        // Save to memory
        activeMatches.put(matchId, match);
        playerToMatch.put(player1Id, matchId);
        playerToMatch.put(player2Id, matchId);

        // Update status
        p1Session.setStatus("BUSY");
        p2Session.setStatus("BUSY");

        // Broadcast online list
        sessionManager.broadcastOnlineList();

        // Get player info
        Player player1 = playerDAO.getPlayerById(player1Id);
        Player player2 = playerDAO.getPlayerById(player2Id);

        // Send match start notification
        // Format: {"type": "MATCH_START", "payload": {"match_id": "uuid", ...}}
        Message startMsg1 = new Message(MessageType.MATCH_START);
        startMsg1.put("match_id", matchId.toString());
        startMsg1.put("opponent_id", player2Id.toString());
        startMsg1.put("opponent_name", player2.getDisplayName());
        startMsg1.put("your_role", "PLAYER1");
        p1Session.getHandler().sendMessage(startMsg1);

        Message startMsg2 = new Message(MessageType.MATCH_START);
        startMsg2.put("match_id", matchId.toString());
        startMsg2.put("opponent_id", player1Id.toString());
        startMsg2.put("opponent_name", player1.getDisplayName());
        startMsg2.put("your_role", "PLAYER2");
        p2Session.getHandler().sendMessage(startMsg2);

        log.info("Match created and started: id={}, invite={}, {} vs {}",
                matchId, invite.getId(), player1.getUsername(), player2.getUsername());
    }

    /**
     * Handle expired invites (called by scheduler)
     */
    public void handleExpiredInvites(List<Invite> expiredInvites) {
        for (Invite invite : expiredInvites) {
            // Update status in database
            inviteDAO.updateInviteStatus(invite.getId(), Protocol.STATUS_EXPIRED);

            // Notify both players
            sendInviteExpired(invite);

            log.info("Invite expired: id={}", invite.getId());
        }
    }

    /**
     * Send invite expired notification
     */
    private void sendInviteExpired(Invite invite) {
        PlayerSession fromSession = sessionManager.getSessionByPlayerId(invite.getFromPlayerId());
        PlayerSession toSession = sessionManager.getSessionByPlayerId(invite.getToPlayerId());

        Message expiredMsg = new Message(MessageType.INVITE_EXPIRED);
        expiredMsg.put("invite_id", invite.getId().toString());
        expiredMsg.put("message", "Invitation expired");

        if (fromSession != null) {
            fromSession.getHandler().sendMessage(expiredMsg);
        }

        if (toSession != null) {
            toSession.getHandler().sendMessage(expiredMsg);
        }
    }

    /**
     * Get match
     */
    public GameMatch getMatch(UUID matchId) {
        return activeMatches.get(matchId);
    }

    /**
     * Get match ID by player
     */
    public UUID getMatchIdByPlayer(UUID playerId) {
        return playerToMatch.get(playerId);
    }

    /**
     * End match
     */
    public synchronized void endMatch(UUID matchId, UUID winnerId) {
        GameMatch match = activeMatches.remove(matchId);

        if (match == null) {
            log.warn("Match not found when trying to end: {}", matchId);
            return;
        }

        // Remove player mappings
        playerToMatch.remove(match.getPlayer1Id());
        playerToMatch.remove(match.getPlayer2Id());

        // Update database
        matchDAO.finishMatch(matchId, winnerId);

        // Update player stats
        updatePlayerStatsAfterMatch(match, winnerId);

        // Update status
        PlayerSession p1Session = sessionManager.getSessionByPlayerId(match.getPlayer1Id());
        PlayerSession p2Session = sessionManager.getSessionByPlayerId(match.getPlayer2Id());

        if (p1Session != null) {
            p1Session.setStatus("IDLE");
        }
        if (p2Session != null) {
            p2Session.setStatus("IDLE");
        }

        sessionManager.broadcastOnlineList();

        log.info("Match ended: id={}, winner={}", matchId, winnerId);
    }

    /**
     * Update player stats after match
     */
    private void updatePlayerStatsAfterMatch(GameMatch match, UUID winnerId) {
        UUID player1Id = match.getPlayer1Id();
        UUID player2Id = match.getPlayer2Id();
        int player1Score = match.getPlayer1Score();
        int player2Score = match.getPlayer2Score();

        double player1Points = 0.0;
        double player2Points = 0.0;
        boolean player1Win = false;
        boolean player2Win = false;

        if (winnerId == null) {
            player1Points = 0.5;
            player2Points = 0.5;
        } else if (winnerId.equals(player1Id)) {
            player1Points = 1.0;
            player2Points = 0.0;
            player1Win = true;
        } else {
            player1Points = 0.0;
            player2Points = 1.0;
            player2Win = true;
        }

        matchDAO.updatePlayerStats(player1Id, player1Score, 10, player1Points, player1Win);
        matchDAO.updatePlayerStats(player2Id, player2Score, 10, player2Points, player2Win);
    }

    /**
     * Handle player disconnect
     */
    public synchronized void handlePlayerDisconnect(UUID playerId) {
        log.info("Handling player disconnect: {}", playerId);
        if (playerId == null) {
            log.warn("handlePlayerDisconnect called with null playerId");
            return;
        }

        UUID matchId = playerToMatch.get(playerId);

        if (matchId != null) {
            GameMatch match = activeMatches.get(matchId);

            if (match != null) {
                UUID opponentId = match.getOpponentId(playerId);

                PlayerSession opponentSession = sessionManager.getSessionByPlayerId(opponentId);
                if (opponentSession != null) {
                    Message msg = new Message(MessageType.PLAYER_DISCONNECTED);
                    msg.put("message", "Opponent disconnected. You win!");
                    opponentSession.getHandler().sendMessage(msg);
                }

                endMatch(matchId, opponentId);
            }
        }
    }

    /**
     * Send error message
     */
    private void sendError(PlayerSession session, String errorMessage) {
        Message errorMsg = new Message(MessageType.ERROR);
        errorMsg.put("message", errorMessage);
        session.getHandler().sendMessage(errorMsg);
    }

    /**
     * Is player in match
     */
    public boolean isPlayerInMatch(UUID playerId) {
        return playerToMatch.containsKey(playerId);
    }

    /**
     * Get active match count
     */
    public int getActiveMatchCount() {
        return activeMatches.size();
    }

    /**
     * Shutdown
     */
    public void shutdown() {
        expirationScheduler.stop();
        log.info("MatchmakingManager shutdown");
    }
}
