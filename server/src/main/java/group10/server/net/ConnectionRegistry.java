package group10.server.net;

import java.net.Socket;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// map session id to socket
public class ConnectionRegistry {
    private final ConcurrentHashMap<UUID, Set<Socket>> map = new ConcurrentHashMap<>();

    public void register(UUID sessionId, Socket socket) {
        map.compute(sessionId, (k, s) -> {
            if (s == null) s = ConcurrentHashMap.newKeySet();
            s.add(socket);
            return s;
        });
    }

    public void unregister(UUID sessionId, Socket socket) {
        map.computeIfPresent(sessionId, (k, s) -> {
            s.remove(socket);
            return s.isEmpty() ? null : s;
        });
    }

    public Set<Socket> getSockets(UUID sessionId) {
        return map.getOrDefault(sessionId, ConcurrentHashMap.newKeySet());
    }
}
