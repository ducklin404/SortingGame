package group10.common;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerInfo {
    private UUID playerId;
    private String username;
    private String displayName;
    private double rankingPoints;
    private String status; // "IDLE" or "BUSY"

    @Override
    public String toString() {
        return String.format("%s (%.1f pts) - %s",
                displayName != null ? displayName : username,
                rankingPoints,
                status);
    }
}
