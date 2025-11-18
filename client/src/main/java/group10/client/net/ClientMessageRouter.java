package group10.client.net;

import java.util.HashMap;
import java.util.Map;
import group10.client.net.MessageHandler;

public class ClientMessageRouter {
    private final Map<String, MessageHandler> handlers = new HashMap<>();

    public void add(String messageType, MessageHandler handler) {
        handlers.put(messageType, handler);
    }

    public void registerAll(ClientConnection connection, boolean uiHandlers) {
        for (Map.Entry<String, MessageHandler> e : handlers.entrySet()) {
            if (uiHandlers) connection.onUi(e.getKey(), e.getValue());
            else connection.on(e.getKey(), e.getValue());
        }
    }
}
