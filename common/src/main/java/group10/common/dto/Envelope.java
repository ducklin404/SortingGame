package group10.common.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public class Envelope {
    private String type;
    private JsonNode payload;
    private UUID sessionId;

    public Envelope() {}

    public Envelope(String type, JsonNode payload, UUID sessionId) {
        this.type = type;
        this.payload = payload;
        this.sessionId = sessionId;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public JsonNode getPayload() { return payload; }
    public void setPayload(JsonNode payload) { this.payload = payload; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
}
