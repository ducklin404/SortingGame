package group10.common;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Lớp Message đại diện cho một gói tin JSON trao đổi giữa Client và Server.
 * Mỗi message có 2 phần:
 *   - type: loại thông điệp (MessageType)
 *   - payload: nội dung dữ liệu (String / JSON object / JSON array)
 */
public class Message {
    private final MessageType type;
    private Object payload;

    public Message(MessageType type) {
        this(type, new JSONObject());
    }

    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    /**
     * Gán thêm thuộc tính vào payload dạng JSON object.
     * Trả về chính đối tượng Message để hỗ trợ chaining.
     */
    public Message put(String key, Object value) {
        if (payload == null) {
            payload = new JSONObject();
        }

        if (payload instanceof JSONObject jsonObject) {
            jsonObject.put(key, value);
            return this;
        }

        throw new IllegalStateException("Cannot add key/value to non-object payload");
    }

    public JSONObject getPayloadAsObject() {
        if (payload == null) {
            payload = new JSONObject();
        }
        if (payload instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        throw new IllegalStateException("Payload is not a JSON object");
    }

    public JSONArray getPayloadAsArray() {
        if (payload instanceof JSONArray jsonArray) {
            return jsonArray;
        }
        throw new IllegalStateException("Payload is not a JSON array");
    }

    public Object get(String key) {
        JSONObject obj = getPayloadAsObject();
        if (!obj.has(key) || obj.isNull(key)) {
            throw new IllegalArgumentException("Key '" + key + "' is missing in payload for type " + type);
        }
        return obj.get(key);
    }

    public String getString(String key) {
        return Objects.toString(get(key));
    }

    public String optString(String key) {
        JSONObject obj = getPayloadAsObject();
        return obj.has(key) && !obj.isNull(key) ? obj.optString(key, null) : null;
    }

    public int getInt(String key) {
        JSONObject obj = getPayloadAsObject();
        if (!obj.has(key) || obj.isNull(key)) {
            throw new IllegalArgumentException("Key '" + key + "' is missing in payload for type " + type);
        }
        return obj.getInt(key);
    }

    public Integer optInt(String key) {
        JSONObject obj = getPayloadAsObject();
        return obj.has(key) && !obj.isNull(key) ? obj.getInt(key) : null;
    }

    public java.util.UUID optUUID(String key) {
        String value = optString(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return java.util.UUID.fromString(value);
    }

    public long getLong(String key) {
        JSONObject obj = getPayloadAsObject();
        if (!obj.has(key) || obj.isNull(key)) {
            throw new IllegalArgumentException("Key '" + key + "' is missing in payload for type " + type);
        }
        return obj.getLong(key);
    }

    public boolean getBoolean(String key) {
        JSONObject obj = getPayloadAsObject();
        if (!obj.has(key) || obj.isNull(key)) {
            throw new IllegalArgumentException("Key '" + key + "' is missing in payload for type " + type);
        }
        return obj.getBoolean(key);
    }

    /**
     *  Chuyển Message thành chuỗi JSON để gửi qua socket
     */
    public String toJson() {
        JSONObject obj = new JSONObject();
        obj.put("type", type.toString());

        //  Nếu payload là chuỗi JSON thật → parse lại để không bị escape
        if (payload instanceof String strPayload) {
            try {
                if (strPayload.trim().startsWith("[")) {
                    obj.put("payload", new JSONArray(strPayload)); // mảng JSON
                } else if (strPayload.trim().startsWith("{")) {
                    obj.put("payload", new JSONObject(strPayload)); // object JSON
                } else {
                    obj.put("payload", strPayload); // chuỗi thường
                }
            } catch (Exception e) {
                obj.put("payload", strPayload);
            }
        } else {
            obj.put("payload", payload);
        }

        return obj.toString() + Protocol.TERMINATOR;
    }

    /**
     *  Parse chuỗi JSON nhận được thành đối tượng Message
     */
    public static Message fromJson(String json) {
        JSONObject obj = new JSONObject(json.trim());
        MessageType type = MessageType.valueOf(obj.getString("type"));
        Object payload = obj.opt("payload");
        return new Message(type, payload);
    }

    @Override
    public String toString() {
        return "[Message type=" + type + ", payload=" + payload + "]";
    }
}
