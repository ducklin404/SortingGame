package group10.persistence.dao;

import group10.persistence.model.Player;
import group10.persistence.model.PlayerStats;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public interface PlayerDao {

    UUID createPlayer(String username, String hashedPassword, String displayName);

    Player findById(UUID id);
    Player findByUsername(String username);

    boolean updateDisplayName(UUID playerId, String displayName);
    boolean updatePassword(UUID playerId, String newHashedPassword);

    boolean deletePlayer(UUID playerId);

    List<Player> listPlayers(int limit, int offset);

    // Stats related
    PlayerStats getPlayerStats(UUID playerId);

    boolean upsertPlayerStats(PlayerStats stats);
    boolean incrementPlayerStats(UUID playerId, BigDecimal pointsDelta, int winsDelta, int lossesDelta, int drawsDelta, int matchesDelta);
}
