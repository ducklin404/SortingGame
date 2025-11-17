package group10.server.net;

import java.net.Socket;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionRegistry {
    private final ConcurrentHashMap<UUID, Socket> map = new ConcurrentHashMap<>();

    public void register(UUID sessionId, Socket socket) {
        map.put(sessionId, socket);
    }

    public void unregister(UUID sessionId, Socket socket) {
        map.remove(sessionId, socket);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Socket getSocket(UUID sessionId) {
        return map.get(sessionId);
    }
}

