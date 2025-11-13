package group10.server;

import group10.common.Player;
import group10.server.ClientHandler;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerSession {
    private String sessionId;
    private Player player;
    private ClientHandler handler;
    private long lastHeartbeat;
    private String status; // "IDLE" or "BUSY"

    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }
}
