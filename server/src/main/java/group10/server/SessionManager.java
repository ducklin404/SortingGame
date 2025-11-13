package group10.server;

import group10.common.Message;
import group10.common.MessageType;
import group10.common.Player;
import group10.common.PlayerInfo;
import group10.persistence.PlayerDAO;
import group10.server.ClientHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SessionManager {
    private final Map<String, PlayerSession> sessions;        // sessionId -> PlayerSession
    private final Map<UUID, String> playerToSession;       // playerId -> sessionId
    private final PlayerDAO playerDAO;

    public SessionManager() {
        this.sessions = new ConcurrentHashMap<>();
        this.playerToSession = new ConcurrentHashMap<>();
        this.playerDAO = new PlayerDAO();
    }

    public synchronized String login(String username, String password, ClientHandler handler) {
        Player player = playerDAO.verifyLogin(username, password);

        if (player == null) {
            log.warn("Login failed for username: {}", username);
            return null;
        }

        if (playerToSession.containsKey(player.getId())) {
            log.warn("Player already logged in: {}", username);
            return null;
        }

        String sessionId = UUID.randomUUID().toString();
        PlayerSession session = PlayerSession.builder()
                .sessionId(sessionId)
                .player(player)
                .handler(handler)
                .lastHeartbeat(System.currentTimeMillis())
                .status("IDLE")
                .build();

        sessions.put(sessionId, session);
        playerToSession.put(player.getId(), sessionId);

        log.info("Player logged in: {} (ID: {})", player.getUsername(), player.getId());

        broadcastOnlineList();

        return sessionId;
    }

    public synchronized void logout(String sessionId) {
        PlayerSession session = sessions.remove(sessionId);

        if (session != null) {
            playerToSession.remove(session.getPlayer().getId());
            log.info("Player logged out: {}", session.getPlayer().getUsername());
            broadcastOnlineList();
        }
    }

    public void updateHeartbeat(String sessionId) {
        PlayerSession session = sessions.get(sessionId);
        if (session != null) {
            session.updateHeartbeat();
        }
    }

    public List<PlayerInfo> getOnlineList() {
        List<PlayerInfo> list = new ArrayList<>();

        for (PlayerSession session : sessions.values()) {
            Player player = session.getPlayer();

            PlayerInfo info = PlayerInfo.builder()
                    .playerId(player.getId())
                    .username(player.getUsername())
                    .displayName(player.getDisplayName())
                    .rankingPoints(0.0) // Will be loaded from DB if needed
                    .status(session.getStatus())
                    .build();

            list.add(info);
        }

        return list;
    }

    public void broadcastOnlineList() {
        List<PlayerInfo> onlineList = getOnlineList();

        Message msg = new Message(MessageType.ONLINE_LIST);
        msg.put("players", onlineList);

        for (PlayerSession session : sessions.values()) {
            session.getHandler().sendMessage(msg);
        }
    }

    public PlayerSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public PlayerSession getSessionByPlayerId(java.util.UUID playerId) {
        String sessionId = playerToSession.get(playerId);
        return sessionId != null ? sessions.get(sessionId) : null;
    }

    public void checkTimeouts() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, PlayerSession> entry : sessions.entrySet()) {
            if (now - entry.getValue().getLastHeartbeat() > 30000) {
                toRemove.add(entry.getKey());
                log.warn("Session timeout: {}", entry.getValue().getPlayer().getUsername());
            }
        }

        for (String sessionId : toRemove) {
            logout(sessionId);
        }
    }

    public int getOnlineCount() {
        return sessions.size();
    }
}
