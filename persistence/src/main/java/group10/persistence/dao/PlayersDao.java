package group10.persistence.dao;

import java.util.UUID;

public interface PlayersDao {
    UUID authenticate(String username, String plaintextPassword);
    UUID findByUsername(String username);
}
