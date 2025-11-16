package handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.common.dto.Envelope;
import group10.common.net.LengthPrefixedIO;
import group10.common.util.JsonUtil;
import group10.common.protocol.ProtocolConstants;
import group10.client.net.MessageHandler;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public class MatchHandlers {
    public static MessageHandler test() {
        return (env, sock) -> {
            System.out.println(env.getPayload());
        };
    }
}
