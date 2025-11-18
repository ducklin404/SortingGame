package group10.client.net;

import group10.common.dto.Envelope;
import java.net.Socket;

@FunctionalInterface
public interface MessageHandler {
    void handle(Envelope env, Socket socket);
}