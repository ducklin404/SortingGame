package group10.server;

import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.common.dto.Envelope;
import group10.common.util.JsonUtil;
import group10.common.protocol.ProtocolConstants;
import group10.common.net.LengthPrefixedIO;
import group10.persistence.dao.PlayerDao;
import group10.persistence.impl.*;
import group10.server.router.MessageHandler;
import group10.server.router.MessageRouter;
import group10.server.session.SessionManager;
import group10.server.handlers.InviteHandlers;
import group10.server.net.ConnectionRegistry;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import io.github.cdimascio.dotenv.Dotenv;
import javax.sql.DataSource;
import group10.persistence.dao.MatchesDao;
import group10.persistence.dao.SessionDao;
import group10.persistence.dao.InvitesDao;


public class Main {
    public static void main(String[] args) throws Exception {
        // set env global
        Dotenv dotenv = Dotenv.configure().directory("./server").load();

        // init db
        DataSource ds = DataSourceFactory.createFromEnv(dotenv);

        PlayerDao playerDao = new PlayerDaoImpl(ds);
        SessionDao sessionDao = new SessionDaoImpl(ds);
        InvitesDao invitesDao = new InvitesDaoImpl(ds);
        MatchesDao matchesDao = new MatchesDaoImpl(ds);
        long timeoutMs = 60000;


        int port = 9000;

        // Executors: router threads (each connection) + handler executor (async handler work)
        ExecutorService routerPool = Executors.newCachedThreadPool();
        ExecutorService handlerExecutor = Executors.newCachedThreadPool();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ConnectionRegistry connectionRegistry = new ConnectionRegistry();

        // test session
        SessionManager sessionManager = new SessionManager(sessionDao, timeoutMs);


        Map<String, MessageHandler> handlers = new ConcurrentHashMap<>();
        // Test ping
        handlers.put(ProtocolConstants.PING, (env, sock) -> {
            // reply with PONG; use the same sessionId if present
            ObjectNode payload = JsonUtil.MAPPER.createObjectNode()
                    .put("ts", System.currentTimeMillis());
            UUID ephemeralUser;
            if (connectionRegistry.isEmpty()){
                ephemeralUser = UUID.fromString("3169622e-885d-43ca-9685-9ecc7314f035");
            }else{
                ephemeralUser = UUID.fromString("3c17e4ef-59dc-44d7-b67d-481a5677573e");
            }
            UUID ephemeralSession = sessionManager.createSession(ephemeralUser);
            // register mapping and attach to envelope so handlers see it
            connectionRegistry.register(ephemeralSession, sock);
            Envelope resp = new Envelope(ProtocolConstants.PONG, payload, ephemeralSession);
            try {
                LengthPrefixedIO.writeObject(sock, resp);
            } catch (IOException e) {
                // best-effort: client probably disconnected
            }
        });

        // Register handlers
        handlers.put(ProtocolConstants.INVITE,
                InviteHandlers.invite(sessionManager));

        handlers.put(ProtocolConstants.INVITE_RESPONSE,
                InviteHandlers.inviteResponse(invitesDao, playerDao, sessionManager, connectionRegistry));


        // Start ServerSocket accept loop
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Minimal server listening on port " + port);

        // shutdown hook to clean up executors and close socket
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            try { serverSocket.close(); } catch (IOException ignored) {}
            routerPool.shutdownNow();
            handlerExecutor.shutdownNow();
            scheduler.shutdownNow();
        }));

        // Accept loop
        while (!serverSocket.isClosed()) {
            Socket client = serverSocket.accept();
            // Create and submit a MessageRouter for each connection
            MessageRouter router = new MessageRouter(client, handlers, sessionManager, handlerExecutor, connectionRegistry);
            routerPool.submit(router);
        }
    }
}
