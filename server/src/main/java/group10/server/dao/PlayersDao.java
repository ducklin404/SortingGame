package group10.server.dao;

import java.util.UUID;


public interface PlayersDao {
    UUID findPlayerIdByUsername(String username);
    // other auth methods...
}
