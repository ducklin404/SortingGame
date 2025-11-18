package group10.server.router;

import group10.common.dto.Envelope;

import java.net.Socket;

@FunctionalInterface
public interface MessageHandler {
    /**
     * Handle an incoming Envelope. Implementations should NOT close the socket.
     * @param envelope parsed message envelope
     * @param clientSocket socket to write replies
     * @throws Exception if handler fails (router will catch/log)
     */
    void handle(Envelope envelope, Socket clientSocket) throws Exception;
}
